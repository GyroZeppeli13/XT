package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tianji.common.utils.BeanUtils;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2024-10-11
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

    @Override
    public List<PointsBoardSeasonVO> getPointsBoardSeason() {
        //1.查询当前赛季
        // 1.1.获取日期
        LocalDate now = LocalDate.now();
        // 1.2查询当前赛季
        PointsBoardSeason currentPointsBoardSeason = getOne(Wrappers.<PointsBoardSeason>lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, now)
                .ge(PointsBoardSeason::getEndTime, now));
        //2.查询历史赛季
        List<PointsBoardSeason> pointsBoardSeasons = list(Wrappers.<PointsBoardSeason>lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, currentPointsBoardSeason.getBeginTime()));
        //3.封装vo
        List<PointsBoardSeasonVO> vos = pointsBoardSeasons.stream()
                .map(p -> BeanUtils.copyBean(p, PointsBoardSeasonVO.class)).collect(Collectors.toList());
        return vos;
    }

    @Override
    public Integer querySeasonByTime(LocalDateTime time) {
        Optional<PointsBoardSeason> optional = lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .oneOpt();
        return optional.map(PointsBoardSeason::getId).orElse(null);
    }
}
