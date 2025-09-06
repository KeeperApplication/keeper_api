package xyz.piod.keeper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import xyz.piod.keeper.entity.ChatRoom;
import xyz.piod.keeper.entity.HiddenChatRoom;
import xyz.piod.keeper.entity.User;

import java.util.Optional;

public interface HiddenChatRoomRepository extends JpaRepository<HiddenChatRoom, Long> {

    Optional<HiddenChatRoom> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    @Transactional
    void deleteByUserAndChatRoom(User user, ChatRoom chatRoom);
}