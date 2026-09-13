package org.example.infrastructure.adapter.repository;

import org.example.domain.auth.adapter.repository.UserRepository;
import org.example.domain.auth.model.entity.UserEntity;
import org.example.infrastructure.dao.IUserDao;
import org.example.infrastructure.dao.po.UserPO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;

/**
 * 用户仓储实现
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    @Resource
    private IUserDao userDao;

    @Override
    public UserEntity findByUsername(String username) {
        return toEntity(userDao.queryByUsername(username));
    }

    /** PO -> Entity */
    private UserEntity toEntity(UserPO po) {
        if (po == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        entity.setId(po.getId());
        entity.setUsername(po.getUsername());
        entity.setPasswordHash(po.getPasswordHash());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        return entity;
    }
}
