package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.*;
import com.zjx.travel.dto.*;
import com.zjx.travel.exception.OrderBusinessException;
import com.zjx.travel.mapper.*;
import com.zjx.travel.service.ScenicOrdersService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.dev33.satoken.exception.NotPermissionException;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ScenicOrdersServiceImpl implements ScenicOrdersService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    @Resource private ScenicOrdersMapper mapper;
    @Resource private ScenicStockMapper stockMapper;
    @Resource private OrderScenicMapper scenicMapper;

    public ScenicOrdersEntity queryById(Integer id){ return mapper.queryById(id); }
    public Page<ScenicOrdersEntity> queryByPage(ScenicOrdersEntity q,Integer p,Integer s,String c,String d){
        p=p==null||p<1?1:p; s=s==null||s<1?10:s; Pageable page=PageRequest.of(p-1,s); return new PageImpl<>(mapper.queryAllByLimit(q,page),page,mapper.count(q)); }
    public boolean deleteById(Integer id){ ScenicOrdersEntity o=mapper.queryById(id); if(o!=null&&Integer.valueOf(0).equals(o.getStatus())) throw new OrderBusinessException("待支付订单请先取消"); return mapper.deleteById(id)>0; }

    @Transactional(rollbackFor=Exception.class)
    public ScenicOrdersEntity createOrder(ScenicOrderCreateDTO r,Integer uid){
        if (r == null || r.getScenicId() == null || r.getScenicId().getId() == null
                || r.getScenicId().getId() <= 0 || r.getTicketCount() == null || r.getTicketCount() <= 0
                || r.getPlayDate() == null || r.getContactName() == null || r.getContactName().trim().isEmpty()
                || r.getContactName().length() > 20 || r.getContactPhone() == null
                || r.getContactPhone().trim().isEmpty() || r.getContactPhone().length() > 20)
            throw new OrderBusinessException("下单参数无效");
        LocalDate date; try { date=LocalDate.parse(r.getPlayDate(),DATE); } catch(Exception e){ throw new OrderBusinessException("游玩日期格式无效"); }
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai")); if(date.isBefore(today)||date.isAfter(today.plusDays(29))) throw new OrderBusinessException("游玩日期不可售");
        ScenicEntity scenic=scenicMapper.findForOrder(r.getScenicId().getId()); if (scenic == null || !Integer.valueOf(1).equals(scenic.getStatus())) throw new OrderBusinessException("景区不可售");
        if (scenic.getPrice() == null || scenic.getPrice() < 0) throw new OrderBusinessException("景区票价无效");
        if(date.equals(today)&&scenic.getDayTicketCutoffTime()!=null&&LocalTime.now(ZoneId.of("Asia/Shanghai")).compareTo(LocalTime.parse(scenic.getDayTicketCutoffTime()))>=0) throw new OrderBusinessException("今日已停止售票");
        BigDecimal unit=BigDecimal.valueOf(scenic.getPrice()).setScale(2,BigDecimal.ROUND_HALF_UP); int n=r.getTicketCount();
        if (stockMapper.queryCapacity(r.getScenicId().getId(), date) == null
                || stockMapper.queryStock(r.getScenicId().getId(), date, 1) == null)
            throw new OrderBusinessException("暂未开放");
        if (stockMapper.decrementCapacity(r.getScenicId().getId(), date, n) != 1
                || stockMapper.decrementStock(r.getScenicId().getId(), date, 1, n) != 1)
            throw new OrderBusinessException("已售罄或库存不足");
        ScenicOrdersEntity o=new ScenicOrdersEntity(); o.setOrderNo("ORDER"+UUID.randomUUID().toString().replace("-","")); o.setUserId(user(uid)); o.setScenicId(ref(r.getScenicId().getId())); o.setContactName(r.getContactName()); o.setContactPhone(r.getContactPhone()); o.setPlayDate(r.getPlayDate()); o.setTicketCount(n); o.setUnitPrice(unit.doubleValue()); o.setTotalAmount(unit.multiply(BigDecimal.valueOf(n)).doubleValue()); o.setPayType(r.getPayType()); o.setPayStatus(0); o.setStatus(0); o.setTicketTypeId(1); o.setStockReserved(1); o.setCreateTime(new Date());
        if(mapper.insert(o)!=1) throw new OrderBusinessException("订单创建失败"); return o;
    }
    private TravelUserEntity user(Integer id){TravelUserEntity u=new TravelUserEntity();u.setId(id);return u;} private ScenicEntity ref(Integer id){ScenicEntity s=new ScenicEntity();s.setId(id);return s;}
    @Transactional(rollbackFor=Exception.class)
    public void cancel(Integer id,Integer uid,boolean admin,String reason){ ScenicOrdersEntity o=mapper.queryById(id); if(o==null) throw new OrderBusinessException("订单不存在"); if(!admin&&!uid.equals(o.getUserId().getId())) throw new NotPermissionException("order:access:other"); if(mapper.transitionToCancelled(id,reason)!=1) throw new OrderBusinessException("订单已处理"); if(o.getStockReserved()!=null&&o.getStockReserved()==1){ if(stockMapper.incrementCapacity(o.getScenicId().getId(),LocalDate.parse(o.getPlayDate()),o.getTicketCount())!=1||stockMapper.incrementStock(o.getScenicId().getId(),LocalDate.parse(o.getPlayDate()),o.getTicketTypeId(),o.getTicketCount())!=1) throw new OrderBusinessException("库存回补失败"); }}
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
