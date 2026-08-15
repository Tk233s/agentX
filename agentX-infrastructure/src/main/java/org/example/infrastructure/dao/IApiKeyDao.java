package org.example.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.dao.po.ApiKeyPO;

import java.util.List;

/**
 * API密钥 DAO 接口
 */
@Mapper
public interface IApiKeyDao {

    /**
     * 新增API密钥
     */
    void insert(ApiKeyPO apiKeyPO);

    /**
     * 更新API密钥
     */
    int update(ApiKeyPO apiKeyPO);

    /**
     * 根据ID删除
     */
    int deleteById(String id);

    /**
     * 根据ID查询
     */
    ApiKeyPO queryById(String id);

    /**
     * 根据用户ID查询所有密钥
     */
    List<ApiKeyPO> queryByUserId(String userId);

    /**
     * 根据用户ID和服务商查询启用的密钥
     */
    ApiKeyPO queryByUserIdAndProvider(@org.apache.ibatis.annotations.Param("userId") String userId,
                                      @org.apache.ibatis.annotations.Param("provider") String provider);
}
