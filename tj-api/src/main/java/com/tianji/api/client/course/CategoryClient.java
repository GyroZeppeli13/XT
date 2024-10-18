package com.tianji.api.client.course;

import com.tianji.api.dto.course.CategoryBasicDTO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;

@FeignClient(contextId = "category",value = "course-service",path = "categorys")
public interface CategoryClient {

    /**
     * 获取所有课程及课程分类
     * @return  所有课程及课程分类
     */
    @GetMapping("getAllOfOneLevel")
    List<CategoryBasicDTO> getAllOfOneLevel();

    @GetMapping("getByIds")
    @ApiOperation("获取在ids中的所有课程分类")
    List<CategoryBasicDTO> getByIds(Set<Long> ids);
}
