package com.tianji.promotion.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "兑换码查询参数")
public class ExchangeCodeQuery extends PageQuery {

    @ApiModelProperty(value = "兑换码状态， 1：待兑换，2：已兑换，3：兑换活动已结束")
    private Integer status;

    @ApiModelProperty(value = "兑换码目标id，例如兑换优惠券，该id则是优惠券的配置id")
    private Long couponId;
}
