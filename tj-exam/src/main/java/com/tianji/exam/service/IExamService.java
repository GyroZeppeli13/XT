package com.tianji.exam.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.exam.domain.dto.ExamDto;
import com.tianji.exam.domain.dto.ExamSubmitDto;
import com.tianji.exam.domain.po.Exam;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.exam.domain.query.ExamPageQuery;
import com.tianji.exam.domain.vo.ExamDetailVO;
import com.tianji.exam.domain.vo.ExamPageVO;
import com.tianji.exam.domain.vo.ExamQuestionVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author author
 * @since 2024-10-30
 */
public interface IExamService extends IService<Exam> {

    ExamQuestionVO beginExamWithQuestions(ExamDto dto);

    void submitExam(ExamSubmitDto dto);

    PageDTO<ExamPageVO> queryExamByPage(ExamPageQuery query);

    List<ExamDetailVO> getExamDetail(Long id);
}
