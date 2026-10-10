package com.mittechkernel.backend.modules.user.service;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ResourceNotFoundException;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.user.dto.UserProfileResponse;
import com.mittechkernel.backend.modules.user.dto.UserProfileUpdateRequest;
import com.mittechkernel.backend.modules.user.entity.UserAccount;
import com.mittechkernel.backend.modules.user.entity.UserProfile;
import com.mittechkernel.backend.modules.user.repository.UserAccountRepository;
import com.mittechkernel.backend.modules.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserProfileService {

    private final CurrentUserService currentUserService;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProfileService(CurrentUserService currentUserService,
                             UserAccountRepository userAccountRepository,
                             UserProfileRepository userProfileRepository) {
        this.currentUserService = currentUserService;
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        UserAccount userAccount = userAccountRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.id()));

        UserProfile profile = userProfileRepository.findById(currentUser.id()).orElse(new UserProfile());
        return toResponse(userAccount, profile);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(UserProfileUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Profile payload is required");
        }

        CurrentUser currentUser = currentUserService.getCurrentUser();
        UserAccount userAccount = userAccountRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.id()));

        UserProfile profile = userProfileRepository.findById(currentUser.id())
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUserId(currentUser.id());
                    newProfile.setCreatedAt(Instant.now());
                    return newProfile;
                });

        if (request.bio() != null) {
            profile.setBio(normalizeText(request.bio()));
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(normalizeText(request.avatarUrl()));
        }
        if (request.githubUrl() != null) {
            profile.setGithubUrl(normalizeText(request.githubUrl()));
        }
        if (request.linkedinUrl() != null) {
            profile.setLinkedinUrl(normalizeText(request.linkedinUrl()));
        }
        if (request.phone() != null) {
            profile.setPhone(normalizeText(request.phone()));
        }

        profile.setUpdatedAt(Instant.now());
        UserProfile savedProfile = userProfileRepository.save(profile);
        return toResponse(userAccount, savedProfile);
    }

    private String normalizeText(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private UserProfileResponse toResponse(UserAccount userAccount, UserProfile profile) {
        return new UserProfileResponse(
                userAccount.getId(),
                userAccount.getName(),
                userAccount.getEmail(),
                userAccount.getProgram(),
                userAccount.getRoles(),
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getAvatarUrl() : null,
                profile != null ? profile.getGithubUrl() : null,
                profile != null ? profile.getLinkedinUrl() : null,
                profile != null ? profile.getPhone() : null
        );
    }
}
