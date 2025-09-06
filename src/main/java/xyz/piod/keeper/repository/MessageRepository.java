package xyz.piod.keeper.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.piod.keeper.entity.Message;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"sender", "repliedTo", "reactions.user", "readReceipts.user"})
    Page<Message> findByChatRoomIdOrderByTimestampDesc(Long chatRoomId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "repliedTo", "reactions.user", "readReceipts.user"})
    List<Message> findByChatRoomIdAndIsPinnedTrueOrderByTimestampDesc(Long chatRoomId);

    List<Message> findByIdLessThanEqualAndChatRoomId(Long id, Long chatRoomId);

    @Query("SELECT m FROM Message m WHERE m.id = :messageId")
    @EntityGraph(attributePaths = {"sender", "repliedTo", "reactions.user", "readReceipts.user"})
    Optional<Message> findByIdWithReactions(@Param("messageId") Long messageId);

    @Transactional
    void deleteByChatRoomId(Long chatRoomId);
}