package xyz.piod.keeper.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import xyz.piod.keeper.dto.MessageResponse;
import xyz.piod.keeper.dto.ReactionResponse;
import xyz.piod.keeper.dto.RepliedMessageInfo;
import xyz.piod.keeper.entity.Message;
import xyz.piod.keeper.entity.Reaction;
import xyz.piod.keeper.entity.MessageReadReceipt;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(source = "sender.username", target = "senderUsername")
    @Mapping(source = "sender.profilePicture", target = "senderProfilePicture")
    @Mapping(target = "seenBy", expression = "java(mapSeenBy(message.getReadReceipts()))")
    MessageResponse toMessageResponse(Message message);

    @Mapping(source = "user.username", target = "username")
    ReactionResponse toReactionResponse(Reaction reaction);

    @Mapping(source = "sender.username", target = "senderUsername")
    RepliedMessageInfo toRepliedMessageInfo(Message message);

    default Set<String> mapSeenBy(Set<MessageReadReceipt> readReceipts) {
        if (readReceipts == null) {
            return null;
        }
        return readReceipts.stream()
                           .map(r -> r.getUser().getUsername())
                           .collect(Collectors.toSet());
    }
}