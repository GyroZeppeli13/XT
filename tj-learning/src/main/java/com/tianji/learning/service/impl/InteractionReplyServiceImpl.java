package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author author
 * @since 2024-10-05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final InteractionQuestionMapper questionMapper;

    private final UserClient userClient;

    private final RemarkClient remarkClient;

    @Override
    @Transactional
    public void saveReplyOrComment(ReplyDTO replyDTO) {
        // 保存回答或评论
        InteractionReply interactionReply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        Long userId = UserContext.getUser();
        interactionReply.setUserId(userId);
        //以下字段都可以靠数据库的默认值保存
        //interactionReply.setCreateTime(LocalDateTime.now());
        //interactionReply.setUpdateTime(LocalDateTime.now());
        //interactionReply.setReplyTimes(0);
        //interactionReply.setLikedTimes(0);
        save(interactionReply);

        InteractionQuestion question = questionMapper.selectById(replyDTO.getQuestionId());
        if(question == null) {
            throw new BadRequestException("非法参数");
        }
        boolean flag = false;

        // 判断当前提交的是否是回答，如果是需要在interaction_question中记录最新一次回答的id
        if(replyDTO.getAnswerId() == null) {
            flag = true;
            // 当前提交的是回复，需要在interaction_question中记录最新一次回答的id
            question.setLatestAnswerId(interactionReply.getId());
            // 当前提交的是回复，则累加问题下回答次数
            question.setAnswerTimes(question.getAnswerTimes() + 1);
        }
        else {
            // 当前提交的是回复，则累加回答下评论次数
            //update(Wrappers.<InteractionReply>lambdaUpdate()
            //        .setSql("reply_times = reply_times + 1")
            //        .eq(InteractionReply::getId, replyDTO.getAnswerId()));
            InteractionReply reply = getById(replyDTO.getAnswerId());
            if(reply == null) {
                throw new BadRequestException("非法参数");
            }
            reply.setReplyTimes(reply.getReplyTimes() + 1);
            //reply.setUpdateTime(LocalDateTime.now());
            updateById(reply);
        }

        // 判断提交评论的用户是否是学生，如果是标记问题状态为未查看
        if(replyDTO.getIsStudent()) {
           flag = true;
           question.setStatus(QuestionStatus.UN_CHECK);
        }

        //如果问题有更新则作更新操作
        if(flag) {
            question.setUpdateTime(LocalDateTime.now());
            questionMapper.updateById(question);
        }
    }

    @Override
    public PageDTO<ReplyVO> queryReplyOrComment(ReplyPageQuery query) {
        // 1.参数校验
        if(query == null || (query.getAnswerId() == null && query.getQuestionId() == null)) {
            throw new BadRequestException("非法参数");
        }
        // 2.分页查询
        Page<InteractionReply> page = lambdaQuery()
                // 如果为回答则AnswerId为0,如果不加限制条件会将问题下所有回答的所有回复也查出来
                .eq(InteractionReply::getAnswerId, query.getAnswerId() != null ? query.getAnswerId() : 0L)
                .eq(query.getQuestionId() != null, InteractionReply::getQuestionId, query.getQuestionId())
                .eq(InteractionReply::getHidden, false)
                //分页查询时默认要按照点赞次数排序
                .page(query.toMpPage(
                        new OrderItem("liked_times", false),
                        new OrderItem("create_time", true)));
        List<InteractionReply> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.根据id查询提问者和最近一次回答的信息
        Set<Long> userIds = new HashSet<>();
        Set<Long> targetReplyIds = new HashSet<>();
        // 3.1.得到问题当中的提问者id和最近一次回答的id
        for (InteractionReply q : records) {
            if(!q.getAnonymity()) { // 只查询非匿名的问题
                userIds.add(q.getUserId());
            }
            if(q.getTargetReplyId() != null && !q.getTargetReplyId().equals(0L)) {
                targetReplyIds.add(q.getTargetReplyId());
            }
            if(q.getTargetUserId() != null) {
                userIds.add(q.getTargetUserId());
            }
        }
        //3.2.根据id查询用户信息（评论者）
        userIds.remove(null);
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        //3.3根据id查询目标回复
        targetReplyIds.remove(null);
        Map<Long, InteractionReply> targetReplyMap = new HashMap<>(targetReplyIds.size());
        if(CollUtils.isNotEmpty(targetReplyIds)) {
            List<InteractionReply> list = list(Wrappers.<InteractionReply>lambdaQuery()
                    .in(InteractionReply::getId, targetReplyIds));
            targetReplyMap = list.stream()
                    .collect(Collectors.toMap(InteractionReply::getId, r -> r));
        }
        //3.4页面展示点赞按钮时，如果点赞过会高亮显示。因此我们要在返回值中标记当前用户是否点赞过这条评论或回答。
        List<Long> bizIds = records.stream().map(InteractionReply::getId).collect(Collectors.toList());
        Set<Long> bizLiked = remarkClient.isBizLiked(bizIds);

        // 4.封装VO
        List<ReplyVO> voList = new ArrayList<>(records.size());
        for (InteractionReply r : records) {
            // 4.1.将PO转为VO
            ReplyVO vo = BeanUtils.copyBean(r, ReplyVO.class);
            vo.setUserId(null);
            voList.add(vo);
            // 4.2.封装回复者信息
            if(!r.getAnonymity()) {
                UserDTO userDTO = userMap.get(r.getUserId());
                if (userDTO != null) {
                    vo.setUserId(userDTO.getId());
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }
            // 4.3 如果为评论并且目标回复不匿名则封装目标用户昵称
            if(r.getTargetReplyId() != null && !r.getTargetReplyId().equals(0L)) {
                InteractionReply interactionReply = targetReplyMap.get(r.getTargetReplyId());
                if(interactionReply != null && !interactionReply.getAnonymity()) {
                    UserDTO targerUserDto = userMap.get(r.getTargetUserId());
                    vo.setTargetUserName(targerUserDto.getName());
                }
            }
            // 4.4当前用户是否点赞过这条评论或回答
            if(bizLiked.contains(r.getId())) {
                vo.setLiked(true);
            }
        }

        return PageDTO.of(page, voList);
    }

    @Override
    public PageDTO<ReplyVO> queryReplyOrCommentAdmin(ReplyPageQuery query) {
        // 1.参数校验
        if(query == null || (query.getAnswerId() == null && query.getQuestionId() == null)) {
            throw new BadRequestException("非法参数");
        }
        // 2.分页查询
        Page<InteractionReply> page = lambdaQuery()
                // 如果为回答则AnswerId为0,如果不加限制条件会将问题下所有回答的所有回复也查出来
                .eq(InteractionReply::getAnswerId, query.getAnswerId() != null ? query.getAnswerId() : 0)
                .eq(query.getQuestionId() != null, InteractionReply::getQuestionId, query.getQuestionId())
                //.eq(InteractionReply::getHidden, false)
                //分页查询时默认要按照点赞次数排序
                .page(query.toMpPage("liked_times", false));
        List<InteractionReply> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.根据id查询提问者和最近一次回答的信息
        Set<Long> userIds = new HashSet<>();
        Set<Long> targetReplyIds = new HashSet<>();
        // 3.1.得到问题当中的提问者id和最近一次回答的id
        for (InteractionReply q : records) {
            userIds.add(q.getUserId());//无论匿名与否都要查
            if(q.getTargetReplyId() != null) {
                targetReplyIds.add(q.getTargetReplyId());
            }
            if(q.getTargetUserId() != null) {
                userIds.add(q.getTargetUserId());
            }
        }
        //3.2.根据id查询用户信息（评论者）
        userIds.remove(null);
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        //3.3页面展示点赞按钮时，如果点赞过会高亮显示。因此我们要在返回值中标记当前用户是否点赞过这条评论或回答。
        List<Long> bizIds = records.stream().map(InteractionReply::getId).collect(Collectors.toList());
        Set<Long> bizLiked = remarkClient.isBizLiked(bizIds);

        // 4.封装VO
        List<ReplyVO> voList = new ArrayList<>(records.size());
        for (InteractionReply r : records) {
            // 4.1.将PO转为VO
            ReplyVO vo = BeanUtils.copyBean(r, ReplyVO.class);
            vo.setUserId(null);
            voList.add(vo);
            // 4.2.封装回复者信息
            UserDTO userDTO = userMap.get(r.getUserId());
            if (userDTO != null) {
                vo.setUserId(userDTO.getId());
                vo.setUserName(userDTO.getName());
                vo.setUserIcon(userDTO.getIcon());
            }
            // 4.3如果为评论无论匿名与否都要封装目标用户昵称
            if(r.getTargetReplyId() != null) {
                UserDTO targerUserDto = userMap.get(r.getTargetUserId());
                vo.setTargetUserName(targerUserDto.getName());
            }
            else {
                // 4.4管理端在统计评论数量的时候，被隐藏的评论也要统计（用户端不统计隐藏回答）
                int replyTimes = count(Wrappers.<InteractionReply>lambdaQuery()
                        .eq(InteractionReply::getAnswerId, r.getId()));
                vo.setReplyTimes(replyTimes);
            }
            // 4.5当前用户是否点赞过这条评论或回答
            if(bizLiked.contains(r.getId())) {
                vo.setLiked(true);
            }
        }

        return PageDTO.of(page, voList);
    }

    @Override
    @Transactional
    public void hiddenOrShowReplyAdmin(Long id, boolean hidden) {
        //校验参数
        if(id == null) {
            throw new BadRequestException("非法参数");
        }
        InteractionReply reply = getById(id);
        if(reply == null) {
            throw new BadRequestException("非法参数");
        }
        if(reply.getHidden().equals(hidden)) {
            return;
        }
        //隐藏或显示回复or评论
        reply.setHidden(hidden);
        updateById(reply);
        //隐藏或显示回答时需要记录问题的回答数变化
        if(reply.getAnswerId().equals(0L)) {
            //操作回答
            InteractionQuestion question = questionMapper.selectById(reply.getQuestionId());
            if(question == null) {
                return;
            }
            if(hidden) {
                question.setAnswerTimes(question.getAnswerTimes() - 1);
            }
            else {
                question.setAnswerTimes(question.getAnswerTimes() + 1);
            }
            questionMapper.updateById(question);
        }
        //隐藏或显示评论时需要记录回答的评论数变化
        else {
            //操作评论
            InteractionReply answer = getById(reply.getAnswerId());
            if(answer == null) {
                return;
            }
            if(hidden) {
                answer.setReplyTimes(answer.getReplyTimes() - 1);
            }
            else {
                answer.setReplyTimes(answer.getReplyTimes() + 1);
            }
            updateById(answer);
        }
    }

    @Override
    public ReplyVO getReplyByIdAdmin(Long id) {
        //校验参数
        if(id == null) {
            throw new BadRequestException("非法参数");
        }
        InteractionReply reply = getById(id);
        if(reply == null) {
            throw new BadRequestException("非法参数");
        }
        //用户信息
        UserDTO userDTO = userClient.queryUserById(reply.getUserId());
        //vo
        ReplyVO vo = BeanUtils.copyBean(reply, ReplyVO.class);
        if(userDTO != null) {
            vo.setUserId(userDTO.getId());
            vo.setUserName(userDTO.getName());
            vo.setUserIcon(userDTO.getIcon());
        }
        //管理端在统计评论数量的时候，被隐藏的评论也要统计（用户端不统计隐藏回答）
        if(reply.getAnswerId().equals(0L)) {
            int replyTimes = count(Wrappers.<InteractionReply>lambdaQuery()
                    .eq(InteractionReply::getAnswerId, reply.getId()));
            vo.setReplyTimes(replyTimes);
        }
        return vo;
    }
}
