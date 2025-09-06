package xyz.piod.keeper.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class MessageResponse {
    private Long id;
    private String content;
    private String senderUsername;
    private String senderProfilePicture;
    private Set<ReactionResponse> reactions;
    private RepliedMessageInfo repliedTo;
    private boolean edited;
    private boolean isPinned;
    private String linkPreviewUrl;
    private String linkPreviewTitle;
    private String linkPreviewDescription;
    private String linkPreviewImage;
    private Set<String> seenBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;
}