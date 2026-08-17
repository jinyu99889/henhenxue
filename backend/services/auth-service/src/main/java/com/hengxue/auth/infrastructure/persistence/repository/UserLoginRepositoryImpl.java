package com.hengxue.auth.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hengxue.auth.domain.entity.LoginUser;
import com.hengxue.auth.domain.repository.UserLoginRepository;
import com.hengxue.auth.infrastructure.persistence.entity.SysPermissionEntity;
import com.hengxue.auth.infrastructure.persistence.entity.SysRoleEntity;
import com.hengxue.auth.infrastructure.persistence.entity.SysRolePermissionEntity;
import com.hengxue.auth.infrastructure.persistence.entity.SysUserEntity;
import com.hengxue.auth.infrastructure.persistence.entity.SysUserIdentityEntity;
import com.hengxue.auth.infrastructure.persistence.entity.SysUserRoleEntity;
import com.hengxue.auth.infrastructure.persistence.mapper.AuthPermissionMapper;
import com.hengxue.auth.infrastructure.persistence.mapper.AuthRoleMapper;
import com.hengxue.auth.infrastructure.persistence.mapper.AuthRolePermissionMapper;
import com.hengxue.auth.infrastructure.persistence.mapper.AuthUserIdentityMapper;
import com.hengxue.auth.infrastructure.persistence.mapper.AuthUserMapper;
import com.hengxue.auth.infrastructure.persistence.mapper.AuthUserRoleMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/** 组合用户、身份、角色和权限 Mapper 的登录仓储实现。 */
@Repository
public class UserLoginRepositoryImpl implements UserLoginRepository {

    @Autowired
    private AuthUserMapper userMapper;

    @Autowired
    private AuthUserIdentityMapper identityMapper;

    @Autowired
    private AuthUserRoleMapper userRoleMapper;

    @Autowired
    private AuthRoleMapper roleMapper;

    @Autowired
    private AuthRolePermissionMapper rolePermissionMapper;

    @Autowired
    private AuthPermissionMapper permissionMapper;

    /**
     * 查询活跃用户和密码身份。
     *
     * @param account 已规范化的用户名或邮箱
     * @return 登录校验快照
     */
    @Override
    public Optional<LoginUser> findActiveByAccount(String account) {
        SysUserEntity user = userMapper.selectList(Wrappers.<SysUserEntity>lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(SysUserEntity::getUsername, account)
                        .or()
                        .eq(SysUserEntity::getEmail, account))
                .eq(SysUserEntity::getStatus, "ACTIVE")
                .isNull(SysUserEntity::getDeletedAt))
                .stream()
                .findFirst()
                .orElse(null);
        if (user == null) {
            return Optional.empty();
        }

        SysUserIdentityEntity identity = identityMapper.selectList(Wrappers.<SysUserIdentityEntity>lambdaQuery()
                .eq(SysUserIdentityEntity::getUserId, user.getId())
                .eq(SysUserIdentityEntity::getProvider, "PASSWORD")
                .isNull(SysUserIdentityEntity::getDeletedAt))
                .stream()
                .findFirst()
                .orElse(null);
        if (identity == null) {
            return Optional.empty();
        }
        return Optional.of(new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getEmailVerifiedAt(),
                user.getNickname(),
                identity.getPasswordHash(),
                user.getVersion() == null ? 1 : user.getVersion()));
    }

    /**
     * 查询活跃角色关联的有效权限。
     *
     * @param userId 用户 ID
     * @return 权限码列表
     */
    @Override
    public List<String> findActivePermissions(String userId) {
        List<SysUserRoleEntity> userRoles = userRoleMapper.selectList(Wrappers.<SysUserRoleEntity>lambdaQuery()
                .eq(SysUserRoleEntity::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> roleIds = userRoles.stream().map(SysUserRoleEntity::getRoleId).toList();
        List<SysRoleEntity> roles = roleMapper.selectList(Wrappers.<SysRoleEntity>lambdaQuery()
                .in(SysRoleEntity::getId, roleIds)
                .eq(SysRoleEntity::getStatus, "ACTIVE"));
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> activeRoleIds = roles.stream().map(SysRoleEntity::getId).toList();
        List<SysRolePermissionEntity> rolePermissions = rolePermissionMapper.selectList(
                Wrappers.<SysRolePermissionEntity>lambdaQuery().in(SysRolePermissionEntity::getRoleId, activeRoleIds));
        if (rolePermissions.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> permissionIds = rolePermissions.stream()
                .map(SysRolePermissionEntity::getPermissionId)
                .distinct()
                .toList();
        return permissionMapper.selectList(Wrappers.<SysPermissionEntity>lambdaQuery()
                        .in(SysPermissionEntity::getId, permissionIds)
                        .eq(SysPermissionEntity::getStatus, "ACTIVE"))
                .stream()
                .map(SysPermissionEntity::getCode)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 写入成功登录时间。
     *
     * @param userId 用户 ID
     * @param loginAt 登录时间
     */
    @Override
    public void updateLastLoginAt(String userId, LocalDateTime loginAt) {
        SysUserEntity update = new SysUserEntity();
        update.setLastLoginAt(loginAt);
        update.setUpdatedAt(loginAt);
        userMapper.update(update, Wrappers.<SysUserEntity>lambdaUpdate()
                .eq(SysUserEntity::getId, userId)
                .eq(SysUserEntity::getStatus, "ACTIVE")
                .isNull(SysUserEntity::getDeletedAt));
    }
}
