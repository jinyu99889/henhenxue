package com.hengxue.auth.application.command;

/** 注册用例接收的原始命令。 */
public record RegisterCommand(
        String username,
        String email,
        String emailCode,
        String password,
        String nickname
) {
}
