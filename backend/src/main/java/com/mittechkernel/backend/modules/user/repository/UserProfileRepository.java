package com.mittechkernel.backend.modules.user.repository;

import com.mittechkernel.backend.modules.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
