package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tianji.learning.constants.LearningConstants.POINTS_RECORD_TABLE_PREFIX;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author author
 * @since 2024-10-11
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {

    private final StringRedisTemplate redisTemplate;

    private final IPointsBoardSeasonService seasonService;

    @Override
    public void addPointsRecord(Long userId, int points, PointsRecordType type) {
        if(userId == null || type == null) {
            throw new BizIllegalException("参数错误");
        }
        LocalDateTime now = LocalDateTime.now();
        int maxPoints = type.getMaxPoints();
        // 1.判断当前方式有没有积分上限
        int realPoints = points;
        if(maxPoints > 0) {
            // 2.有，则需要判断是否超过上限
            LocalDateTime begin = DateUtils.getDayStartTime(now);
            LocalDateTime end = DateUtils.getDayEndTime(now);
            // 2.1.查询今日已得积分
            int currentPoints = queryUserPointsByTypeAndDate(userId, type, begin, end);
            // 2.2.判断是否超过上限
            if(currentPoints >= maxPoints) {
                // 2.3.超过，直接结束
                return;
            }
            // 2.4.没超过，保存积分记录
            if(currentPoints + points > maxPoints){
                realPoints = maxPoints - currentPoints;
            }
        }
        // 3.没有，直接保存积分记录
        PointsRecord p = new PointsRecord();
        p.setPoints(realPoints);
        p.setUserId(userId);
        p.setType(type);
        save(p);
        // 4.更新总积分到Redis
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key, userId.toString(), realPoints);
    }

    private int queryUserPointsByTypeAndDate(
            Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end) {
        // 1.查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.select("sum(points) as totalPoints")
                .eq("user_id", userId)
                .eq("type", type)
                .between("create_time", begin, end);
        // 2.调用mapper，查询结果
        Map<String, Object> map = getMap(wrapper);
        int points = 0;
        if(map != null) {
            BigDecimal totalPoints = (BigDecimal)map.get("totalPoints");
            points = totalPoints.intValue();
        }
        // 3.判断并返回
        return points;
    }

    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        // 1.获取用户
        Long userId = UserContext.getUser();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        // 3.构建查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.select("type", "sum(points) as points")
                .eq("user_id", userId)
                .between("create_time", begin, end)
                .groupBy("type");
        // 4.查询
        List<PointsRecord> list = list(wrapper);
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyList();
        }
        // 5.封装返回
        List<PointsStatisticsVO> vos = new ArrayList<>(list.size());
        for (PointsRecord p : list) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setPoints(p.getPoints());
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public void createPointsRecordTableOfLastSeason(Integer season) {
        getBaseMapper().createPointsRecordTable(POINTS_RECORD_TABLE_PREFIX + season);
    }

    @Override
    public List<PointsRecord> queryCurrentRecordList(int season,int pageNo, int pageSize) {
        //查询上个赛季的时间
        PointsBoardSeason boardSeason = seasonService.getById(season);
        //分页查询积分记录
        Page<PointsRecord> page = lambdaQuery()
                .between(PointsRecord::getCreateTime, boardSeason.getBeginTime().atStartOfDay()
                        , DateUtils.getDayEndTime(boardSeason.getEndTime().atStartOfDay()))
                .page(new Page<>(pageNo, pageSize));
        return page.getRecords();
    }

    @Override
    @Transactional
    public void clearPointsRecordOfLastMonth() {
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);

        // 2.查询赛季信息
        PointsBoardSeason boardSeason = seasonService.getOne(Wrappers.<PointsBoardSeason>lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time));

        // 3.清除上个月记录
        remove(Wrappers.<PointsRecord>lambdaQuery()
                .between(PointsRecord::getCreateTime, boardSeason.getBeginTime().atStartOfDay()
                        , DateUtils.getDayEndTime(boardSeason.getEndTime().atStartOfDay())));
    }
}
