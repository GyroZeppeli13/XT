package com.tianji.promotion.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.tianji.promotion.domain.query.ExchangeCodeQuery;
import com.tianji.promotion.domain.vo.ExchangeCodeVO;
import com.tianji.promotion.service.IExchangeCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 兑换码 前端控制器
 * </p>
 *
 * @author author
 * @since 2024-10-18
 */
@RestController
@RequestMapping("/codes")
@Api("兑换码相关接口")
@RequiredArgsConstructor
public class ExchangeCodeController {

    private final IExchangeCodeService exchangeCodeService;

    @ApiOperation("查询兑换码")
    @GetMapping("/page")
    public PageDTO<ExchangeCodeVO> queryExchangeCodeByPage (ExchangeCodeQuery query) {
        return exchangeCodeService.queryExchangeCodeByPage(query);
    }
}
