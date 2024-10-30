package com.tianji.exam.domain.vo;

import com.tianji.api.dto.exam.QuestionDTO;
import com.tianji.exam.domain.po.Question;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "问题详情数据")
public class ExamDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "学员答案，格式如1,2,3")
    private String answer;

    @ApiModelProperty(value = "老师评语")
    private String comment;

    @ApiModelProperty(value = "是否正确")
    private Boolean correct;

    @ApiModelProperty(value = "学员得分")
    private Integer score;

    @ApiModelProperty(value = "问题详细信息")
    private QuestionDTO question;
}
