package com.hengxue.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hengxue.auth.infrastructure.persistence.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;

/** 用户主体表的 MyBatis Mapper。 */
@Mapper
public interface AuthUserMapper extends BaseMapper<SysUserEntity> {
}
