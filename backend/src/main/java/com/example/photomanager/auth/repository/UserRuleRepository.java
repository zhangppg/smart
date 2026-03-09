package com.example.photomanager.auth.repository;

import com.example.photomanager.auth.entity.UserRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRuleRepository extends JpaRepository<UserRuleEntity, Long> {
    List<UserRuleEntity> findByUserId(Long userId);
}
