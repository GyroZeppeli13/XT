package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author author
 * @since 2024-10-11
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final PointsBoardSeasonMapper seasonMapper;

    @Override
    public List<PointsBoardSeasonVO> getPointsBoardSeason() {
        //1.查询当前赛季
        // 1.1.获取日期
        LocalDate now = LocalDate.now();
        // 1.2查询当前赛季
        PointsBoardSeason currentPointsBoardSeason = seasonMapper.selectOne(Wrappers.<PointsBoardSeason>lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, now)
                .ge(PointsBoardSeason::getEndTime, now));
        //2.查询历史赛季
        List<PointsBoardSeason> pointsBoardSeasons = seasonMapper.selectList(Wrappers.<PointsBoardSeason>lambdaQuery()
                .lt(PointsBoardSeason::getEndTime, currentPointsBoardSeason.getBeginTime()));
        //3.封装vo
        List<PointsBoardSeasonVO> vos = pointsBoardSeasons.stream()
                .map(p -> BeanUtils.copyBean(p, PointsBoardSeasonVO.class)).collect(Collectors.toList());
        return vos;
    }
}
