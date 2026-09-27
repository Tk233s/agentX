package org.example.trigger.http;

import org.example.api.response.Response;
import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.IAgentDomainService;
import org.example.trigger.dto.agent.AgentReq;
import org.example.trigger.dto.agent.AgentRes;
import org.example.trigger.dto.agent.UpdateGroup;
import org.example.types.context.UserContext;
import org.example.types.enums.ResponseCode;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

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
        AgentEntity agent = AgentAssembler.toEntity(agentReq, UserContext.requireCurrentUserId());
        // 2. 调用领域服务执行创建（业务校验 + 落库）
        AgentEntity created = agentDomainService.createAgent(agent);
        // 3. 领域实体 -> 出参 DTO，包统一响应返回
        return Response.success(AgentAssembler.toRes(created));
    }

    /**
     * 更新智能体
     * @param agentReq 更新请求入参（需携带id）
     * @return 更新成功的智能体信息
     */
    @PostMapping("/update")
    public Response<AgentRes> updateAgent(@RequestBody @Validated(UpdateGroup.class) AgentReq agentReq) {
        // 1. 入参 DTO -> 领域实体（仅携带ID与可编辑业务字段）
        AgentEntity agent = AgentAssembler.toUpdateEntity(agentReq);
        // 2. 调用领域服务执行更新（校验 + 合并保留归属/状态/时间戳 + 落库）
        AgentEntity updated = agentDomainService.updateAgent(agent, UserContext.requireCurrentUserId());
        // 3. 领域实体 -> 出参 DTO，包统一响应返回
        return Response.success(AgentAssembler.toRes(updated));
    }

    /**
     * 删除智能体
     * @param id 智能体ID
     * @return 统一响应
     */
    @PostMapping("/delete")
    public Response<Void> deleteAgent(@RequestParam String id) {
        agentDomainService.deleteAgent(id, UserContext.requireCurrentUserId());
        return Response.success();
    }

    /**
     * 根据ID查询智能体
     * @param id 智能体ID
     * @return 智能体信息，不存在返回null
     */
    @GetMapping("/get")
    public Response<AgentRes> getAgent(@RequestParam String id) {
        AgentEntity agent = agentDomainService.getAgent(id, UserContext.requireCurrentUserId());
        if (agent == null) {
            return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "智能体不存在");
        }
        return Response.success(AgentAssembler.toRes(agent));
    }

    /**
     * 根据创建者用户ID查询智能体列表
     * @param userId 创建者用户ID
     * @return 智能体信息列表
     */
    @GetMapping("/list")
    public Response<List<AgentRes>> listAgents() {
        List<AgentEntity> agents = agentDomainService.listAgents(UserContext.requireCurrentUserId());
        List<AgentRes> resList = agents.stream()
                .map(AgentAssembler::toRes)
                .collect(Collectors.toList());
        return Response.success(resList);
    }
}
