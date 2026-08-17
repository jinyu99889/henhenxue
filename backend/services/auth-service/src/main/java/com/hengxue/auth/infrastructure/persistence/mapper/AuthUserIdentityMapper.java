package com.hengxue.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hengxue.auth.infrastructure.persistence.entity.SysUserIdentityEntity;
import org.apache.ibatis.annotations.Mapper;

/** 密码身份表的 MyBatis Mapper。 */
@Mapper
public interface AuthUserIdentityMapper extends BaseMapper<SysUserIdentityEntity> {
}
