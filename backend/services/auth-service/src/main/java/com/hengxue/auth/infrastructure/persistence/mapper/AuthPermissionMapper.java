package com.hengxue.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hengxue.auth.infrastructure.persistence.entity.SysPermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/** 权限表的 MyBatis Mapper。 */
@Mapper
public interface AuthPermissionMapper extends BaseMapper<SysPermissionEntity> {
}
