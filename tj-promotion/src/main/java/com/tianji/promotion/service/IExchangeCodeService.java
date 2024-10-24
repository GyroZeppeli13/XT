package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.ExchangeCodeQuery;
import com.tianji.promotion.domain.vo.ExchangeCodeVO;

import java.util.List;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author author
 * @since 2024-10-18
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {

    void asyncGenerateCode(Coupon coupon);

    PageDTO<ExchangeCodeVO> queryExchangeCodeByPage(ExchangeCodeQuery query);

    boolean updateExchangeMark(long serialNum, boolean b);

    Long exchangeTargetId(long serialNum);
}
