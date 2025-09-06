package xyz.piod.keeper.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private MessageType type;
    private Long id;
    private String content;
    private String senderUsername;
    private String senderProfilePicture;
    private Long repliedToId;
    private RepliedMessageInfo repliedTo;
    private Long roomId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;
    private boolean edited;

    private MessageResponse updatedMessage;
    private UserResponse userActionParticipant;

    private ChatRoomResponse room;

    private String linkPreviewUrl;
    private String linkPreviewTitle;
    private String linkPreviewDescription;
    private String linkPreviewImage;
    private Long lastMessageId;

    public enum MessageType {
        CHAT,
        TYPING,
        EDIT,
        DELETE,
        REACTION_UPDATE,
        MESSAGE_UPDATED,
        USER_JOINED,
        FRIEND_REQUEST_RECEIVED,
        FRIEND_REQUEST_ACCEPTED,
        FRIEND_REMOVED,
        PIN_UPDATE,
        MESSAGES_SEEN,
        DM_CHANNEL_CREATED
    }
}