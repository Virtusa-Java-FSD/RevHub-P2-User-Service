package com.revhub.service;

import com.revhub.dto.AuthResponse;
import com.revhub.dto.LoginRequest;
import com.revhub.dto.SignupRequest;
import com.revhub.model.User;
import com.revhub.model.UserRole;
import com.revhub.repository.UserRepository;
import com.revhub.security.JwtTokenProvider;
import com.revhub.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordResetService passwordResetService;

    public AuthResponse signup(SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setRole(UserRole.USER); // Default role
        User savedUser = userRepository.save(user);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        signupRequest.getEmail(),
                        signupRequest.getPassword()));
        String token = tokenProvider.generateToken(authentication);
        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getProfilePicture(),
                savedUser.getRole().name()); // Added role
    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), // This can be email or username now
                        loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get details from UserPrincipal
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Check if user is banned
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getIsBanned()) {
            throw new RuntimeException(
                    "Your account has been banned by admin. Please contact support or create a new profile.");
        }

        if (user.getIsDeleted()) {
            throw new RuntimeException(
                    "Your account has been deleted. Please contact support or create a new profile.");
        }

        String token = tokenProvider.generateToken(authentication);

        return new AuthResponse(
                token,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userPrincipal.getFirstName(),
                userPrincipal.getLastName(),
                userPrincipal.getProfilePicture(),
                userPrincipal.getRole().name()); // Added role
    }

    public void sendPasswordResetOtp(String email) {
        passwordResetService.sendOtp(email);
    }

    public void verifyPasswordResetOtp(String email, String otp) {
        passwordResetService.verifyOtp(email, otp);
    }

    public void resetPassword(String email, String newPassword) {
        passwordResetService.resetPassword(email, newPassword);
    }
}
