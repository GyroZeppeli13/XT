package com.tianji.promotion.handler;

import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CouponHandler {

    private final ICouponService couponService;

    private final StringRedisTemplate redisTemplate;

    @XxlJob("couponIssueJob")
    public void couponIssue(){
        // 更新优惠券状态，从未开始，修改到发放中
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        int pageNo = index + 1; // 起始页，就是分片序号+1
        int pageSize = 1000;
        List<List<Coupon>> updateList = new ArrayList<>();
        while (true) {
            //分页查询状态为未开始的优惠卷
            List<Coupon> couponList = couponService.queryCouponWithStatusByPage(CouponStatus.UN_ISSUE, pageNo, pageSize);
            if (CollUtils.isEmpty(couponList)) {
                break;
            }
            //修改优惠卷状态未发放中
            couponList.forEach( c -> {
                c.setStatus(CouponStatus.ISSUING);
            });
            updateList.add(couponList);
            // 翻页，跳过N个页，N就是分片数量
            pageNo+=total;
        }
        updateList.forEach(couponService::updateBatchById);
    }

    @XxlJob("stopCouponIssueJob")
    public void stopCouponIssue(){
        // 更新优惠券状态，从发放中，修改到发放结束
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        int pageNo = index + 1; // 起始页，就是分片序号+1
        int pageSize = 1000;
        List<List<Coupon>> updateList = new ArrayList<>();
        while (true) {
            //分页查询状态为发放中的优惠卷
            List<Coupon> couponList = couponService.queryCouponWithStatusByPage(CouponStatus.ISSUING, pageNo, pageSize);
            if (CollUtils.isEmpty(couponList)) {
                break;
            }
            //修改优惠卷状态发放结束
            couponList.forEach( c -> {
                c.setStatus(CouponStatus.FINISHED);
            });
            updateList.add(couponList);
            // 翻页，跳过N个页，N就是分片数量
            pageNo+=total;
        }
        updateList.forEach(couponService::updateBatchById);
    }

    //两个定时任务同时进行出错可能性小一些
    @XxlJob("couponIssueJobHandler")
    public void couponIssueJobHandler() {
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        int pageNo = index + 1; // 起始页，就是分片序号+1
        int pageSize = 1000;
        List<List<Coupon>> issueList = new ArrayList<>();
        // 1.更新优惠券状态，从未开始，修改到发放中
        while (true) {
            //分页查询状态为未开始的优惠卷
            List<Coupon> couponList = couponService.queryCouponWithStatusByPage(CouponStatus.UN_ISSUE, pageNo, pageSize);
            if (CollUtils.isEmpty(couponList)) {
                break;
            }
            //修改优惠卷状态未发放中
            couponList.forEach( c -> {
                c.setStatus(CouponStatus.ISSUING);
            });
            issueList.add(couponList);
            // 翻页，跳过N个页，N就是分片数量
            pageNo+=total;
        }
        issueList.forEach(couponService::updateBatchById);
        //对于延时发放的优惠券，将来需要编写定时任务，扫描发放时间已经到了的优惠券。修改状态为发放中，同时添加优惠券缓存。管道？
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = (StringRedisConnection) connection;
            for(List<Coupon> coupons : issueList) {
                for (Coupon coupon : coupons) {
                    // 2.1.组织数据
                    Map<String, String> map = new HashMap<>(4);
                    map.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueBeginTime())));
                    map.put("issueEndTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueEndTime())));
                    map.put("totalNum", String.valueOf(coupon.getTotalNum()));
                    map.put("userLimit", String.valueOf(coupon.getUserLimit()));
                    // 2.2.写缓存
                    src.hMSet(PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupon.getId(), map);
                }
            }
            return null;
        });

        // 2.更新优惠券状态，从发放中，修改到发放结束
        List<List<Coupon>> finishList = new ArrayList<>();
        while (true) {
            //分页查询状态为发放中的优惠卷
            List<Coupon> couponList = couponService.queryCouponWithStatusByPage(CouponStatus.ISSUING, pageNo, pageSize);
            if (CollUtils.isEmpty(couponList)) {
                break;
            }
            //修改优惠卷状态发放结束
            couponList.forEach( c -> {
                c.setStatus(CouponStatus.FINISHED);
            });
            finishList.add(couponList);
            // 翻页，跳过N个页，N就是分片数量
            pageNo+=total;
        }
        finishList.forEach(couponService::updateBatchById);
        //如果到达则需要将优惠券状态置为发放结束，并移除Redis缓存。
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = (StringRedisConnection) connection;
            for(List<Coupon> coupons : finishList) {
                for (Coupon coupon : coupons) {
                    // 移除缓存
                    src.del(PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupon.getId());
                }
            }
            return null;
        });
    }

}
