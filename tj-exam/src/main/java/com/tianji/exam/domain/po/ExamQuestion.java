package com.tianji.exam.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author author
 * @since 2024-10-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exam_question")
@ApiModel(value="ExamQuestion对象", description="")
public class ExamQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "记录id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "考试id")
    private Long examId;

    @ApiModelProperty(value = "题目id")
    private Long questionId;

    @ApiModelProperty(value = "学员答案，格式如1,2,3")
    private String answer;

    @ApiModelProperty(value = "老师评语")
    private String comment;

    @ApiModelProperty(value = "是否正确")
    private Boolean correct;

    @ApiModelProperty(value = "学员得分")
    private Integer score;


}
