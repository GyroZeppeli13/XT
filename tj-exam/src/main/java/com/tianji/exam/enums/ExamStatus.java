package com.tianji.exam.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ExamStatus {
    PRACTICE(1, "练习"),
    EXAMINATION(2, "考试"),
    ;
    @JsonValue
    @EnumValue
    int value;
    String desc;

    ExamStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
