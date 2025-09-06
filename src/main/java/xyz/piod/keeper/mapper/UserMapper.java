package xyz.piod.keeper.mapper;

import org.mapstruct.Mapper;
import xyz.piod.keeper.dto.UserResponse;
import xyz.piod.keeper.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
}