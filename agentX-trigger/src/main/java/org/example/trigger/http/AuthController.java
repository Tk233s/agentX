package org.example.trigger.http;

import org.example.api.response.Response;
import org.example.domain.auth.model.entity.LoginEntity;
import org.example.domain.auth.service.IAuthDomainService;
import org.example.trigger.dto.auth.LoginReq;
import org.example.trigger.dto.auth.LoginRes;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * 认证管理
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private IAuthDomainService authDomainService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Response<LoginRes> login(@RequestBody @Validated LoginReq req) {
        LoginEntity login = authDomainService.login(req.getUsername(), req.getPassword());
        return Response.success(AuthAssembler.toRes(login));
    }
}
