package com.tianji.exam.domain.vo;

import com.tianji.api.dto.exam.QuestionDTO;
import com.tianji.exam.domain.po.Question;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "考试记录数据")
public class ExamQuestionVO {

    @ApiModelProperty("考试id")
    private Long id;

    @ApiModelProperty("考试题目集合")
    private List<QuestionDTO> questions;
}
