package com.nhcwash.backend.configs;


import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        boolean softDeleted = user.getDeletedAt() != null || Boolean.TRUE.equals(user.getIsDeleted());
        boolean enabled = Boolean.TRUE.equals(user.getIsActive()) && !softDeleted;
        boolean accountNonLocked = !softDeleted;

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                enabled,
                true,
                true,
                accountNonLocked,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getName())));
    }
}
