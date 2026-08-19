package org.example.trigger.http;

import org.example.api.response.Response;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.trigger.dto.apikey.ApiKeyReq;
import org.example.trigger.dto.apikey.ApiKeyRes;
import org.example.trigger.dto.apikey.ApiKeyUpdateGroup;
import org.example.types.enums.ResponseCode;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API密钥管理
 */
@RestController
@RequestMapping("/apikey")
public class ApiKeyController {

    @Resource
    private IApiKeyDomainService apiKeyDomainService;

    /**
     * 创建API密钥
     */
    @PostMapping("/create")
    public Response<ApiKeyRes> createApiKey(@RequestBody @Validated ApiKeyReq req) {
        ApiKeyEntity apiKey = ApiKeyAssembler.toEntity(req);
        ApiKeyEntity created = apiKeyDomainService.createApiKey(apiKey);
        return Response.success(ApiKeyAssembler.toRes(created));
    }

    /**
     * 更新API密钥
     */
    @PostMapping("/update")
    public Response<ApiKeyRes> updateApiKey(@RequestBody @Validated(ApiKeyUpdateGroup.class) ApiKeyReq req) {
        ApiKeyEntity apiKey = ApiKeyAssembler.toUpdateEntity(req);
        ApiKeyEntity updated = apiKeyDomainService.updateApiKey(apiKey);
        return Response.success(ApiKeyAssembler.toRes(updated));
    }

    /**
     * 删除API密钥
     */
    @PostMapping("/delete")
    public Response<Void> deleteApiKey(@RequestParam String id) {
        apiKeyDomainService.deleteApiKey(id);
        return Response.success();
    }

    /**
     * 根据ID查询API密钥
     */
    @GetMapping("/get")
    public Response<ApiKeyRes> getApiKey(@RequestParam String id) {
        ApiKeyEntity apiKey = apiKeyDomainService.getApiKey(id);
        if (apiKey == null) {
            return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), "API密钥不存在");
        }
        return Response.success(ApiKeyAssembler.toRes(apiKey));
    }

    /**
     * 根据用户ID查询所有密钥
     */
    @GetMapping("/list")
    public Response<List<ApiKeyRes>> listApiKeys(@RequestParam String userId) {
        List<ApiKeyEntity> apiKeys = apiKeyDomainService.listApiKeys(userId);
        List<ApiKeyRes> resList = apiKeys.stream()
                .map(ApiKeyAssembler::toRes)
                .collect(Collectors.toList());
        return Response.success(resList);
    }
}
