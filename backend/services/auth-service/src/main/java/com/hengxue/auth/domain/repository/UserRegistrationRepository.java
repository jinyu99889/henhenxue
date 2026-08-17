package com.hengxue.auth.domain.repository;

import com.hengxue.auth.domain.entity.NewUser;
import java.time.LocalDateTime;

/** 用户注册所需的用户、身份与角色持久化端口。 */
public interface UserRegistrationRepository {

    /**
     * 判断活跃用户名是否存在。
     *
     * @param username 规范化后的用户名
     * @return 用户名已存在时为 {@code true}
     */
    boolean existsActiveUsername(String username);

    /**
     * 判断活跃邮箱是否存在。
     *
     * @param email 规范化后的邮箱
     * @return 邮箱已存在时为 {@code true}
     */
    boolean existsActiveEmail(String email);

    /**
     * 查询当前可用的普通用户角色。
     *
     * @return 普通用户角色 ID；未初始化或不可用时为 {@code null}
     */
    String findActiveUserRoleId();

    /**
     * 在当前事务中创建用户、密码身份和普通用户角色关联。
     *
     * @param user 待创建的用户信息
     * @param passwordHash BCrypt 密码哈希
     * @param roleId 普通用户角色 ID
     * @param now 当前 UTC 时间
     */
    void create(NewUser user, String passwordHash, String roleId, LocalDateTime now);
}
