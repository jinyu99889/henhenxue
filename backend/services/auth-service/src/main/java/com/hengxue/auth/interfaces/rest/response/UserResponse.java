package com.hengxue.auth.interfaces.rest.response;

import java.time.LocalDateTime;
import java.util.List;

/** 对外返回的用户摘要。 */
public record UserResponse(
        String id,
        String username,
        String email,
        LocalDateTime emailVerifiedAt,
        String nickname,
        String avatarFileId,
        List<String> permissions,
        int version
) {
}
