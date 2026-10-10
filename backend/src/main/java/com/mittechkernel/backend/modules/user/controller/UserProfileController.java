package com.mittechkernel.backend.modules.user.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.modules.user.dto.UserProfileResponse;
import com.mittechkernel.backend.modules.user.dto.UserProfileUpdateRequest;
import com.mittechkernel.backend.modules.user.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getMyProfile(), request));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(@Valid @RequestBody UserProfileUpdateRequest request,
                                                                            HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.updateMyProfile(request), httpServletRequest));
    }
}
