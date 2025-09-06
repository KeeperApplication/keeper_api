package xyz.piod.keeper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.piod.keeper.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPublicId(String publicId);

    List<User> findByUsernameContainingIgnoreCase(String username);
}