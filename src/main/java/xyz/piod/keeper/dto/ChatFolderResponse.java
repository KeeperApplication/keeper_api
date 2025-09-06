package xyz.piod.keeper.dto;

import java.util.Set;

public record ChatFolderResponse(Long id, String name, Set<ChatRoomResponse> rooms) {}