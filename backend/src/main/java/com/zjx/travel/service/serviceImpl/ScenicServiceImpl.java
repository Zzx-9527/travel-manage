package com.zjx.travel.service.serviceImpl;

import com.zjx.travel.domain.ScenicEntity;
import com.zjx.travel.mapper.ScenicMapper;
import com.zjx.travel.service.ScenicService;
import com.zjx.travel.util.RedisKeys;
import com.zjx.travel.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.util.*;

@Service
public class ScenicServiceImpl implements ScenicService {
    private static final Logger log = LoggerFactory.getLogger(ScenicServiceImpl.class);

    private static int PAGE_DEFAULT = 1;
    private static int SIZE_DEFAULT = 10;

    /**
     * 景点详情缓存时长。
     * 景点属于"读多写少、允许短暂陈旧"的数据，30 分钟是可接受的上限；
     * 实际写入时会由 RedisUtil 加 ±10% 抖动，避免大批 key 同时过期。
     */
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(30);

    @Resource
    private ScenicMapper scenicMapper;

    @Resource
    private RedisUtil redisUtil;

    /**
     * 查询景点详情：Cache-Aside。
     *
     * <p>流程：读缓存命中直接返回；未命中时只放一个请求回源（手写锁），
     * 查库结果回写缓存；查不到也缓存 60 秒空值，防止同一个不存在的 id 反复打库。
     *
     * <p>id 非法时直接返回 null，连缓存都不查——这是防穿透的第一道闸门。
     */
    public ScenicEntity queryById(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        return redisUtil.getOrLoad(
                RedisKeys.scenicDetail(id),
                DETAIL_CACHE_TTL,
                () -> this.scenicMapper.queryById(id));
    }

    public Page<ScenicEntity> queryByPage(ScenicEntity scenic, Integer page, Integer size, String orderCol, String orderDirect){

        if(page == null || page <= 0){
            page = PAGE_DEFAULT;
        }
        if(size == null || size <= 0){
            size = SIZE_DEFAULT;
        }

        Sort sort = null;
        if(orderCol != null) {
            Sort.Order order = new Sort.Order(("DESC".equals(orderDirect) ? Sort.Direction.DESC : Sort.Direction.ASC), orderCol);
            sort = Sort.by(order);
        }

        PageRequest pageRequest = PageRequest.of(page - 1, size);
        if(sort != null){
            pageRequest = PageRequest.of(page - 1, size, sort);
        }

        long total = this.scenicMapper.count(scenic);

        return new PageImpl<>(this.scenicMapper.queryAllByLimit(scenic, pageRequest), pageRequest, total);
    }

    /**
     * 新增：先写库，成功后立即删缓存。
     * 新增前可能已经有人查过这个 id 并缓存了空值哨兵，不删的话最长会有 60 秒读不到新数据。
     */
    public boolean insert(ScenicEntity scenic){
        // 自定义 XML 插入不会触发 MyBatis-Plus 的自动填充，这里兜底补时间
        Date now = new Date();
        if (scenic.getCreateTime() == null) {
            scenic.setCreateTime(now);
        }
        if (scenic.getUpdateTime() == null) {
            scenic.setUpdateTime(now);
        }
        boolean success = this.scenicMapper.insert(scenic) > 0;
        if (success) {
            evictDetail(scenic.getId());
        }
        return success;
    }

    /**
     * 更新：先写库，后删缓存，而不是"更新缓存"。
     * 并发更新时，两个请求各自写缓存会互相覆盖成旧值；删除是幂等的，下次读自然回源拿到新值。
     * 极端情况下（删除失败）由 TTL 兜底，最长 30 分钟自愈。
     */
    public boolean update(ScenicEntity scenic){
        boolean success = this.scenicMapper.update(scenic) > 0;
        if (success) {
            evictDetail(scenic.getId());
        }
        return success;
    }

    public boolean deleteById(Integer id){
        boolean success = this.scenicMapper.deleteById(id) > 0;
        if (success) {
            evictDetail(id);
        }
        return success;
    }

    private void evictDetail(Integer id) {
        if (id == null) {
            return;
        }
        boolean deleted = redisUtil.delete(RedisKeys.scenicDetail(id));
        if (!deleted) {
            // 删除未命中不是错误（缓存里本来可能就没有），Redis 异常时由 TTL 兜底
            log.debug("删除景点详情缓存未命中或失败，id={}", id);
        }
    }

    /**
     * 浏览计数只写 Redis，不回写数据库：详情接口是热路径，不能为了计数多一次 UPDATE。
     * RedisUtil.increment 内部已做异常兜底，失败只是丢一次计数，不影响接口。
     */
    @Override
    public void recordView(Integer scenicId) {
        if (scenicId == null || scenicId <= 0) {
            return;
        }
        redisUtil.increment(RedisKeys.scenicViewCount(scenicId), 1L);
    }

    @Override
    public boolean increaseViewCount(Integer scenicId, long delta) {
        if (scenicId == null || delta <= 0) {
            return false;
        }
        return this.scenicMapper.increaseViewCount(scenicId, delta) > 0;
    }

}
