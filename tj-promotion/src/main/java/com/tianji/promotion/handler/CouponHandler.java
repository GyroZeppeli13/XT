package com.tianji.promotion.handler;

import com.tianji.common.utils.CollUtils;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponHandler {

    private final ICouponService couponService;

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
        List<List<Coupon>> updateList = new ArrayList<>();
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
            updateList.add(couponList);
            // 翻页，跳过N个页，N就是分片数量
            pageNo+=total;
        }
        // 2.更新优惠券状态，从发放中，修改到发放结束
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

}
