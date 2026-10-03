package org.example.trigger.http;

import org.example.api.response.Response;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.service.ISessionDomainService;
import org.example.trigger.dto.session.SessionReq;
import org.example.trigger.dto.session.SessionRes;
import org.example.types.context.UserContext;
import org.example.types.enums.ResponseCode;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话管理
 */
@RestController
@RequestMapping("/session")
public class SessionController {

    @Resource
    private ISessionDomainService sessionDomainService;

    /**
     * 创建会话
     */
    @PostMapping("/create")
    public Response<SessionRes> createSession(@RequestBody @Validated SessionReq req) {
        SessionEntity session = SessionAssembler.toEntity(req, UserContext.requireCurrentUserId());
        SessionEntity created = sessionDomainService.createSession(session);
        return Response.success(SessionAssembler.toRes(created));
    }

    /**
     * 重命名会话
     */
    @PostMapping("/rename")
    public Response<SessionRes> renameSession(
            @RequestParam("id") String id,
            @RequestParam("title") String title) {
        SessionEntity updated = sessionDomainService.updateSessionTitle(id, UserContext.requireCurrentUserId(), title);
        return Response.success(SessionAssembler.toRes(updated));
    }

    /**
     * 删除会话（级联删除消息）
     */
    @PostMapping("/delete")
    public Response<Void> deleteSession(@RequestParam("id") String id) {
        sessionDomainService.deleteSession(id, UserContext.requireCurrentUserId());
        return Response.success();
    }

    /**
     * 根据ID查询会话
     */
    @GetMapping("/get")
    public Response<SessionRes> getSession(@RequestParam("id") String id) {
        SessionEntity session = sessionDomainService.getSession(id, UserContext.requireCurrentUserId());
        if (session == null) {
            return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话不存在");
        }
        return Response.success(SessionAssembler.toRes(session));
    }

    /**
     * 根据用户ID查询所有会话
     */
    @GetMapping("/list")
    public Response<List<SessionRes>> listSessions() {
        List<SessionEntity> sessions = sessionDomainService.listSessions(UserContext.requireCurrentUserId());
        List<SessionRes> resList = sessions.stream()
                .map(SessionAssembler::toRes)
                .collect(Collectors.toList());
        return Response.success(resList);
    }
}
