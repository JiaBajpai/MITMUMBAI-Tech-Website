package com.mittechkernel.backend.modules.auth.service;

import com.mittechkernel.backend.modules.auth.entity.AuthUser;
import com.mittechkernel.backend.modules.auth.repository.AuthUserRepository;
import com.mittechkernel.backend.modules.auth.security.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    public AuthUserDetailsService(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUser authUser = authUserRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserPrincipal(authUser.getId(), authUser.getEmail(), authUser.getPasswordHash(),
                authUser.getRoles(), authUser.isActive());
    }
}
