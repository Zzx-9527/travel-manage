package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.*;
import com.zjx.travel.dto.*;
import com.zjx.travel.exception.OrderBusinessException;
import com.zjx.travel.mapper.*;
import com.zjx.travel.service.ScenicOrdersService;
import com.zjx.travel.util.LockUtil;
import com.zjx.travel.util.RedisKeys;
import com.zjx.travel.util.TicketSaleWindow;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import cn.dev33.satoken.exception.NotPermissionException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ScenicOrdersServiceImpl implements ScenicOrdersService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 本期只有成人票，与 scenic_stock.ticket_type_id 约定一致 */
    private static final int TICKET_TYPE_ADULT = 1;

    /** 抢锁最多等 1 秒：抢不到就不排队了，直接交给数据库条件更新 */
    private static final long STOCK_LOCK_WAIT_SECONDS = 1L;

    /** 锁租约：单次下单事务通常几毫秒，10 秒足够，异常时也能自动释放 */
    private static final long STOCK_LOCK_LEASE_SECONDS = 10L;

    @Resource private ScenicOrdersMapper mapper;
    @Resource private ScenicStockMapper stockMapper;
    @Resource private OrderScenicMapper orderScenicMapper;
    @Resource private LockUtil lockUtil;
    @Resource private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTxTemplate() {
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    public ScenicOrdersEntity queryById(Integer id){ return mapper.queryById(id); }
    public Page<ScenicOrdersEntity> queryByPage(ScenicOrdersEntity q,Integer p,Integer s,String c,String d){
        p=p==null||p<1?1:p; s=s==null||s<1?10:s; Pageable page=PageRequest.of(p-1,s); return new PageImpl<>(mapper.queryAllByLimit(q,page),page,mapper.count(q)); }
    public Map<String, Object> statusStats(ScenicOrdersEntity q){ return mapper.statusStats(q); }
    public boolean deleteById(Integer id){ ScenicOrdersEntity o=mapper.queryById(id); if(o!=null&&Integer.valueOf(0).equals(o.getStatus())) throw new OrderBusinessException("待支付订单请先取消"); return mapper.deleteById(id)>0; }

    /**
     * 下单：先用「库存行锁」把同一行余量的竞争收敛，再在本地事务里做原子扣减。
     *
     * <p><b>锁不是正确性的来源。</b>正确性只由事务里的两条条件更新保证
     * （{@code UPDATE ... WHERE remaining >= n} + 影响行数判断）。锁的作用是：
     * <ol>
     *   <li>持锁后先做一次余量预检，余量不足的请求直接失败，不打数据库写；</li>
     *   <li>让同一行库存的扣减在进入事务前就排好队，减少行锁上的排队时间。</li>
     * </ol>
     * 因此抢不到锁时<b>不报错、不排队</b>，直接放行走数据库——Redis 故障时最坏退化成
     * "没有锁"，但绝不会超卖。
     *
     * <p><b>锁必须在事务之外。</b>这里不用 {@code @Transactional}，而是显式用
     * {@link TransactionTemplate} 包住事务体：解锁放在 {@code finally} 里，此时事务已经提交。
     * 如果反过来（方法整体加事务、在方法内解锁），会出现"锁已释放、数据尚未提交"的窗口，
     * 下一个线程拿到锁后读到旧余量，锁就白加了。
     */
    public ScenicOrdersEntity createOrder(ScenicOrderCreateDTO r,Integer uid){
        // ==================== 1. 参数与可售性校验（无锁、无事务） ====================
        if (r == null || r.getScenicId() == null || r.getScenicId().getId() == null
                || r.getScenicId().getId() <= 0 || r.getTicketCount() == null || r.getTicketCount() <= 0
                || r.getPlayDate() == null || r.getContactName() == null || r.getContactName().trim().isEmpty()
                || r.getContactName().length() > 20 || r.getContactPhone() == null
                || r.getContactPhone().trim().isEmpty() || r.getContactPhone().length() > 20)
            throw new OrderBusinessException("下单参数无效");
        LocalDate date; try { date=LocalDate.parse(r.getPlayDate(),DATE); } catch(Exception e){ throw new OrderBusinessException("游玩日期格式无效"); }
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        if (!TicketSaleWindow.isSellableDate(date, today)) throw new OrderBusinessException("游玩日期不可售");
        ScenicEntity scenic=orderScenicMapper.findForOrder(r.getScenicId().getId()); if (scenic == null || !Integer.valueOf(1).equals(scenic.getStatus())) throw new OrderBusinessException("景区不可售");
        if (scenic.getPrice() == null || scenic.getPrice() < 0) throw new OrderBusinessException("景区票价无效");
        if (TicketSaleWindow.isClosedForToday(date, today, LocalTime.now(ZoneId.of("Asia/Shanghai")), scenic.getDayTicketCutoffTime()))
            throw new OrderBusinessException("今日已停止售票");
        BigDecimal unit=BigDecimal.valueOf(scenic.getPrice()).setScale(2,BigDecimal.ROUND_HALF_UP);
        int n=r.getTicketCount();
        Integer scenicId=r.getScenicId().getId();

        // ==================== 2. 库存行锁 + 事务扣减 ====================
        String lockKey = RedisKeys.stockLock(scenicId, date, TICKET_TYPE_ADULT);
        boolean locked = lockUtil.tryLockWithRedisson(lockKey, STOCK_LOCK_WAIT_SECONDS, STOCK_LOCK_LEASE_SECONDS);
        try {
            if (locked) precheckStock(scenicId, date, n);
            return txTemplate.execute(status -> persist(r, uid, scenicId, date, unit, n));
        } finally {
            if (locked) lockUtil.unlockRedisson(lockKey);
        }
    }

    /**
     * 持锁时的快速失败：容量或票种任一不足就不进事务，少一次数据库写。
     * 这里读到的余量只用于"提前拒绝"，最终仍以事务里的条件更新为准。
     */
    private void precheckStock(Integer scenicId, LocalDate date, int count) {
        ScenicCapacityEntity capacity = stockMapper.queryCapacity(scenicId, date);
        ScenicStockEntity stock = stockMapper.queryStock(scenicId, date, TICKET_TYPE_ADULT);
        if (capacity == null || stock == null) throw new OrderBusinessException("暂未开放");
        int capacityLeft = capacity.getRemaining() == null ? 0 : capacity.getRemaining();
        int stockLeft = stock.getRemaining() == null ? 0 : stock.getRemaining();
        if (Math.min(capacityLeft, stockLeft) < count) throw new OrderBusinessException("已售罄或库存不足");
    }

    /** 事务体：扣容量 → 扣票种 → 落单。任何一步影响行数不为 1 都抛异常整体回滚。 */
    private ScenicOrdersEntity persist(ScenicOrderCreateDTO r, Integer uid, Integer scenicId,
                                       LocalDate date, BigDecimal unit, int n) {
        if (stockMapper.queryCapacity(scenicId, date) == null
                || stockMapper.queryStock(scenicId, date, TICKET_TYPE_ADULT) == null)
            throw new OrderBusinessException("暂未开放");
        if (stockMapper.decrementCapacity(scenicId, date, n) != 1
                || stockMapper.decrementStock(scenicId, date, TICKET_TYPE_ADULT, n) != 1)
            throw new OrderBusinessException("已售罄或库存不足");
        ScenicOrdersEntity o=new ScenicOrdersEntity(); o.setOrderNo("ORDER"+UUID.randomUUID().toString().replace("-","")); o.setUserId(user(uid)); o.setScenicId(ref(scenicId)); o.setContactName(r.getContactName()); o.setContactPhone(r.getContactPhone()); o.setPlayDate(r.getPlayDate()); o.setTicketCount(n); o.setUnitPrice(unit.doubleValue()); o.setTotalAmount(unit.multiply(BigDecimal.valueOf(n)).doubleValue()); o.setPayType(r.getPayType()); o.setPayStatus(0); o.setStatus(0); o.setTicketTypeId(TICKET_TYPE_ADULT); o.setStockReserved(1); o.setCreateTime(new Date());
        if(mapper.insert(o)!=1) throw new OrderBusinessException("订单创建失败"); return o;
    }
    private TravelUserEntity user(Integer id){TravelUserEntity u=new TravelUserEntity();u.setId(id);return u;} private ScenicEntity ref(Integer id){ScenicEntity s=new ScenicEntity();s.setId(id);return s;}
    @Transactional(rollbackFor=Exception.class)
    public void cancel(Integer id,Integer uid,boolean admin,String reason){
        ScenicOrdersEntity o=mapper.queryById(id);
        if(o==null) throw new OrderBusinessException("订单不存在");
        if(!admin&&!uid.equals(o.getUserId().getId())) throw new NotPermissionException("order:access:other");
        if(Integer.valueOf(1).equals(o.getStatus())) throw new OrderBusinessException("已支付订单不可取消");
        if(Integer.valueOf(3).equals(o.getStatus())) throw new OrderBusinessException("已完成订单不可取消");
        if(mapper.transitionToCancelled(id,reason)!=1) throw new OrderBusinessException("订单已取消或状态已变更");
        if(o.getStockReserved()!=null&&o.getStockReserved()==1){
            if(stockMapper.incrementCapacity(o.getScenicId().getId(),LocalDate.parse(o.getPlayDate()),o.getTicketCount())!=1
                    ||stockMapper.incrementStock(o.getScenicId().getId(),LocalDate.parse(o.getPlayDate()),o.getTicketTypeId(),o.getTicketCount())!=1)
                throw new OrderBusinessException("库存回补失败");
        }
    }
    @Transactional(rollbackFor=Exception.class)
    public void timeoutCancel(ScenicOrdersEntity candidate){
        ScenicOrdersEntity o = mapper.queryById(candidate.getId());
        if (o == null || !Integer.valueOf(1).equals(o.getStockReserved())) return;
        if (mapper.transitionToTimeout(o.getId()) != 1) return;
        LocalDate date = LocalDate.parse(o.getPlayDate());
        if (stockMapper.incrementCapacity(o.getScenicId().getId(), date, o.getTicketCount()) != 1
                || stockMapper.incrementStock(o.getScenicId().getId(), date, o.getTicketTypeId(), o.getTicketCount()) != 1)
            throw new OrderBusinessException("库存回补失败");
    }
    @Transactional(rollbackFor=Exception.class)
    public void pay(Integer id,Integer uid){
        ScenicOrdersEntity existing = mapper.queryById(id);
        if (existing == null) throw new OrderBusinessException("订单不存在");
        if (!uid.equals(existing.getUserId().getId())) throw new NotPermissionException("order:access:other");
        if (mapper.transitionToPaid(id, uid) != 1) throw new OrderBusinessException("订单不可支付");
    }
    @Transactional(rollbackFor=Exception.class)
    public boolean editContact(ScenicOrderEditDTO r,Integer uid){
        if (r == null || r.getId() == null || r.getContactName() == null || r.getContactName().trim().isEmpty()
                || r.getContactName().length() > 20 || r.getContactPhone() == null
                || r.getContactPhone().trim().isEmpty() || r.getContactPhone().length() > 20)
            throw new OrderBusinessException("联系人参数无效");
        ScenicOrdersEntity existing = mapper.queryById(r.getId());
        if (existing == null) return false;
        if (!uid.equals(existing.getUserId().getId())) throw new NotPermissionException("order:access:other");
        return mapper.updateContact(r.getId(),uid,r.getContactName(),r.getContactPhone()) == 1;
    }
}
