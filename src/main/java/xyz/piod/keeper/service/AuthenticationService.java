package xyz.piod.keeper.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import xyz.piod.keeper.config.CacheConfig;
import xyz.piod.keeper.dto.AccountUpdateRequest;
import xyz.piod.keeper.dto.RecaptchaResponse;
import xyz.piod.keeper.entity.AuthProvider;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.exception.ResourceNotFoundException;
import xyz.piod.keeper.exception.UnauthorizedOperationException;
import xyz.piod.keeper.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${recaptcha.secret-key}")
    private String recaptchaSecretKey;

    @Transactional
    public User processOAuthPostLogin(String email, String name, String imageUrl) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();
            log.info("Processing login for existing user: {}", email);
            return existingUser;
        } else {
            User newUser = new User();
            newUser.setEmail(email);

            String baseUsername = name;
            if (baseUsername.length() > 15) {
                baseUsername = baseUsername.substring(0, 15);
            }
            String finalUsername = generateUniqueUsername(baseUsername);

            newUser.setUsername(finalUsername);
            newUser.setProfilePicture(imageUrl);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            newUser.setPublicId(generateUniquePublicId());

            log.info("Creating new user '{}' for email: {}", finalUsername, email);
            return userRepository.save(newUser);
        }
    }

    @Transactional
    public void setPassword(String username, String newPassword, String recaptchaToken) {
        if (!isRecaptchaValid(recaptchaToken)) {
            throw new IllegalArgumentException("reCAPTCHA validation failed.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPassword() != null) {
            throw new IllegalStateException("User already has a password set.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.USER_CACHE, key = "#currentUsername")
    public User updateUserAccount(String currentUsername, AccountUpdateRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + currentUsername));

        boolean isUsernameChange = request.getUsername() != null && !request.getUsername().isEmpty() && !request.getUsername().equals(user.getUsername());
        boolean isPasswordChange = request.getNewPassword() != null && !request.getNewPassword().isEmpty();

        if (isUsernameChange || isPasswordChange) {
            validateCurrentPassword(user, request.getCurrentPassword());
        }

        if (isUsernameChange) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new ValidationException("Username '" + request.getUsername() + "' is already taken.");
            }
            user.setUsername(request.getUsername());
        }

        if (isPasswordChange) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setPasswordChangedAt(LocalDateTime.now());
        }

        if (request.getProfilePicture() != null) {
            user.setProfilePicture(request.getProfilePicture());
        }

        return userRepository.save(user);
    }

    private void validateCurrentPassword(User user, String currentPasswordRequest) {
        if (user.getPassword() == null) {
            throw new UnauthorizedOperationException("Cannot change sensitive data for an account created with Google without a password set first.");
        }
        if (currentPasswordRequest == null || !passwordEncoder.matches(currentPasswordRequest, user.getPassword())) {
            throw new UnauthorizedOperationException("Current password is incorrect.");
        }
    }

    private boolean isRecaptchaValid(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("reCAPTCHA token is null or empty.");
            return false;
        }
        String url = "https://www.google.com/recaptcha/api/siteverify?secret=" + recaptchaSecretKey + "&response=" + token;
        try {
            RecaptchaResponse response = restTemplate.getForObject(url, RecaptchaResponse.class);
            if (response != null && response.isSuccess()) {
                log.info("reCAPTCHA validation successful for host: {}", response.getHostname());
                return true;
            } else {
                log.warn("reCAPTCHA validation failed. Response: {}", response);
                return false;
            }
        } catch (Exception e) {
            log.error("Error while validating reCAPTCHA token", e);
            return false;
        }
    }

    private String generateUniqueUsername(String baseUsername) {
        String currentUsername = baseUsername;
        int attempt = 1;
        while (userRepository.findByUsername(currentUsername).isPresent()) {
            String suffix = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
            int availableLength = 15 - suffix.length();
            if (baseUsername.length() > availableLength) {
                currentUsername = baseUsername.substring(0, availableLength) + suffix;
            } else {
                currentUsername = baseUsername + suffix;
            }
            attempt++;
            if(attempt > 5) {
                return generateUniquePublicId().substring(0, 15);
            }
        }
        return currentUsername;
    }

    private String generateUniquePublicId() {
        String publicId;
        do {
            long timestamp = System.currentTimeMillis();
            int randomNum = ThreadLocalRandom.current().nextInt(1000, 10000);
            publicId = String.valueOf(timestamp) + randomNum;
        } while (userRepository.findByPublicId(publicId).isPresent());
        return publicId;
    }
}