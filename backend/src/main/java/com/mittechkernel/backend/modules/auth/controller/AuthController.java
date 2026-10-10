package com.mittechkernel.backend.modules.auth.controller;

import com.mittechkernel.backend.common.exception.ApiException;
import com.mittechkernel.backend.common.exception.UnauthorizedException;
import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.auth.dto.AuthSuccessResponse;
import com.mittechkernel.backend.modules.auth.dto.AuthSessionResponse;
import com.mittechkernel.backend.modules.auth.dto.LoginRequest;
import com.mittechkernel.backend.modules.auth.dto.PasswordSetupRequest;
import com.mittechkernel.backend.modules.auth.dto.UserPublicResponse;
import com.mittechkernel.backend.modules.auth.entity.AuthUser;
import com.mittechkernel.backend.modules.auth.repository.AuthUserRepository;
import com.mittechkernel.backend.modules.auth.service.AuthService;
import com.mittechkernel.backend.modules.admin.service.AccountAdministrationService;
import com.mittechkernel.backend.modules.auth.service.JwtService;
import com.mittechkernel.backend.security.BrowserOriginGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthUserRepository authUserRepository;
    private final CurrentUserService currentUserService;
    private final AccountAdministrationService accountAdministrationService;
    private final JwtService jwtService;
    private final BrowserOriginGuard originGuard;
    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final String cookiePath;

    public AuthController(AuthService authService,
                         AuthUserRepository authUserRepository,
                         CurrentUserService currentUserService,
                         AccountAdministrationService accountAdministrationService,
                         JwtService jwtService,
                         BrowserOriginGuard originGuard,
                         @Value("${app.security.refresh-cookie.name:kernel_refresh}") String cookieName,
                         @Value("${app.security.refresh-cookie.secure:false}") boolean cookieSecure,
                         @Value("${app.security.refresh-cookie.same-site:Lax}") String cookieSameSite,
                         @Value("${app.security.refresh-cookie.path:/api/v1/auth}") String cookiePath) {
        this.authService = authService;
        this.authUserRepository = authUserRepository;
        this.currentUserService = currentUserService;
        this.accountAdministrationService = accountAdministrationService;
        this.jwtService = jwtService;
        this.originGuard = originGuard;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.cookiePath = cookiePath;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> login(@Valid @RequestBody LoginRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        originGuard.requireAllowedOrigin(httpServletRequest);
        AuthSuccessResponse result = authService.login(request.email(), request.password());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
                .body(ApiResponse.success(new AuthSessionResponse(result.accessToken(), result.user()), httpServletRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "${app.security.refresh-cookie.name:kernel_refresh}", required = false) String refreshToken,
                                                                   HttpServletRequest httpServletRequest) {
        originGuard.requireAllowedOrigin(httpServletRequest);
        try {
            AuthSuccessResponse result = authService.refresh(refreshToken);
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
                    .body(ApiResponse.success(new AuthSessionResponse(result.accessToken(), result.user()), httpServletRequest));
        } catch (ApiException failure) {
            return ResponseEntity.status(failure.getStatus()).header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                    .body(ApiResponse.error(failure.getStatus(), failure.getCode(), failure.getMessage(), httpServletRequest.getRequestURI()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@CookieValue(name = "${app.security.refresh-cookie.name:kernel_refresh}", required = false) String refreshToken,
                                                  HttpServletRequest httpServletRequest) {
        originGuard.requireAllowedOrigin(httpServletRequest);
        if (refreshToken != null && !refreshToken.isBlank()) authService.logout(refreshToken);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, expiredCookie().toString()).body(ApiResponse.success(null, httpServletRequest));
    }

    @PostMapping("/password/setup")
    public ResponseEntity<ApiResponse<Void>> completePasswordSetup(@Valid @RequestBody PasswordSetupRequest request, HttpServletRequest httpServletRequest) {
        originGuard.requireAllowedOrigin(httpServletRequest);
        accountAdministrationService.completePasswordSetup(request.token(), request.password());
        return ResponseEntity.ok(ApiResponse.success(null, httpServletRequest));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserPublicResponse>> currentUser(HttpServletRequest httpServletRequest) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        AuthUser authUser = authUserRepository.findById(currentUser.id())
                .orElseThrow(() -> new UnauthorizedException("User session is invalid"));
        UserPublicResponse response = new UserPublicResponse(authUser.getId(), authUser.getName(), authUser.getEmail(), authUser.getProgram(), authUser.getRoles());
        return ResponseEntity.ok(ApiResponse.success(response, httpServletRequest));
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(cookieName, token).httpOnly(true).secure(cookieSecure).sameSite(cookieSameSite)
                .path(cookiePath).maxAge(jwtService.getRefreshTokenExpirationMs() / 1000).build();
    }

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from(cookieName, "").httpOnly(true).secure(cookieSecure).sameSite(cookieSameSite)
                .path(cookiePath).maxAge(0).build();
    }
}
