package com.tianji.exam.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.exam.domain.dto.ExamDto;
import com.tianji.exam.domain.dto.ExamSubmitDto;
import com.tianji.exam.domain.query.ExamPageQuery;
import com.tianji.exam.domain.vo.ExamDetailVO;
import com.tianji.exam.domain.vo.ExamPageVO;
import com.tianji.exam.domain.vo.ExamQuestionVO;
import com.tianji.exam.service.IExamService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author author
 * @since 2024-10-30
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/exams")
@Api(tags = "考试相关接口")
public class ExamController {

    private final IExamService examService;

    @ApiOperation("获取试题并开始考试")
    @PostMapping
    public ExamQuestionVO beginExamWithQuestions(@RequestBody ExamDto dto) {
        return examService.beginExamWithQuestions(dto);
    }

    @ApiOperation("提交考试结果")
    @PostMapping("/details")
    public void submitExam(@RequestBody ExamSubmitDto dto) {
        examService.submitExam(dto);
    }

    @ApiOperation("分页查询考试记录")
    @GetMapping("/page")
    public PageDTO<ExamPageVO> queryExamByPage(@RequestBody ExamPageQuery query) {
        return examService.queryExamByPage(query);
    }

    @ApiOperation("查询考试记录详情")
    @GetMapping("/{id}")
    public List<ExamDetailVO> getExamDetail(@PathVariable("id") Long id) {
        return examService.getExamDetail(id);
    }
}
