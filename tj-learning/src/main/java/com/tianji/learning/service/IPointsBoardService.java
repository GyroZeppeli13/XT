package com.tianji.learning.service;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.vo.PointsBoardSeasonVO;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author author
 * @since 2024-10-11
 */
public interface IPointsBoardService extends IService<PointsBoard> {

    List<PointsBoardSeasonVO> getPointsBoardSeason();
}
