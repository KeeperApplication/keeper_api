package xyz.piod.keeper.mapper;

import org.mapstruct.Mapper;
import xyz.piod.keeper.dto.ChatFolderResponse;
import xyz.piod.keeper.entity.ChatFolder;

@Mapper(componentModel = "spring", uses = {ChatRoomMapper.class})
public interface ChatFolderMapper {
    ChatFolderResponse toChatFolderResponse(ChatFolder chatFolder);
}