package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSearchDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.Constant;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.*;
import com.tianji.learning.domain.dto.NoteFormDTO;
import com.tianji.learning.domain.po.Note;
import com.tianji.learning.domain.query.NoteAdminPageQuery;
import com.tianji.learning.domain.query.NotePageQuery;
import com.tianji.learning.domain.vo.NoteAdminDetailVO;
import com.tianji.learning.domain.vo.NoteAdminVO;
import com.tianji.learning.domain.vo.NoteVO;
import com.tianji.learning.mapper.NoteMapper;
import com.tianji.learning.service.INoteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tianji.common.constants.MqConstants.Exchange.LEARNING_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.NOTE_GATHERED;
import static com.tianji.common.constants.MqConstants.Key.WRITE_NOTE;

/**
 * <p>
 * 存储笔记信息 服务实现类
 * </p>
 *
 * @author author
 * @since 2024-10-27
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements INoteService {

    private final CourseClient courseClient;
    private final SearchClient searchClient;
    private final CatalogueClient catalogueClient;
    private final UserClient userClient;
    private final RabbitMqHelper mqHelper;
    private final CategoryCache categoryCache;

    @Override
    @Transactional
    public void saveNote(NoteFormDTO noteDTO) {
        // 获取userId
        Long userId = UserContext.getUser();
        // 封装po
        Note note = BeanUtils.copyBean(noteDTO, Note.class);
        note.setUserId(userId);
        note.setAuthorId(userId);
        // 保存
        save(note);
        // 发送mq消息,记录笔记得3分
        mqHelper.send(LEARNING_EXCHANGE, WRITE_NOTE, userId);
    }

    @Override
    @Transactional
    public void gatherNote(Long id) {
        // 获取userId
        Long userId = UserContext.getUser();
        // 获取需要采集的笔记
        Note note = getById(id);
        if(note == null || note.getHidden() || note.getIsPrivate()) {
            throw new BadRequestException("该笔记不存在或者不能采集");
        }
        // 封装
        note.setIsPrivate(true);
        note.setIsGathered(true);
        note.setGatheredNoteId(id);
        note.setAuthorId(note.getUserId());
        note.setUserId(userId);
        note.setId(null);
        // 保存
        save(note);
        // 发送mq消息
        // 记录笔记得3分
        mqHelper.send(LEARNING_EXCHANGE, WRITE_NOTE, userId);
        // 笔记被采集得2分
        mqHelper.send(LEARNING_EXCHANGE, NOTE_GATHERED, note.getAuthorId());
    }

    @Override
    @Transactional
    public void removeGatherNote(Long id) {
        // 获取userId
        Long userId = UserContext.getUser();
        // 获取需要取消采集的笔记
        // 1.笔记删除条件
        LambdaUpdateWrapper<Note> queryWrapper =
                Wrappers.lambdaUpdate(Note.class)
                        .eq(Note::getUserId, userId)
                        .eq(Note::getGatheredNoteId, id);
        // 2.删除笔记
        baseMapper.delete(queryWrapper);
    }

    @Override
    @Transactional
    public void updateNote(NoteFormDTO noteDTO) {
        // 获取userId
        Long userId = UserContext.getUser();
        // 获取需要更新的笔记
        Note note = getById(noteDTO.getId());
        if(note == null || !note.getUserId().equals(userId)) {
            throw new BadRequestException("该笔记不存在或者不能被更新");
        }
        // 封装po
        Note po = BeanUtils.copyBean(noteDTO, Note.class);
        // 采集笔记不能设置公开
        if (noteDTO.getIsPrivate() != null) {
            po.setIsPrivate(note.getIsGathered() || noteDTO.getIsPrivate());
        }
        //更新笔记
        updateById(po);
    }

    @Override
    public void removeMyNote(Long id) {
        // 获取userId
        Long userId = UserContext.getUser();
        // 获取需要更新的笔记
        Note note = getById(id);
        if(note == null || !note.getUserId().equals(userId)) {
            throw new BadRequestException("该笔记不存在或者不能被删除");
        }
        // 删除笔记
        removeById(id);
    }

    @Override
    public PageDTO<NoteVO> queryNotePage(NotePageQuery query) {
        // 校验参数
        if(query == null) {
            throw new BadRequestException("非法参数");
        }
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if(courseId == null && sectionId == null) {
            throw new BadRequestException("课程id和小节id不能同时为空");
        }
        // 获取userId
        Page<Note> page = new Page<>(query.getPageNo(), query.getPageSize());
        Long userId = UserContext.getUser();
        // 分页查询
        page = query.getOnlyMine() ?
                lambdaQuery()
                .eq(Note::getHidden, false)
                .eq(Note::getUserId, userId)
                .eq(courseId != null, Note::getCourseId, courseId)
                .eq(sectionId != null, Note::getSectionId, sectionId)
                .orderByAsc(sectionId != null, Note::getNoteMoment)
                .orderByDesc(sectionId == null, Note::getId)
                .page(page)
                : baseMapper.queryNotePageBySectionId(page, userId, courseId, sectionId);
        return parseNotePages(page);
    }

    private PageDTO<NoteVO> parseNotePages(Page<Note> page) {
        // 校验
        List<Note> noteList = page.getRecords();
        if(CollUtils.isEmpty(noteList)) {
            return PageDTO.empty(page);
        }
        // 查询作者信息
        Set<Long> ids = noteList.stream().map(Note::getUserId).collect(Collectors.toSet());
        List<UserDTO> userDTOS = userClient.queryUserByIds(ids);
        Map<Long, UserDTO> userDTOMap = new HashMap<>();
        if(CollUtils.isNotEmpty(userDTOS)) {
            userDTOMap = userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        //封装vo
        List<NoteVO> list = new ArrayList<>(noteList.size());
        for(Note note : noteList) {
            NoteVO vo = BeanUtils.copyBean(note, NoteVO.class);
            UserDTO userDTO = userDTOMap.get(note.getUserId());
            if(userDTO != null) {
                vo.setAuthorId(userDTO.getId());
                vo.setAuthorName(userDTO.getName());
                vo.setAuthorIcon(userDTO.getIcon());
            }
            vo.setIsGathered(BooleanUtils.isTrue(note.getIsGathered()));
            list.add(vo);
        }
        return PageDTO.of(page, list);
    }

    @Override
    public PageDTO<NoteAdminVO> queryNotePageForAdmin(NoteAdminPageQuery query) {
        // 1.分页条件
        Page<Note> page = new Page<>(query.getPageNo(), query.getPageSize());
        // 2.课程名称
        List<Long> courseIdList = null;
        if(StringUtils.isNotEmpty(query.getName())){
            // 2.1.查询课程信息
            courseIdList = searchClient.queryCoursesIdByName(query.getName());
            // 2.2.判空
            if(CollUtils.isEmpty(courseIdList)){
                return PageDTO.empty(page);
            }
        }
        // 3.排序条件
        if (StringUtils.isNotBlank(query.getSortBy())) {
            page.addOrder(new OrderItem(query.getSortBy(), query.getIsAsc()));
        } else {
            page.addOrder(new OrderItem(Constant.DATA_FIELD_NAME_CREATE_TIME, false));
        }
        // 4.搜索条件
        LocalDateTime beginTime = query.getBeginTime();
        LocalDateTime endTime = query.getEndTime();
        QueryWrapper<Note> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .in(CollUtils.isNotEmpty(courseIdList), Note::getCourseId, courseIdList)
                .eq(query.getHidden() != null, Note::getHidden, query.getHidden())
                .ge(beginTime != null, Note::getCreateTime, beginTime)
                .le(endTime != null, Note::getCreateTime, endTime);
        // 5.查询
        page = baseMapper.queryNotePage(page, wrapper);
        List<Note> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 6.数据处理
        List<NoteAdminVO> list = new ArrayList<>(records.size());
        // 6.1.获取问题关联的id信息
        // 课程id
        Set<Long> courseIds = new HashSet<>();
        // 章节id
        Set<Long> csIds = new HashSet<>();
        // 用户id
        Set<Long> uIds = new HashSet<>();
        for (Note r : records) {
            courseIds.add(r.getCourseId());
            if (r.getChapterId() != null) csIds.add(r.getChapterId());
            if (r.getSectionId() != null) csIds.add(r.getSectionId());
            uIds.add(r.getUserId());
        }
        // 6.1.获取课程信息
        List<CourseSimpleInfoDTO> courseInfos = courseClient.getSimpleInfoList(courseIds);
        Map<Long, String> courseMap = CollUtils.isEmpty(courseInfos) ?
                new HashMap<>() :
                courseInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, CourseSimpleInfoDTO::getName));
        // 6.2.获取章节信息
        List<CataSimpleInfoDTO> csInfos = catalogueClient.batchQueryCatalogue(csIds);
        Map<Long, String> csNameMap = CollUtils.isEmpty(csInfos) ?
                new HashMap<>() :
                csInfos.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        // 6.3.获取用户信息
        List<UserDTO> userInfos = userClient.queryUserByIds(uIds);
        Map<Long, String> userMap = CollUtils.isEmpty(userInfos) ?
                new HashMap<>() :
                userInfos.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        // 7.封装vo
        for (Note r : records) {
            NoteAdminVO v = BeanUtils.toBean(r, NoteAdminVO.class);
            v.setAuthorName(userMap.get(r.getUserId()));
            v.setCourseName(courseMap.get(r.getCourseId()));
            v.setChapterName(csNameMap.get(r.getChapterId()));
            v.setSectionName(csNameMap.get(r.getSectionId()));
            v.setUsedTimes(baseMapper.queryNoteGathersNum(r.getId()));
            list.add(v);
        }
        return new PageDTO<>(page.getTotal(), page.getPages(), list);
    }

    @Override
    public NoteAdminDetailVO queryNoteDetailForAdmin(Long id) {
        // 1.查询笔记
        Note note = getById(id);
        AssertUtils.isNotNull(note, "笔记不存在");
        // 2.转换VO
        NoteAdminDetailVO vo = BeanUtils.toBean(note, NoteAdminDetailVO.class);
        // 3.查询课程信息
        CourseFullInfoDTO courseInfo =
                courseClient.getCourseInfoById(note.getCourseId(), false, false);
        if (courseInfo != null) {
            // 3.1.课程信息
            vo.setCourseName(courseInfo.getName());
            // 3.2.课程分类信息
            List<Long> cateIds = List.of(
                    courseInfo.getFirstCateId(),
                    courseInfo.getSecondCateId(),
                    courseInfo.getThirdCateId());
            String categoryNames = categoryCache.getCategoryNames(cateIds);
            vo.setCategoryNames(categoryNames);
        }
        // 4.查询章节信息
        List<CataSimpleInfoDTO> cataInfos = catalogueClient
                .batchQueryCatalogue(List.of(note.getChapterId(), note.getSectionId()));
        if (cataInfos != null && cataInfos.size() == 2) {
            for (CataSimpleInfoDTO cataInfo : cataInfos) {
                if (cataInfo.getId().equals(note.getChapterId())) {
                    vo.setChapterName(cataInfo.getName());
                } else {
                    vo.setSectionName(cataInfo.getName());
                }
            }
        }
        // 5.查询用户信息
        // 5.1.查询采集过当前笔记的用户
        Set<Long> uIds = baseMapper.queryNoteGathers(id);
        // 5.2.当前笔记作者
        Long authorId = note.getAuthorId();
        uIds.add(authorId);
        // 5.3.查询用户
        uIds.remove(0L);
        List<UserDTO> users = userClient.queryUserByIds(uIds);
        if (users != null && users.size() == uIds.size()) {
            uIds.remove(authorId);
            // 填充作者信息
            users.stream().filter(u -> u.getId().equals(authorId)).findAny().ifPresent(u -> {
                vo.setAuthorName(u.getName());
                vo.setAuthorPhone(u.getCellPhone());
            });
            // 填充采集者信息
            List<String> gathers = users.stream()
                    .filter(u -> !u.getId().equals(authorId))
                    .map(UserDTO::getName).collect(Collectors.toList());
            vo.setGathers(gathers);
            vo.setUsedTimes(gathers.size());
        }
        return vo;
    }

    @Override
    public void hiddenNote(Long id, Boolean hidden) {
        Note note = new Note();
        note.setId(id);
        note.setHidden(hidden);
        updateById(note);
    }
}
