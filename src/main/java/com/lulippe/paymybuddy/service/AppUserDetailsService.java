package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        final AppUser appUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("username not found" + email));
        return new User(appUser.getEmail(), appUser.getPassword(), getGrantedAuthorities(appUser.getRole().getRoleName()));
    }

    private List<GrantedAuthority> getGrantedAuthorities(final String role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role));
        log.debug("role : {}", role);
        return authorities;
    }
}
