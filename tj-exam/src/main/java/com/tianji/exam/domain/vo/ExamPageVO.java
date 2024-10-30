package com.tianji.exam.domain.vo;

import com.tianji.exam.enums.ExamStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 考试视图对象
 * </p>
 *
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "考试分页数据")
public class ExamPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("考试id")
    private Long id;

    @ApiModelProperty("考试类型（1：考试，2：练习）")
    private ExamStatus type;

    @ApiModelProperty("得分")
    private Integer score;

    @ApiModelProperty("提交时间")
    private LocalDateTime commitTime;

    @ApiModelProperty("考试用时（单位：秒）")
    private Long duration;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("小节名称")
    private String sectionName;
}
