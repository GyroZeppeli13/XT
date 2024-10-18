package com.tianji.learning.handler;

import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsRecordService;
import com.tianji.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.tianji.learning.constants.LearningConstants.POINTS_BOARD_TABLE_PREFIX;
import static com.tianji.learning.constants.LearningConstants.POINTS_RECORD_TABLE_PREFIX;

@Component
@RequiredArgsConstructor
public class PointsRecordPersistentHandler {

    private final IPointsBoardSeasonService seasonService;

    private final IPointsRecordService pointsRecordService;

    //创建历史记录表
    @XxlJob("createRecordTableJob")
    public void createPointsRecordTableOfLastSeason(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.查询赛季id
        Integer season = seasonService.querySeasonByTime(time);
        if (season == null) {
            // 赛季不存在
            return;
        }
        // 3.创建表
        pointsRecordService.createPointsRecordTableOfLastSeason(season);
    }

    //数据迁移到历史记录表
    @XxlJob("savePointsRecord2DB")
    public void savePointsRecordDB(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);

        // 2.计算动态表名
        // 2.1.查询赛季信息
        Integer season = seasonService.querySeasonByTime(time);

        // 3.查询积分记录数据
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        int pageNo = index + 1; // 起始页，就是分片序号+1
        int pageSize = 1000;
        while (true) {
            List<PointsRecord> recordList = pointsRecordService.queryCurrentRecordList(season, pageNo, pageSize);
            if (CollUtils.isEmpty(recordList)) {
                break;
            }
            // 将动态表名存入ThreadLocal
            TableInfoContext.setInfo(POINTS_RECORD_TABLE_PREFIX + season);
            // 迁移到历史数据表
            pointsRecordService.saveBatch(recordList);
            // 翻页，跳过N个页，N就是分片数量
            pageNo+=total;
            // 移除动态表名
            TableInfoContext.remove();
        }
    }

    //清除历史记录
    @XxlJob("clearPointsRecordOfLastMonth")
    public void clearPointsRecordOfLastMonth(){
        pointsRecordService.clearPointsRecordOfLastMonth();
    }
}
