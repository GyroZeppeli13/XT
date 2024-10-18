package com.tianji.learning.mapper;

import com.tianji.learning.domain.po.PointsBoard;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * <p>
 * 学霸天梯榜 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2024-10-11
 */
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {

    void createPointsBoardTable(@Param("tableName") String tableName);

    @Select("select id, points from ${seasonTableName} where user_id = #{userId}")
    Map queryMyHistoryBoard(@Param("seasonTableName") String seasonTableName, @Param("userId") String userId);

    @MapKey("id")
    Map queryHistoryBoardList(@Param("seasonTableName") String seasonTableName, @Param("from") int from, @Param("pageSize")Integer pageSize);
}
