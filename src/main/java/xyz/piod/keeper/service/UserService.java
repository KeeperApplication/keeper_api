package xyz.piod.keeper.service;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xyz.piod.keeper.config.CacheConfig;
import xyz.piod.keeper.dto.AccountUpdateRequest;
import xyz.piod.keeper.dto.RecaptchaResponse;
import xyz.piod.keeper.dto.UserResponse;
import xyz.piod.keeper.entity.AuthProvider;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.exception.ResourceNotFoundException;
import xyz.piod.keeper.exception.UnauthorizedOperationException;
import xyz.piod.keeper.mapper.UserMapper;
import xyz.piod.keeper.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String recaptchaSecretKey;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper,
                       @Value("${recaptcha.secret-key}") String recaptchaSecretKey) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.recaptchaSecretKey = recaptchaSecretKey;
    }

    public User processOAuthPostLogin(String email, String name, String imageUrl) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();
            log.info("Processing login for existing user: {}", email);
            return existingUser;
        } else {
            User newUser = new User();
            newUser.setEmail(email);

            String username = name;
            if (username.length() > 15) {
                username = username.substring(0, 15);
            }
            if (userRepository.findByUsername(username).isPresent()) {
                username = username.substring(0, 11) + ThreadLocalRandom.current().nextInt(1000, 9999);
            }

            newUser.setUsername(username);
            newUser.setProfilePicture(imageUrl);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            newUser.setPublicId(generateUniquePublicId());

            log.info("Creating new user '{}' for email: {}", username, email);
            return userRepository.save(newUser);
        }
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

    @CacheEvict(value = CacheConfig.USER_CACHE, key = "#username")
    public void saveFcmToken(String username, String fcmToken) {
        User user = findUserByUsername(username);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
        log.info("saved fcm token for user {}", username);
    }

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
        userRepository.save(user);
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

    @CacheEvict(value = CacheConfig.USER_CACHE, key = "#currentUsername")
    public User updateUserAccount(String currentUsername, AccountUpdateRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + currentUsername));

        boolean isUsernameChange = request.getUsername() != null && !request.getUsername().isEmpty() && !request.getUsername().equals(user.getUsername());
        boolean isPasswordChange = request.getNewPassword() != null && !request.getNewPassword().isEmpty();

        if (isUsernameChange || isPasswordChange) {
            if (user.getPassword() == null) {
                throw new UnauthorizedOperationException("Cannot change sensitive data for an account created with Google without a password set.");
            }
            if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new UnauthorizedOperationException("Current password is incorrect.");
            }
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

    @Cacheable(value = CacheConfig.USER_CACHE, key = "#username")
    public User findUserByUsername(String username) {
        log.info("DATABASE HIT: Fetching user by username '{}' from database.", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Cacheable(value = CacheConfig.USER_CACHE, key = "'publicId:' + #publicId")
    public User findUserByPublicId(String publicId) {
        log.info("DATABASE HIT: Fetching user by publicId '{}' from database.", publicId);
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + publicId));
    }

    @Cacheable(value = CacheConfig.USER_CACHE, key = "#id")
    public User findUserById(Long id) {
        log.info("DATABASE HIT: Fetching user by id '{}' from database.", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    public List<UserResponse> searchUsers(String query) {
        return userRepository.findByPublicId(query)
                .map(user -> Collections.singletonList(userMapper.toUserResponse(user)))
                .orElseGet(() ->
                        userRepository.findByUsernameContainingIgnoreCase(query)
                                .stream()
                                .map(userMapper::toUserResponse)
                                .collect(Collectors.toList())
                );
    }
}