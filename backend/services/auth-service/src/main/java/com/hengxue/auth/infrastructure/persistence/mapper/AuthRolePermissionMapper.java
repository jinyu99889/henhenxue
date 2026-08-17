package com.hengxue.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hengxue.auth.infrastructure.persistence.entity.SysRolePermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/** 角色权限关联表的 MyBatis Mapper。 */
@Mapper
public interface AuthRolePermissionMapper extends BaseMapper<SysRolePermissionEntity> {
}
