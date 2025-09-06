package xyz.piod.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import xyz.piod.keeper.config.RabbitMQConfig;
import xyz.piod.keeper.dto.ChatMessage;
import xyz.piod.keeper.dto.command.MessageCommand;
import xyz.piod.keeper.entity.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandConsumerService {

    private final MessageService messageService;
    private final ReactionService reactionService;
    private final JwtService jwtService;
    private final UserService userService;

    private User getUserByToken(String token) {
        String username = jwtService.extractUsername(token);
        return userService.findUserByUsername(username);
    }

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_COMMANDS_QUEUE)
    public void handleMessageCommand(MessageCommand command, @Header("amqp_receivedRoutingKey") String routingKey) {
        String action = routingKey.substring(routingKey.lastIndexOf('.') + 1);

        switch (action) {
            case "new_message":
                handleNewMessageCommand(command);
                break;
            case "edit_message":
                handleEditMessageCommand(command);
                break;
            case "delete_message":
                handleDeleteMessageCommand(command);
                break;
            case "toggle_pin":
                handleTogglePinCommand(command);
                break;
            case "toggle_reaction":
                handleToggleReactionCommand(command);
                break;
            case "messages_seen":
                handleMessagesSeenCommand(command);
                break;
            default:
                log.warn("Received a message command with unknown action: {}", action);
        }
    }

    private void handleNewMessageCommand(MessageCommand command) {
        try {
            User sender = getUserByToken(command.getUserToken());
            log.info("Received new_message command from user '{}' for room {}", sender.getUsername(), command.getRoomId());

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setContent(command.getContent());
            chatMessage.setRepliedToId(command.getRepliedToId());

            messageService.saveAndBroadcastMessage(command.getRoomId(), chatMessage, sender);
        } catch (Exception e) {
            log.error("Failed to process new_message command: {}", e.getMessage(), e);
        }
    }

    private void handleEditMessageCommand(MessageCommand command) {
        try {
            User currentUser = getUserByToken(command.getUserToken());
            log.info("Received edit_message command from user '{}' for message {}", currentUser.getUsername(), command.getMessageId());

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setId(command.getMessageId());
            chatMessage.setContent(command.getContent());

            messageService.editMessage(chatMessage, currentUser);
        } catch (Exception e) {
            log.error("Failed to process edit_message command for message {}: {}", command.getMessageId(), e.getMessage(), e);
        }
    }

    private void handleDeleteMessageCommand(MessageCommand command) {
        try {
            User currentUser = getUserByToken(command.getUserToken());
            log.info("Received delete_message command from user '{}' for message {}", currentUser.getUsername(), command.getMessageId());

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setId(command.getMessageId());

            messageService.deleteMessage(chatMessage, currentUser);
        } catch (Exception e) {
            log.error("Failed to process delete_message command for message {}: {}", command.getMessageId(), e.getMessage(), e);
        }
    }

    private void handleTogglePinCommand(MessageCommand command) {
        try {
            User currentUser = getUserByToken(command.getUserToken());
            log.info("Received toggle_pin command from user '{}' for message {}", currentUser.getUsername(), command.getMessageId());
            messageService.togglePinMessage(command.getMessageId(), currentUser);
        } catch (Exception e) {
            log.error("Failed to process toggle_pin command for message {}: {}", command.getMessageId(), e.getMessage(), e);
        }
    }

    private void handleToggleReactionCommand(MessageCommand command) {
        try {
            User currentUser = getUserByToken(command.getUserToken());
            log.info("Received toggle_reaction command from user '{}' for message {}", currentUser.getUsername(), command.getMessageId());
            reactionService.toggleReaction(command.getMessageId(), currentUser.getUsername(), command.getEmoji());
        } catch (Exception e) {
            log.error("Failed to process toggle_reaction command for message {}: {}", command.getMessageId(), e.getMessage(), e);
        }
    }

    private void handleMessagesSeenCommand(MessageCommand command) {
        try {
            User currentUser = getUserByToken(command.getUserToken());
            log.info("Received messages_seen command from user '{}' for room {}", currentUser.getUsername(), command.getRoomId());
            messageService.markMessagesAsSeen(currentUser, command.getRoomId(), command.getLastMessageId());
        } catch (Exception e) {
            log.error("Failed to process messages_seen command for room {}: {}", command.getRoomId(), e.getMessage(), e);
        }
    }
}