package com.hengxue.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hengxue.auth.infrastructure.persistence.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Mapper;

/** 角色表的 MyBatis Mapper。 */
@Mapper
public interface AuthRoleMapper extends BaseMapper<SysRoleEntity> {
}
