package xyz.piod.keeper.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.piod.keeper.entity.ChatRoom;
import xyz.piod.keeper.entity.User;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByInviteCode(String inviteCode);

    @Query(value = "SELECT cr.id FROM ChatRoom cr " +
            "WHERE :user MEMBER OF cr.participants AND cr.isPrivate = false AND cr.id NOT IN " +
            "(SELECT hcr.chatRoom.id FROM HiddenChatRoom hcr WHERE hcr.user = :user)",
            countQuery = "SELECT count(cr) FROM ChatRoom cr " +
                    "WHERE :user MEMBER OF cr.participants AND cr.isPrivate = false AND cr.id NOT IN " +
                    "(SELECT hcr.chatRoom.id FROM HiddenChatRoom hcr WHERE hcr.user = :user)")
    Page<Long> findVisiblePublicRoomIdsForUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.participants LEFT JOIN FETCH cr.owner WHERE cr.id IN :ids")
    @EntityGraph(attributePaths = {"participants", "owner"})
    List<ChatRoom> findByIdsWithParticipants(@Param("ids") List<Long> ids);

    @Query(value = "SELECT cr.id FROM ChatRoom cr " +
            "WHERE :user MEMBER OF cr.participants AND cr.isPrivate = true AND cr.id NOT IN " +
            "(SELECT hcr.chatRoom.id FROM HiddenChatRoom hcr WHERE hcr.user = :user)",
            countQuery = "SELECT count(cr) FROM ChatRoom cr " +
                    "WHERE :user MEMBER OF cr.participants AND cr.isPrivate = true AND cr.id NOT IN " +
                    "(SELECT hcr.chatRoom.id FROM HiddenChatRoom hcr WHERE hcr.user = :user)")
    Page<Long> findVisiblePrivateRoomIdsForUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN TRUE ELSE FALSE END FROM ChatRoom cr JOIN cr.participants p WHERE cr.id = :roomId AND p.id = :userId")
    boolean isUserParticipant(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"participants", "owner"})
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p1 JOIN cr.participants p2 WHERE cr.isPrivate = true AND p1 = :user1 AND p2 = :user2")
    Optional<ChatRoom> findPrivateRoomBetween(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT cr.id FROM ChatRoom cr JOIN cr.participants p WHERE p.id = :userId")
    List<Long> findAllRoomIdsByParticipantId(@Param("userId") Long userId);
}