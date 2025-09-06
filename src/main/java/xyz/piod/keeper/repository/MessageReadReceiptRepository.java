package xyz.piod.keeper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.piod.keeper.entity.MessageReadReceipt;

import java.util.Set;

public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceipt, Long> {

    @Query("SELECT m.id FROM Message m JOIN m.readReceipts r WHERE m.chatRoom.id = :roomId AND r.user.id = :userId")
    Set<Long> findMessageIdsSeenByUserInRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);
}