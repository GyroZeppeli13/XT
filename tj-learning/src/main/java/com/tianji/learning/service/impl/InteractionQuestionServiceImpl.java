package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.api.vo.course.CourseTeacherVO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author author
 * @since 2024-10-05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final IInteractionReplyService replyService;

    private final InteractionReplyMapper replyMapper;

    private final UserClient userClient;

    private final SearchClient searchClient;

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    private final CategoryCache categoryCache;

    @Override
    @Transactional
    public void saveQuestion(QuestionFormDTO questionDTO) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.数据转换
        InteractionQuestion question = BeanUtils.toBean(questionDTO, InteractionQuestion.class);
        // 3.补充数据
        question.setUserId(userId);
        // 4.保存问题
        save(question);
    }

    @Override
    @Transactional
    public void updateQuestion(Long id, QuestionFormDTO questionDTO) {
        if(id == null || questionDTO == null || StringUtils.isBlank(questionDTO.getTitle())
                || StringUtils.isBlank(questionDTO.getDescription()) || questionDTO.getAnonymity() == null) {
            throw new BadRequestException("非法参数");
        }
        InteractionQuestion question = getById(id);
        if(question == null) {
            throw new BadRequestException("非法参数");
        }
        //判断是否是当前用户提问的
        Long userId = UserContext.getUser();
        //如果不是则报错
        if(!question.getUserId().equals(userId)) {
            throw new BadRequestException("非法参数");
        }

        question.setTitle(questionDTO.getTitle());
        question.setDescription(questionDTO.getDescription());
        question.setAnonymity(questionDTO.getAnonymity());

        updateById(question);
    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        // 1.参数校验，课程id为空
        Long courseId = query.getCourseId();
        if (courseId == null) {
            throw new BadRequestException("课程id不能为空");
        }
        // 2.分页查询
        Long sectionId = query.getSectionId();
        Long userId = UserContext.getUser();
        Page<InteractionQuestion> page = lambdaQuery()
                // 指定不select的字段
                .select(InteractionQuestion.class, info -> !info.getProperty().equals("description"))//不select description字段
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, userId)
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.根据id查询提问者和最近一次回答的信息
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();
        // 3.1.得到问题当中的提问者id和最近一次回答的id
        for (InteractionQuestion q : records) {
            if(!q.getAnonymity()) { // 只查询非匿名的问题
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }
        // 3.2.根据id查询最近一次回答
        answerIds.remove(null);
        Map<Long, InteractionReply> replyMap = new HashMap<>(answerIds.size());
        if(CollUtils.isNotEmpty(answerIds)) {
//            List<InteractionReply> replies = replyMapper.selectBatchIds(answerIds);
            List<InteractionReply> replies = replyService.list(Wrappers.<InteractionReply>lambdaQuery()
                    .in(InteractionReply::getQuestionId, answerIds)
                    .eq(InteractionReply::getAnonymity, false));//回答为匿名则不显示
            for (InteractionReply reply : replies) {
                replyMap.put(reply.getId(), reply);
                if(!reply.getAnonymity()){ // 匿名用户不做查询
                    userIds.add(reply.getUserId());
                }
            }
        }

        // 3.3.根据id查询用户信息（提问者）
        userIds.remove(null);
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        // 4.封装VO
        List<QuestionVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion r : records) {
            // 4.1.将PO转为VO
            QuestionVO vo = BeanUtils.copyBean(r, QuestionVO.class);
            vo.setUserId(null);
            voList.add(vo);
            // 4.2.封装提问者信息
            if(!r.getAnonymity()){
                UserDTO userDTO = userMap.get(r.getUserId());
                if (userDTO != null) {
                    vo.setUserId(userDTO.getId());
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }

            // 4.3.封装最近一次回答的信息
            InteractionReply reply = replyMap.get(r.getLatestAnswerId());
            if (reply != null) {
                vo.setLatestReplyContent(reply.getContent());
                if(!reply.getAnonymity()){// 匿名用户直接忽略
                    UserDTO user = userMap.get(reply.getUserId());
                    vo.setLatestReplyUser(user.getName());
                }

            }
        }

        return PageDTO.of(page, voList);
    }

    @Override
    public QuestionVO queryQuestionById(Long id) {
        // 1.根据id查询数据
        if(id == null) {
            throw new BadRequestException("非法参数");
        }
        InteractionQuestion question = getById(id);
        // 2.数据校验
        if(question == null) {
            throw new BadRequestException("非法参数");
        }
        if(question.getHidden()){
            // 没有数据或者是被隐藏了
            return null;
        }
        // 3.查询提问者信息
        UserDTO user = null;
        if(!question.getAnonymity()){
            user = userClient.queryUserById(question.getUserId());
        }
        // 4.封装VO
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        return vo;
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        //查询问题是否存在
        if(id == null) {
            throw new BadRequestException("非法参数");
        }
        InteractionQuestion question = getById(id);
        //判断是否是当前用户提问的
        Long userId = UserContext.getUser();
        //如果不是则报错
        if(!question.getUserId().equals(userId)) {
            throw new BadRequestException("非法参数");
        }
        //如果是则删除问题
        removeById(id);
        //然后删除问题下的回答及评论
        replyService.remove(Wrappers.<InteractionReply>lambdaQuery()
                .eq(InteractionReply::getQuestionId, id));
    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        // 1.处理课程名称，得到课程id
        List<Long> courseIds = null;
        if (StringUtils.isNotBlank(query.getCourseName())) {
            courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if (CollUtils.isEmpty(courseIds)) {
                return PageDTO.empty(0L, 0L);
            }
        }
        // 2.分页查询
        Integer status = query.getStatus();
        LocalDateTime begin = query.getBeginTime();
        LocalDateTime end = query.getEndTime();
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .gt(begin != null, InteractionQuestion::getCreateTime, begin)
                .lt(end != null, InteractionQuestion::getCreateTime, end)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }

        // 3.准备VO需要的数据：用户数据、课程数据、章节数据
        Set<Long> userIds = new HashSet<>();
        Set<Long> cIds = new HashSet<>();
        Set<Long> cataIds = new HashSet<>();
        // 3.1.获取各种数据的id集合
        for (InteractionQuestion q : records) {
            userIds.add(q.getUserId());
            cIds.add(q.getCourseId());
            cataIds.add(q.getChapterId());
            cataIds.add(q.getSectionId());
        }
        // 3.2.根据id查询用户
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userMap = new HashMap<>(users.size());
        if (CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        // 3.3.根据id查询课程
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        Map<Long, CourseSimpleInfoDTO> cInfoMap = new HashMap<>(cInfos.size());
        if (CollUtils.isNotEmpty(cInfos)) {
            cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }

        // 3.4.根据id查询章节
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }


        // 4.封装VO
        List<QuestionAdminVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion q : records) {
            // 4.1.将PO转VO，属性拷贝
            QuestionAdminVO vo = BeanUtils.copyBean(q, QuestionAdminVO.class);
            voList.add(vo);
            // 4.2.用户信息
            UserDTO user = userMap.get(q.getUserId());
            if (user != null) {
                vo.setUserName(user.getName());
            }
            // 4.3.课程信息以及分类信息
            CourseSimpleInfoDTO cInfo = cInfoMap.get(q.getCourseId());
            if (cInfo != null) {
                vo.setCourseName(cInfo.getName());
                vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
            }
            // 4.4.章节信息
            vo.setChapterName(cataMap.getOrDefault(q.getChapterId(), ""));
            vo.setSectionName(cataMap.getOrDefault(q.getSectionId(), ""));
        }
        return PageDTO.of(page, voList);
    }

    @Override
    @Transactional
    public void hiddenOrShowQuestionAdmin(Long id, boolean hidden) {
        //校验参数
        if(id == null) {
            throw new BadRequestException("非法参数");
        }
        InteractionQuestion question = getById(id);
        if(question == null) {
            throw new BadRequestException("非法参数");
        }
        if(question.getHidden().equals(hidden)) {
            return;
        }
        //隐藏或显示问题
        question.setHidden(hidden);
        updateById(question);
    }

    @Override
    public QuestionAdminVO queryQuestionByIdAdmin(Long id) {
        // 1.根据id查询数据
        if(id == null) {
            throw new BadRequestException("非法参数");
        }
        InteractionQuestion question = getById(id);
        // 2.数据校验
        if(question == null) {
            throw new BadRequestException("非法参数");
        }
        // 3.查询提问者信息
        UserDTO user = userClient.queryUserById(question.getUserId());
        // 4.查询课程信息
        CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(question.getCourseId(), true, false);
        // 5.查询章节信息
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(List.of(question.getChapterId(), question.getSectionId()));
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        // 6.查询教师信息
        List<CourseTeacherVO> teachers = courseClient.teacher(question.getCourseId(), true);
        StringBuilder sb = new StringBuilder();
        for (CourseTeacherVO teacher : teachers) {
            sb.append(teacher.getName()).append("/");
        }
        String teacherNames = sb.deleteCharAt(sb.length() - 1).toString();

        // 7.封装VO
        QuestionAdminVO vo = BeanUtils.copyBean(question, QuestionAdminVO.class);
        //用户信息
        if (user != null) {
            vo.setUserId(user.getId());
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        //课程信息
        if(courseInfo != null) {
            vo.setCourseName(courseInfo.getName());
            vo.setCategoryName(categoryCache.getCategoryNames(courseInfo.getCategoryIds()));
        }
        // 章节信息
        vo.setChapterName(cataMap.getOrDefault(question.getChapterId(), ""));
        vo.setSectionName(cataMap.getOrDefault(question.getSectionId(), ""));
        // 教师信息
        vo.setTeacherName(teacherNames);

        //管理端在统计回答数量的时候，被隐藏的回答也要统计（用户端不统计隐藏回答）
        int answerTimes = replyService.count(Wrappers.<InteractionReply>lambdaQuery()
                .eq(InteractionReply::getAnswerId, 0L)//只查问题回答不包括问题回答的回复
                .eq(InteractionReply::getQuestionId, question.getId()));
        vo.setAnswerTimes(answerTimes);

        // 8.每当调用根据id查询问题接口，我们可以认为管理员查看了该问题，应该将问题status标记为已查看
        if(question.getStatus().equals(QuestionStatus.UN_CHECK)) {
            question.setStatus(QuestionStatus.CHECKED);
            updateById(question);
        }

        return vo;
    }
}
