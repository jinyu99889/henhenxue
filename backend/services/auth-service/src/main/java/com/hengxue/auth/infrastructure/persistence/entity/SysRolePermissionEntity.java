package com.hengxue.auth.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/** sys_role_permission 表的角色权限关联实体。 */
@TableName("sys_role_permission")
public class SysRolePermissionEntity {

    private String roleId;
    private String permissionId;

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public String getPermissionId() { return permissionId; }
    public void setPermissionId(String permissionId) { this.permissionId = permissionId; }
}
