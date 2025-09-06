package xyz.piod.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.piod.keeper.dto.ChatMessage;
import xyz.piod.keeper.entity.Message;
import xyz.piod.keeper.entity.Reaction;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.exception.ResourceNotFoundException;
import xyz.piod.keeper.mapper.MessageMapper;
import xyz.piod.keeper.repository.MessageRepository;
import xyz.piod.keeper.repository.ReactionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final MessageRepository messageRepository;
    private final BroadcastService broadcastService;
    private final MessageMapper messageMapper;
    private final UserService userService;

    @Transactional
    public void toggleReaction(Long messageId, String username, String emoji) {
        User user = userService.findUserByUsername(username);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        reactionRepository.findByUserAndMessageAndEmoji(user, message, emoji)
                .ifPresentOrElse(
                        reactionRepository::delete,
                        () -> {
                            Reaction newReaction = new Reaction();
                            newReaction.setUser(user);
                            newReaction.setMessage(message);
                            newReaction.setEmoji(emoji);
                            reactionRepository.save(newReaction);
                        }
                );

        Message updatedMessage = messageRepository.findByIdWithReactions(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found after reaction update: " + messageId));

        Long roomId = updatedMessage.getChatRoom().getId();

        ChatMessage broadcastMessage = new ChatMessage();
        broadcastMessage.setType(ChatMessage.MessageType.REACTION_UPDATE);
        broadcastMessage.setRoomId(roomId);
        broadcastMessage.setUpdatedMessage(messageMapper.toMessageResponse(updatedMessage));

        String topic = "room:" + roomId;
        broadcastService.broadcast(topic, "new_event", broadcastMessage);
    }
}