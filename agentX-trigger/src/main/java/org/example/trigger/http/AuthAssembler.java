package org.example.trigger.http;

import org.example.domain.auth.model.entity.LoginEntity;
import org.example.trigger.dto.auth.LoginRes;

/**
 * 认证DTO转换器
 */
public class AuthAssembler {

    /** Entity -> Res */
    public static LoginRes toRes(LoginEntity entity) {
        LoginRes res = new LoginRes();
        res.setUserId(entity.getUserId());
        res.setUsername(entity.getUsername());
        res.setToken(entity.getToken());
        res.setExpiresAt(entity.getExpiresAt());
        return res;
    }
}
