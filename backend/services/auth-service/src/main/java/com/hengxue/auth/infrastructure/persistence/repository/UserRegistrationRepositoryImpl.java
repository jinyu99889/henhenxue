package com.hengxue.auth.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hengxue.auth.application.support.SnowflakeIdGenerator;
import com.hengxue.auth.domain.entity.NewUser;
import com.hengxue.auth.domain.repository.UserRegistrationRepository;
import com.hengxue.auth.infrastructure.persistence.entity.SysRoleEntity;
import com.hengxue.auth.infrastructure.persistence.entity.SysUserEntity;
import com.hengxue.auth.infrastructure.persistence.entity.SysUserIdentityEntity;
import com.hengxue.auth.infrastructure.persistence.entity.SysUserRoleEntity;
import com.hengxue.auth.infrastructure.persistence.mapper.AuthRoleMapper;
import com.hengxue.auth.infrastructure.persistence.mapper.AuthUserIdentityMapper;
import com.hengxue.auth.infrastructure.persistence.mapper.AuthUserMapper;
import com.hengxue.auth.infrastructure.persistence.mapper.AuthUserRoleMapper;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/** 组合用户、身份和角色 Mapper 的注册仓储实现。 */
@Repository
public class UserRegistrationRepositoryImpl implements UserRegistrationRepository {

    private static final String USER_ROLE_CODE = "USER";

    @Autowired private AuthUserMapper userMapper;
    @Autowired private AuthUserIdentityMapper identityMapper;
    @Autowired private AuthRoleMapper roleMapper;
    @Autowired private AuthUserRoleMapper userRoleMapper;
    @Autowired private SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 判断用户名是否已被活跃用户使用。
     *
     * @param username 规范化后的用户名
     * @return 用户名存在时为 {@code true}
     */
    @Override
    public boolean existsActiveUsername(String username) {
        return userMapper.selectCount(Wrappers.<SysUserEntity>lambdaQuery()
                .eq(SysUserEntity::getUsername, username)
                .isNull(SysUserEntity::getDeletedAt)) > 0;
    }

    /**
     * 判断邮箱是否已被活跃用户使用。
     *
     * @param email 规范化后的邮箱
     * @return 邮箱存在时为 {@code true}
     */
    @Override
    public boolean existsActiveEmail(String email) {
        return userMapper.selectCount(Wrappers.<SysUserEntity>lambdaQuery()
                .eq(SysUserEntity::getEmail, email)
                .isNull(SysUserEntity::getDeletedAt)) > 0;
    }

    /**
     * 查询可用普通用户角色。
     *
     * @return 普通用户角色 ID；不存在时为 {@code null}
     */
    @Override
    public String findActiveUserRoleId() {
        SysRoleEntity role = roleMapper.selectOne(Wrappers.<SysRoleEntity>lambdaQuery()
                .eq(SysRoleEntity::getCode, USER_ROLE_CODE)
                .eq(SysRoleEntity::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        return role == null ? null : role.getId();
    }

    /**
     * 创建用户、密码身份和普通用户角色关联。
     *
     * @param user 待创建用户
     * @param passwordHash BCrypt 密码哈希
     * @param roleId 普通用户角色 ID
     * @param now 当前 UTC 时间
     */
    @Override
    public void create(NewUser user, String passwordHash, String roleId, LocalDateTime now) {
        SysUserEntity userEntity = new SysUserEntity();
        userEntity.setId(user.id());
        userEntity.setUsername(user.username());
        userEntity.setEmail(user.email());
        userEntity.setEmailVerifiedAt(now);
        userEntity.setNickname(user.nickname());
        userEntity.setStatus("ACTIVE");
        userEntity.setCreatedAt(now);
        userEntity.setUpdatedAt(now);
        userEntity.setVersion(1);
        userMapper.insert(userEntity);

        SysUserIdentityEntity identityEntity = new SysUserIdentityEntity();
        identityEntity.setId(snowflakeIdGenerator.next());
        identityEntity.setUserId(user.id());
        identityEntity.setProvider("PASSWORD");
        identityEntity.setIdentifier(user.email());
        identityEntity.setPasswordHash(passwordHash);
        identityEntity.setVerifiedAt(now);
        identityEntity.setCreatedAt(now);
        identityMapper.insert(identityEntity);

        SysUserRoleEntity userRoleEntity = new SysUserRoleEntity();
        userRoleEntity.setUserId(user.id());
        userRoleEntity.setRoleId(roleId);
        userRoleMapper.insert(userRoleEntity);
    }
}
