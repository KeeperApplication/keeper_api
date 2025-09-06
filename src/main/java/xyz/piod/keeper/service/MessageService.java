package xyz.piod.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import xyz.piod.keeper.dto.ChatMessage;
import xyz.piod.keeper.dto.LinkPreviewRequest;
import xyz.piod.keeper.dto.MessageResponse;
import xyz.piod.keeper.entity.ChatRoom;
import xyz.piod.keeper.entity.Message;
import xyz.piod.keeper.entity.MessageReadReceipt;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.exception.ResourceNotFoundException;
import xyz.piod.keeper.exception.UnauthorizedOperationException;
import xyz.piod.keeper.mapper.MessageMapper;
import xyz.piod.keeper.repository.ChatRoomRepository;
import xyz.piod.keeper.repository.MessageReadReceiptRepository;
import xyz.piod.keeper.repository.MessageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageReadReceiptRepository readReceiptRepository;
    private final BroadcastService broadcastService;
    private final MessageMapper messageMapper;
    private final UserService userService;

    public List<MessageResponse> getPinnedMessagesForRoom(Long roomId) {
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userService.findUserByUsername(principal.getUsername());

        if (!chatRoomRepository.isUserParticipant(roomId, currentUser.getId())) {
            throw new UnauthorizedOperationException("User is not a participant of this room.");
        }

        return messageRepository.findByChatRoomIdAndIsPinnedTrueOrderByTimestampDesc(roomId)
                .stream()
                .map(messageMapper::toMessageResponse)
                .collect(Collectors.toList());
    }

    public Page<MessageResponse> getMessagesForRoom(Long roomId, int page, int size) {
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userService.findUserByUsername(principal.getUsername());

        if (!chatRoomRepository.isUserParticipant(roomId, currentUser.getId())) {
            throw new UnauthorizedOperationException("User is not a participant of this room.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<Message> messagesPage = messageRepository.findByChatRoomIdOrderByTimestampDesc(roomId, pageable);
        return messagesPage.map(messageMapper::toMessageResponse);
    }

    public void saveAndBroadcastMessage(Long roomId, ChatMessage chatMessage, User sender) {
        if (chatMessage.getContent() == null || chatMessage.getContent().length() > 2000) {
            log.warn("User {} attempted to send a message exceeding the 2000 character limit in room {}.",
                    sender.getUsername(), roomId);
            return;
        }

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatRoom not found with id: " + roomId));

        Message message = new Message();
        message.setContent(chatMessage.getContent());
        message.setSender(sender);
        message.setChatRoom(chatRoom);

        if (chatMessage.getRepliedToId() != null) {
            Message repliedToMessage = messageRepository.findById(chatMessage.getRepliedToId()).orElse(null);
            message.setRepliedTo(repliedToMessage);
        }

        Message savedMessage = messageRepository.save(message);

        ChatMessage broadcastMessage = new ChatMessage();
        broadcastMessage.setId(savedMessage.getId());
        broadcastMessage.setType(ChatMessage.MessageType.CHAT);
        broadcastMessage.setContent(savedMessage.getContent());
        broadcastMessage.setRoomId(roomId);
        broadcastMessage.setSenderUsername(sender.getUsername());
        broadcastMessage.setSenderProfilePicture(sender.getProfilePicture());
        broadcastMessage.setRepliedTo(messageMapper.toRepliedMessageInfo(savedMessage.getRepliedTo()));
        broadcastMessage.setTimestamp(savedMessage.getTimestamp());
        broadcastMessage.setEdited(savedMessage.isEdited());

        String topic = "room:" + roomId;
        broadcastService.broadcast(topic, "new_event", broadcastMessage);

    }

    public void updateMessageWithLinkPreview(Long messageId, LinkPreviewRequest previewRequest) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        message.setLinkPreviewUrl(previewRequest.url());
        message.setLinkPreviewTitle(previewRequest.title());
        message.setLinkPreviewDescription(previewRequest.description());
        message.setLinkPreviewImage(previewRequest.image());

        Message updatedMessage = messageRepository.save(message);

        ChatMessage broadcastMessage = new ChatMessage();
        broadcastMessage.setType(ChatMessage.MessageType.MESSAGE_UPDATED);
        broadcastMessage.setRoomId(updatedMessage.getChatRoom().getId());
        broadcastMessage.setUpdatedMessage(messageMapper.toMessageResponse(updatedMessage));

        String topic = "room:" + updatedMessage.getChatRoom().getId();
        broadcastService.broadcast(topic, "new_event", broadcastMessage);
    }

    public void editMessage(ChatMessage chatMessage, User currentUser) {
        Message message = messageRepository.findById(chatMessage.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + chatMessage.getId()));

        if (!message.getSender().equals(currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to edit this message");
        }

        message.setContent(chatMessage.getContent());
        message.setEdited(true);
        messageRepository.save(message);

        Long roomId = message.getChatRoom().getId();

        ChatMessage broadcastMessage = new ChatMessage();
        broadcastMessage.setId(message.getId());
        broadcastMessage.setType(ChatMessage.MessageType.EDIT);
        broadcastMessage.setContent(message.getContent());
        broadcastMessage.setRoomId(roomId);
        broadcastMessage.setEdited(true);

        String topic = "room:" + roomId;
        broadcastService.broadcast(topic, "new_event", broadcastMessage);
    }

    public void deleteMessage(ChatMessage chatMessage, User currentUser) {
        Message message = messageRepository.findById(chatMessage.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + chatMessage.getId()));

        if (!message.getSender().equals(currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to delete this message");
        }

        Long roomId = message.getChatRoom().getId();

        messageRepository.delete(message);

        chatMessage.setType(ChatMessage.MessageType.DELETE);
        chatMessage.setRoomId(roomId);

        String topic = "room:" + roomId;
        broadcastService.broadcast(topic, "new_event", chatMessage);
    }

    public void togglePinMessage(Long messageId, User currentUser) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        ChatRoom chatRoom = message.getChatRoom();
        if (chatRoom.getOwner() == null || !chatRoom.getOwner().equals(currentUser)) {
            throw new UnauthorizedOperationException("Only the room owner can pin messages.");
        }

        message.setPinned(!message.isPinned());
        Message updatedMessage = messageRepository.save(message);

        ChatMessage broadcastMessage = new ChatMessage();
        broadcastMessage.setType(ChatMessage.MessageType.PIN_UPDATE);
        broadcastMessage.setRoomId(chatRoom.getId());
        broadcastMessage.setUpdatedMessage(messageMapper.toMessageResponse(updatedMessage));

        String topic = "room:" + chatRoom.getId();
        broadcastService.broadcast(topic, "new_event", broadcastMessage);
    }

    public void markMessagesAsSeen(User user, Long roomId, Long lastMessageId) {
        Set<Long> alreadySeenMessageIds = readReceiptRepository.findMessageIdsSeenByUserInRoom(roomId, user.getId());
        List<Message> messagesToConsider = messageRepository.findByIdLessThanEqualAndChatRoomId(lastMessageId, roomId);

        List<MessageReadReceipt> newReceipts = new ArrayList<>();
        for (Message message : messagesToConsider) {
            if (!alreadySeenMessageIds.contains(message.getId())) {
                MessageReadReceipt receipt = new MessageReadReceipt();
                receipt.setUser(user);
                receipt.setMessage(message);
                newReceipts.add(receipt);
            }
        }

        if (!newReceipts.isEmpty()) {
            readReceiptRepository.saveAll(newReceipts);

            ChatMessage broadcastMessage = new ChatMessage();
            broadcastMessage.setType(ChatMessage.MessageType.MESSAGES_SEEN);
            broadcastMessage.setRoomId(roomId);
            broadcastMessage.setSenderUsername(user.getUsername());
            broadcastMessage.setLastMessageId(lastMessageId);

            String topic = "room:" + roomId;
            broadcastService.broadcast(topic, "new_event", broadcastMessage);
        }
    }
}