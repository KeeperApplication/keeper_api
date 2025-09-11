package xyz.piod.keeper.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.piod.keeper.config.CacheConfig;
import xyz.piod.keeper.dto.AccountUpdateRequest;
import xyz.piod.keeper.entity.AuthProvider;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.exception.ResourceNotFoundException;
import xyz.piod.keeper.repository.UserRepository;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;

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
    public User loginOrRegisterLocalUser(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();
            log.info("Processing local login for existing user: {}", email);
            return existingUser;
        } else {
            log.info("Creating new user from local flow for email: {}", email);
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setAuthProvider(AuthProvider.LOCAL);
            newUser.setPublicId(generateUniquePublicId());

            String usernameBase = email.substring(0, email.indexOf('@'));
            String finalUsername = generateUniqueUsername(usernameBase);
            newUser.setUsername(finalUsername);

            return userRepository.save(newUser);
        }
    }

    @Transactional
    @CacheEvict(value = CacheConfig.USER_CACHE, key = "#currentUsername")
    public User updateUserAccount(String currentUsername, AccountUpdateRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + currentUsername));

        boolean isUsernameChange = request.getUsername() != null && !request.getUsername().isEmpty() && !request.getUsername().equals(user.getUsername());

        if (isUsernameChange) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new ValidationException("Username '" + request.getUsername() + "' is already taken.");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getProfilePicture() != null) {
            user.setProfilePicture(request.getProfilePicture());
        }

        return userRepository.save(user);
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