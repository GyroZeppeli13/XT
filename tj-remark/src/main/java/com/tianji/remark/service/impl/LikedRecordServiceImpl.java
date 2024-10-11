//package com.tianji.remark.service.impl;
//
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.baomidou.mybatisplus.core.toolkit.Wrappers;
//import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
//import com.tianji.common.exceptions.BadRequestException;
//import com.tianji.common.utils.CollUtils;
//import com.tianji.common.utils.StringUtils;
//import com.tianji.common.utils.UserContext;
//import com.tianji.remark.domain.dto.LikeRecordFormDTO;
//import com.tianji.remark.domain.dto.LikedTimesDTO;
//import com.tianji.remark.domain.po.LikedRecord;
//import com.tianji.remark.mapper.LikedRecordMapper;
//import com.tianji.remark.service.ILikedRecordService;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
//import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;
//
///**
// * <p>
// * 点赞记录表 服务实现类
// * </p>
// *
// * @author author
// * @since 2024-10-09
// */
////@Service
//@Slf4j
//@RequiredArgsConstructor
//public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
//
//    private final RabbitMqHelper mqHelper;
//
//    @Override
//    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
//        // 1.基于前端的参数，判断是执行点赞还是取消点赞
//        boolean success = recordDTO.getLiked() ? like(recordDTO) : unlike(recordDTO);
//        // 2.判断是否执行成功，如果失败，则直接结束
//        if (!success) {
//            return;
//        }
//        // 3.如果执行成功，统计点赞总数
//        Integer likedTimes = lambdaQuery()
//                .eq(LikedRecord::getBizId, recordDTO.getBizId())
//                .count();
//        // 4.发送MQ通知
//        mqHelper.send(
//                LIKE_RECORD_EXCHANGE,
//                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, recordDTO.getBizType()),
//                //LikedTimesDTO注解@AllArgsConstructor(staticName = "of")
//                LikedTimesDTO.of(recordDTO.getBizId(), likedTimes));
//    }
//
//    private boolean unlike(LikeRecordFormDTO recordDTO) {
//        return remove(new QueryWrapper<LikedRecord>().lambda()
//                .eq(LikedRecord::getUserId, UserContext.getUser())
//                .eq(LikedRecord::getBizId, recordDTO.getBizId()));
//    }
//
//    private boolean like(LikeRecordFormDTO recordDTO) {
//        Long userId = UserContext.getUser();
//        // 1.查询点赞记录
//        Integer count = lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .eq(LikedRecord::getBizId, recordDTO.getBizId())
//                .count();
//        // 2.判断是否存在，如果已经存在，直接结束
//        if (count > 0) {
//            return false;
//        }
//        // 3.如果不存在，直接新增
//        LikedRecord r = new LikedRecord();
//        r.setUserId(userId);
//        r.setBizId(recordDTO.getBizId());
//        r.setBizType(recordDTO.getBizType());
//        return save(r);
//    }
//
//    @Override
//    public Set<Long> isBizLiked(List<Long> bizIds) {
//        if(CollUtils.isEmpty(bizIds)) {
//            throw new BadRequestException("错误参数");
//        }
//        Long userId = UserContext.getUser();
//        List<LikedRecord> list = list(Wrappers.<LikedRecord>lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .in(LikedRecord::getBizId, bizIds));
//        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
//    }
//}
