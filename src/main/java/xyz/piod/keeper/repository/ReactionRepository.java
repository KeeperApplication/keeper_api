package xyz.piod.keeper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.piod.keeper.entity.Message;
import xyz.piod.keeper.entity.Reaction;
import xyz.piod.keeper.entity.User;

import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByUserAndMessageAndEmoji(User user, Message message, String emoji);
}