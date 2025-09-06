package xyz.piod.keeper.mapper;

import org.mapstruct.Mapper;
import xyz.piod.keeper.dto.ChatRoomResponse;
import xyz.piod.keeper.entity.ChatRoom;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ChatRoomMapper {
    ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom);
}