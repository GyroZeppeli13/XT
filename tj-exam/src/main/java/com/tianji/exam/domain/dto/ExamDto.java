package com.tianji.exam.domain.dto;

import com.tianji.exam.enums.ExamStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * <p>
 * 考试数据传输对象
 * </p>
 *
 * @author 虎哥
 * @since 2022-10-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "考试数据传输对象")
public class ExamDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("课程id")
    @NotNull(message = "课程id不能为空")
    private Long courseId;

    @ApiModelProperty("小节id")
    @NotNull(message = "小节id不能为空")
    private Long sectionId;

    @ApiModelProperty("考试类型，1-练习，2-考试")
    @NotNull(message = "考试类型不能为空")
    private ExamStatus type;
}
