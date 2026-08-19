package com.notification.platform.domain.repository;

import com.notification.platform.domain.model.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {
    Optional<UserProfileEntity> findByEmail(String email);
    Optional<UserProfileEntity> findByPhoneNumber(String phoneNumber);
}
