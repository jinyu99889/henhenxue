package com.hengxue.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hengxue.auth.infrastructure.persistence.entity.SysUserRoleEntity;
import org.apache.ibatis.annotations.Mapper;

/** 用户角色关联表的 MyBatis Mapper。 */
@Mapper
public interface AuthUserRoleMapper extends BaseMapper<SysUserRoleEntity> {
}
