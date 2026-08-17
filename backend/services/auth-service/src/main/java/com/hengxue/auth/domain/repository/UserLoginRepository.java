package com.hengxue.auth.domain.repository;

import com.hengxue.auth.domain.entity.LoginUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 密码登录所需的用户、身份和权限持久化端口。 */
public interface UserLoginRepository {

    /**
     * 按用户名或邮箱查询活跃用户及密码身份。
     *
     * @param account 已规范化的用户名或邮箱
     * @return 找到的登录快照
     */
    Optional<LoginUser> findActiveByAccount(String account);

    /**
     * 查询用户当前有效角色授予的权限码。
     *
     * @param userId 用户 ID
     * @return 去重后的权限码
     */
    List<String> findActivePermissions(String userId);

    /**
     * 更新成功登录时间。
     *
     * @param userId 用户 ID
     * @param loginAt 成功登录时间
     */
    void updateLastLoginAt(String userId, LocalDateTime loginAt);
}
