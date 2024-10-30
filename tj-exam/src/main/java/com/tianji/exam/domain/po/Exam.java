package com.tianji.exam.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianji.exam.enums.ExamStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("exam")
public class Exam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 考试id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 考试类型，1：考试，2：练习
     */
    private ExamStatus type;

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 小节id
     */
    private Long sectionId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 是否已提交
     */
    private Boolean isCommit;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 提交时间
     */
    private LocalDateTime commitTime;
}
