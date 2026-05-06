package com.chronos.service;

import com.chronos.dto.AuthDto;
import com.chronos.entity.User;
import com.chronos.enums.UserRole;
import com.chronos.repository.UserRepository;
import com.chronos.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService userDetailsService;

    @InjectMocks AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .password("encoded-password")
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .build();
    }

    @Test
    void register_success() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("secret123");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        org.springframework.security.core.userdetails.User springUser =
                new org.springframework.security.core.userdetails.User(
                        "alice", "encoded-password", java.util.List.of());
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(springUser);
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        AuthDto.AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("alice");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throws() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("secret123");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void login_success() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setUsername("alice");
        req.setPassword("secret123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));

        org.springframework.security.core.userdetails.User springUser =
                new org.springframework.security.core.userdetails.User(
                        "alice", "encoded-password", java.util.List.of());
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(springUser);
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        AuthDto.AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("alice");
    }

    @Test
    void login_badCredentials_throws() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
