package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.PointsRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 学习积分记录，每个月底清零 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2024-10-11
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    void createPointsRecordTable(@Param("tableName") String tableName);
}
