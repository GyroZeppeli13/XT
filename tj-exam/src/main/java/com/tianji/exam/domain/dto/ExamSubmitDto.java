package com.tianji.exam.domain.dto;

import com.tianji.exam.domain.po.ExamDetail;
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
@ApiModel(description = "考试数据提交对象")
public class ExamSubmitDto {

    @ApiModelProperty("考试id")
    private Long id;

    @ApiModelProperty("考试题目答案集合")
    private List<ExamDetail> examDetails;
}
