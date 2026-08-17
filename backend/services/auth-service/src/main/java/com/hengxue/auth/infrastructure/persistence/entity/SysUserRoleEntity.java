package com.hengxue.auth.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** sys_user_role 表的用户角色关联实体。 */
@TableName("sys_user_role")
public class SysUserRoleEntity {

    private String userId;
    private String roleId;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
}
