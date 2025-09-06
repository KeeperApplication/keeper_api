package xyz.piod.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.piod.keeper.dto.ChatMessage;
import xyz.piod.keeper.dto.ChatRoomResponse;
import xyz.piod.keeper.entity.ChatRoom;
import xyz.piod.keeper.entity.HiddenChatRoom;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.exception.ResourceNotFoundException;
import xyz.piod.keeper.exception.UnauthorizedOperationException;
import xyz.piod.keeper.mapper.ChatRoomMapper;
import xyz.piod.keeper.mapper.UserMapper;
import xyz.piod.keeper.repository.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final FriendshipRepository friendshipRepository;
    private final HiddenChatRoomRepository hiddenChatRoomRepository;
    private final BroadcastService broadcastService;
    private final ChatRoomMapper chatRoomMapper;
    private final UserMapper userMapper;
    private final UserService userService;

    @Transactional
    public ChatRoomResponse getOrCreateDirectMessageChannel(String username1, String username2) {
        User user1 = userService.findUserByUsername(username1);
        User user2 = userService.findUserByUsername(username2);

        friendshipRepository.findFriendshipBetween(user1, user2)
                .filter(f -> f.getStatus() == xyz.piod.keeper.entity.FriendshipStatus.ACCEPTED)
                .orElseThrow(() -> new UnauthorizedOperationException("Users are not friends."));

        Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findPrivateRoomBetween(user1, user2);

        if (existingRoomOpt.isPresent()) {
            ChatRoom existingRoom = existingRoomOpt.get();
            hiddenChatRoomRepository.findByUserAndChatRoom(user1, existingRoom)
                    .ifPresent(hiddenChatRoomRepository::delete);
            return chatRoomMapper.toChatRoomResponse(existingRoom);
        } else {
            ChatRoom newRoom = createDirectMessageChannelAndNotify(user1, user2);
            return chatRoomMapper.toChatRoomResponse(newRoom);
        }
    }

    private ChatRoom createDirectMessageChannelAndNotify(User user1, User user2) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(user1.getUsername() + " & " + user2.getUsername());
        chatRoom.setOwner(null);
        chatRoom.setInviteCode(UUID.randomUUID().toString());
        chatRoom.setPrivate(true);
        chatRoom.setParticipants(Set.of(user1, user2));

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        ChatRoomResponse roomResponse = chatRoomMapper.toChatRoomResponse(savedRoom);

        ChatMessage notification = new ChatMessage();
        notification.setType(ChatMessage.MessageType.DM_CHANNEL_CREATED);
        notification.setRoom(roomResponse);

        broadcastService.broadcast("user:" + user1.getUsername(), "new_event", notification);
        broadcastService.broadcast("user:" + user2.getUsername(), "new_event", notification);

        log.info("Created and broadcasted new DM channel #{} for users {} and {}", savedRoom.getId(), user1.getUsername(), user2.getUsername());

        return savedRoom;
    }

    public Page<ChatRoomResponse> getDirectMessagesForUser(User user, Pageable pageable) {
        Page<Long> roomIdsPage = chatRoomRepository.findVisiblePrivateRoomIdsForUser(user, pageable);
        List<Long> roomIds = roomIdsPage.getContent();

        if (roomIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<ChatRoom> rooms = chatRoomRepository.findByIdsWithParticipants(roomIds);

        return roomIdsPage.map(roomId -> {
            ChatRoom room = rooms.stream().filter(r -> r.getId().equals(roomId)).findFirst().orElse(null);
            return chatRoomMapper.toChatRoomResponse(room);
        });
    }

    @Transactional
    public void hideDirectMessageChannel(Long roomId, User currentUser) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));

        if (!chatRoom.isPrivate() || !chatRoom.getParticipants().contains(currentUser)) {
            throw new UnauthorizedOperationException("You can only hide your own private chats.");
        }

        if (hiddenChatRoomRepository.findByUserAndChatRoom(currentUser, chatRoom).isEmpty()) {
            HiddenChatRoom hidden = new HiddenChatRoom();
            hidden.setUser(currentUser);
            hidden.setChatRoom(chatRoom);
            hiddenChatRoomRepository.save(hidden);
        }
    }

    public ChatRoomResponse createGroupChat(String name, User owner) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(name);
        chatRoom.setOwner(owner);
        chatRoom.setInviteCode(UUID.randomUUID().toString());
        chatRoom.getParticipants().add(owner);
        chatRoom.setPrivate(false);

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        return chatRoomMapper.toChatRoomResponse(savedRoom);
    }

    public Page<ChatRoomResponse> getGroupChatsForUser(User user, Pageable pageable) {
        Page<Long> roomIdsPage = chatRoomRepository.findVisiblePublicRoomIdsForUser(user, pageable);
        List<Long> roomIds = roomIdsPage.getContent();

        if (roomIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<ChatRoom> rooms = chatRoomRepository.findByIdsWithParticipants(roomIds);

        return roomIdsPage.map(roomId -> {
            ChatRoom room = rooms.stream().filter(r -> r.getId().equals(roomId)).findFirst().orElse(null);
            return chatRoomMapper.toChatRoomResponse(room);
        });
    }

    public ChatRoomResponse joinGroupChat(String inviteCode, User user) {
        ChatRoom chatRoom = chatRoomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with invite code: " + inviteCode));

        if (chatRoom.isPrivate()) {
            throw new UnauthorizedOperationException("Cannot join a private room with an invite code.");
        }

        if (chatRoom.getParticipants().contains(user)) {
            return chatRoomMapper.toChatRoomResponse(chatRoom);
        }

        chatRoom.getParticipants().add(user);
        ChatRoom updatedRoom = chatRoomRepository.save(chatRoom);

        ChatMessage joinNotification = new ChatMessage();
        joinNotification.setType(ChatMessage.MessageType.USER_JOINED);
        joinNotification.setRoomId(chatRoom.getId());
        joinNotification.setUserActionParticipant(userMapper.toUserResponse(user));

        String topic = "room:" + chatRoom.getId();
        broadcastService.broadcast(topic, "new_event", joinNotification);

        return chatRoomMapper.toChatRoomResponse(updatedRoom);
    }

    public ChatRoomResponse updateGroupChat(Long roomId, String newName, User currentUser) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + roomId));

        if (chatRoom.isPrivate() || chatRoom.getOwner() == null || !chatRoom.getOwner().equals(currentUser)) {
            throw new UnauthorizedOperationException("Only the room owner can edit the room.");
        }

        chatRoom.setName(newName);
        ChatRoom updatedRoom = chatRoomRepository.save(chatRoom);
        return chatRoomMapper.toChatRoomResponse(updatedRoom);
    }

    public void deleteGroupChat(Long roomId, User currentUser) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + roomId));

        if (chatRoom.isPrivate() || chatRoom.getOwner() == null || !chatRoom.getOwner().equals(currentUser)) {
            throw new UnauthorizedOperationException("Only the room owner can delete the room.");
        }

        messageRepository.deleteByChatRoomId(roomId);
        chatRoomRepository.delete(chatRoom);
    }

    public void kickParticipant(Long roomId, Long userIdToKick, User currentUser) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found with id: " + roomId));

        if (chatRoom.isPrivate() || chatRoom.getOwner() == null || !chatRoom.getOwner().equals(currentUser)) {
            throw new UnauthorizedOperationException("Only the room owner can kick participants.");
        }

        User userToKick = userService.findUserById(userIdToKick);

        if (userToKick.equals(currentUser)) {
            throw new UnauthorizedOperationException("The room owner cannot be kicked.");
        }

        if (chatRoom.getParticipants().remove(userToKick)) {
            chatRoomRepository.save(chatRoom);
        } else {
            throw new ResourceNotFoundException("User is not a participant of this room.");
        }
    }
}