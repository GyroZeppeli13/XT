package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.service.IPointsBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 前端控制器
 * </p>
 *
 * @author author
 * @since 2024-10-11
 */
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@Api(tags = "排行榜相关接口")
public class PointsBoardController {

    private final IPointsBoardService pointsBoardService;

    @GetMapping("/seasons/list")
    @ApiOperation("查询赛季列表功能")
    public List<PointsBoardSeasonVO> getPointsBoardSeason() {
        return pointsBoardService.getPointsBoardSeason();
    }
}
