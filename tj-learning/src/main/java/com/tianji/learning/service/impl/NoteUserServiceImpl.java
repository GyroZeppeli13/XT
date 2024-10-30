package com.tianji.learning.service.impl;

import com.tianji.learning.domain.po.NoteUser;
import com.tianji.learning.mapper.NoteUserMapper;
import com.tianji.learning.service.INoteUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 存储引用的笔记用户信息 服务实现类
 * </p>
 *
 * @author author
 * @since 2024-10-27
 */
@Service
public class NoteUserServiceImpl extends ServiceImpl<NoteUserMapper, NoteUser> implements INoteUserService {

}
