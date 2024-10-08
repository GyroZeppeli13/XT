package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 管理端互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author author
 * @since 2024-10-05
 */
@Api(tags = "互动问题的回答或评论管理端相关接口")
@RestController
@RequestMapping("/admin/replies")
@RequiredArgsConstructor
public class InteractionReplyAdminController {

    private final IInteractionReplyService replyService;

    @ApiOperation("管理端分页查询回答或评论列表")
    @GetMapping("/page")
    public PageDTO<ReplyVO> queryReplyOrCommentAdmin(ReplyPageQuery query) {
        return replyService.queryReplyOrCommentAdmin(query);
    }

    @ApiOperation("管理端显示或隐藏评论")
    @PutMapping("/{id}/hidden/{hidden}")
    public void hiddenOrShowReplyAdmin(@PathVariable("id") Long id, @PathVariable("hidden") boolean hidden) {
        replyService.hiddenOrShowReplyAdmin(id, hidden);
    }

    @ApiOperation("管理端根据id查询回答")
    @GetMapping("/{id}")
    public ReplyVO getReplyByIdAdmin(@PathVariable("id") Long id) {
        return replyService.getReplyByIdAdmin(id);
    }
}
