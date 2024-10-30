package com.tianji.exam.domain.po;

import com.tianji.exam.constants.QuestionType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * <p>
 * 考试详情数据传输对象
 * </p>
 *
 * @author 虎哥
 * @since 2022-10-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "考试详情数据")
public class ExamDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("题目id")
    @NotNull(message = "题目id不能为空")
    private Long questionId;

    @ApiModelProperty("题目类型，1：单选题，2：多选题，3：不定向选择题，4：判断题，5：主观题")
    @NotNull(message = "题目类型不能为空")
    private QuestionType questionType;

    @ApiModelProperty("题目答案")
    private String answer;
}
