package xyz.piod.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.piod.keeper.config.CacheConfig;
import xyz.piod.keeper.dto.UserResponse;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.exception.ResourceNotFoundException;
import xyz.piod.keeper.mapper.UserMapper;
import xyz.piod.keeper.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Cacheable(value = CacheConfig.USER_CACHE, key = "#username")
    public User findUserByUsername(String username) {
        log.info("DATABASE HIT: Fetching user by username '{}' from database.", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Cacheable(value = CacheConfig.USER_CACHE, key = "'email:' + #email")
    public User findUserByEmail(String email) {
        log.info("DATABASE HIT: Fetching user by email '{}' from database.", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
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

    @Transactional
    @CacheEvict(value = CacheConfig.USER_CACHE, key = "#username")
    public void updateFcmToken(String username, String fcmToken) {
        User user = findUserByUsername(username);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
        log.info("Updated FCM token for user: {}", username);
    }
}