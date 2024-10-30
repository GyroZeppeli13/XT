package com.tianji.exam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.learning.LearningClient;
import com.tianji.api.client.trade.TradeClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.exam.QuestionDTO;
import com.tianji.api.dto.leanring.LearningRecordFormDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.exam.domain.dto.ExamDto;
import com.tianji.exam.domain.dto.ExamSubmitDto;
import com.tianji.exam.domain.po.Exam;
import com.tianji.exam.domain.po.ExamDetail;
import com.tianji.exam.domain.po.ExamQuestion;
import com.tianji.exam.domain.po.Question;
import com.tianji.exam.domain.query.ExamPageQuery;
import com.tianji.exam.domain.vo.ExamDetailVO;
import com.tianji.exam.domain.vo.ExamPageVO;
import com.tianji.exam.domain.vo.ExamQuestionVO;
import com.tianji.exam.mapper.ExamMapper;
import com.tianji.exam.service.IExamQuestionService;
import com.tianji.exam.service.IExamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.exam.service.IQuestionDetailService;
import com.tianji.exam.service.IQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2024-10-30
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements IExamService {

    private final TradeClient tradeClient;
    private final IQuestionService questionService;
    private final IExamQuestionService examQuestionService;
    private final LearningClient learningClient;
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;

    @Override
    public ExamQuestionVO beginExamWithQuestions(ExamDto dto) {
        // 获取当前用户id
        Long userId = UserContext.getUser();
        // 验证用户是否购买了该课程
        if(!tradeClient.checkMyLesson(dto.getCourseId())) {
            throw new BizIllegalException("用户未购买该课程");
        }
        // 验证用户是否重复考试
        Exam exam = getOne(Wrappers.<Exam>lambdaQuery()
                .eq(Exam::getUserId, userId)
                .eq(Exam::getCourseId, dto.getCourseId())
                .eq(Exam::getSectionId, dto.getSectionId())
                .eq(Exam::getType, dto.getType()));
        if(exam != null && exam.getIsCommit()) {
            throw new BizIllegalException("用户重复考试");
        }
        // 如果考试记录不存在，新增考试记录
        if(exam == null) {
            exam = new Exam();
            exam.setCourseId(dto.getCourseId());
            exam.setSectionId(dto.getSectionId());
            exam.setType(dto.getType());
            exam.setUserId(userId);
            save(exam);
        }
        // 封装vo
        ExamQuestionVO vo = new ExamQuestionVO();
        // 查询题目信息
        List<QuestionDTO> questionDTOS = questionService.queryQuestionByBizId(dto.getSectionId());
        vo.setQuestions(questionDTOS);
        vo.setId(exam.getId());
        return vo;
    }

    @Override
    public void submitExam(ExamSubmitDto dto) {
        Exam exam = getById(dto.getId());
        // 考试记录是否存在
        if(exam == null) {
            throw new BadRequestException("考试记录不存在");
        }
        // 考试记录是否属于当前用户
        Long userId = UserContext.getUser();
        if(!userId.equals(exam.getUserId())) {
            throw new BadRequestException("考试记录不属于当前用户");
        }
        // 考试记录是否重复提交
        if(exam.getIsCommit()) {
            throw new BadRequestException("考试记录重复提交");
        }
        // 自动批阅考试试题
        List<ExamDetail> examDetails = dto.getExamDetails();
        List<Long> ids = examDetails.stream().map(ExamDetail::getQuestionId).collect(Collectors.toList());
        List<QuestionDTO> questionDTOS = questionService.queryQuestionByIds(ids);
        Map<Long, QuestionDTO> map = questionDTOS.stream().collect(Collectors
                .toMap(QuestionDTO::getId, q -> q));
        List<ExamQuestion> list = new ArrayList<>();
        for(ExamDetail examDetail : examDetails) {
            ExamQuestion examQuestion = new ExamQuestion();
            examQuestion.setExamId(dto.getId());
            examQuestion.setQuestionId(examDetail.getQuestionId());
            examQuestion.setAnswer(examDetail.getAnswer());
            //判断题目回答是否正确
            String answer = examDetail.getAnswer();
            QuestionDTO questionDTO = map.get(examDetail.getQuestionId());
            String correctAnswer = questionDTO.getAnswer();
            if(answer.equals(correctAnswer)) {
                examQuestion.setCorrect(true);
                examQuestion.setScore(questionDTO.getScore());
            }
            else {
                examQuestion.setCorrect(false);
                examQuestion.setScore(0);
            }
        }
        // 插入考试回答记录
        examQuestionService.saveBatch(list);
        // 更新考试记录
        exam.setIsCommit(true);
        exam.setCommitTime(LocalDateTime.now());
        int sum = list.stream().mapToInt(ExamQuestion::getScore).sum();
        exam.setScore(sum);
        updateById(exam);
        // 通知学习中心提交学习记录（用mq异步通知更好）
        LearningRecordFormDTO learningRecordFormDTO = new LearningRecordFormDTO();
        learningRecordFormDTO.setSectionType(1);
        learningRecordFormDTO.setSectionId(exam.getSectionId());
        learningRecordFormDTO.setCommitTime(LocalDateTime.now());
        learningClient.addLearningRecord(learningRecordFormDTO);
        // todo 记录问题回答次数和回答正确次数
    }

    @Override
    public PageDTO<ExamPageVO> queryExamByPage(ExamPageQuery query) {
        if(query == null) {
            throw new BadRequestException("非法参数");
        }
        // 分页查询exam信息
        Page<Exam> page = lambdaQuery().page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<Exam> records = page.getRecords();
        // 查询课程和小节信息
        Set<Long> courseIds = records.stream().map(Exam::getCourseId).collect(Collectors.toSet());
        Set<Long> sectionIds = records.stream().map(Exam::getSectionId).collect(Collectors.toSet());
        // 查询课程
        List<CourseSimpleInfoDTO> simpleInfoList = courseClient.getSimpleInfoList(courseIds);
        Map<Long, String> courseMap = simpleInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, CourseSimpleInfoDTO::getName));
        // 查询小节
        List<CataSimpleInfoDTO> cataSimpleInfoDTOS = catalogueClient.batchQueryCatalogue(sectionIds);
        Map<Long, String> sectionMap = cataSimpleInfoDTOS.stream()
                .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        // 封装vo
        List<ExamPageVO> list = new ArrayList<>(records.size());
        for(Exam exam : records) {
            ExamPageVO vo = BeanUtils.copyBean(exam, ExamPageVO.class);
            vo.setCourseName(courseMap.getOrDefault(exam.getCourseId(), null));
            vo.setSectionName(sectionMap.getOrDefault(exam.getSectionId(), null));
            long duration = Duration.between(exam.getCreateTime(), exam.getCommitTime()).getSeconds();
            vo.setDuration(duration);
            list.add(vo);
        }
        return PageDTO.of(page, list);
    }

    @Override
    public List<ExamDetailVO> getExamDetail(Long id) {
        // 查询ExamQuestion
        List<ExamQuestion> list = examQuestionService.list(Wrappers.<ExamQuestion>lambdaQuery()
                .eq(ExamQuestion::getExamId, id));
        // 查询question信息
        List<Long> ids = list.stream().map(ExamQuestion::getQuestionId).collect(Collectors.toList());
        List<QuestionDTO> questionDTOS = questionService.queryQuestionByIds(ids);
        Map<Long, QuestionDTO> questionDTOMap = questionDTOS.stream()
                .collect(Collectors.toMap(QuestionDTO::getId, q -> q));
        // 封装vo
        List<ExamDetailVO> vos = new ArrayList<>();
        for(ExamQuestion examQuestion : list) {
            ExamDetailVO vo = BeanUtils.copyBean(examQuestion, ExamDetailVO.class);
            QuestionDTO questionDTO = questionDTOMap.get(examQuestion.getQuestionId());
            vo.setQuestion(questionDTO);
        }
        return vos;
    }
}
