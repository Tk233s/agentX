package org.example.trigger.http;

import org.example.api.response.Response;
import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.IAgentDomainService;
import org.example.trigger.dto.agent.AgentReq;
import org.example.trigger.dto.agent.AgentRes;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户端的主要业务
 */
@RestController
@RequestMapping("/agent")
public class AiAgentController {

    @Resource
    private IAgentDomainService agentDomainService;

    /**
     * 创建智能体
     * @param agentReq 创建请求入参
     * @return 创建成功的智能体信息
     */
    @PostMapping("/create")
    public Response<AgentRes> createAgent(@RequestBody @Validated AgentReq agentReq) {
        // 1. 入参 DTO -> 领域实体（初始状态由领域工厂统一生成）
        AgentEntity agent = AgentAssembler.toEntity(agentReq);
        // 2. 调用领域服务执行创建（业务校验 + 落库）
        AgentEntity created = agentDomainService.createAgent(agent);
        // 3. 领域实体 -> 出参 DTO，包统一响应返回
        return Response.success(AgentAssembler.toRes(created));
    }
}
