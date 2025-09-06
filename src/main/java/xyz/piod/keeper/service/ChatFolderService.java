package xyz.piod.keeper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.piod.keeper.dto.ChatFolderResponse;
import xyz.piod.keeper.entity.ChatFolder;
import xyz.piod.keeper.entity.ChatRoom;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.exception.ResourceNotFoundException;
import xyz.piod.keeper.exception.UnauthorizedOperationException;
import xyz.piod.keeper.mapper.ChatFolderMapper;
import xyz.piod.keeper.repository.ChatFolderRepository;
import xyz.piod.keeper.repository.ChatRoomRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatFolderService {

    private final ChatFolderRepository folderRepository;
    private final ChatRoomRepository roomRepository;
    private final ChatFolderMapper folderMapper;

    public List<ChatFolderResponse> getFoldersForUser(User user) {
        return folderRepository.findByUser(user).stream()
                .map(folderMapper::toChatFolderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatFolderResponse createFolder(String name, User user) {
        ChatFolder folder = new ChatFolder();
        folder.setName(name);
        folder.setUser(user);
        ChatFolder savedFolder = folderRepository.save(folder);
        return folderMapper.toChatFolderResponse(savedFolder);
    }

    @Transactional
    public ChatFolderResponse renameFolder(Long folderId, String newName, User user) {
        ChatFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        if (!folder.getUser().equals(user)) {
            throw new UnauthorizedOperationException("You can only rename your own folders.");
        }
        folder.setName(newName);
        ChatFolder savedFolder = folderRepository.save(folder);
        return folderMapper.toChatFolderResponse(savedFolder);
    }

    @Transactional
    public void deleteFolder(Long folderId, User user) {
        ChatFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        if (!folder.getUser().equals(user)) {
            throw new UnauthorizedOperationException("You can only delete your own folders.");
        }
        folderRepository.delete(folder);
    }

    @Transactional
    public ChatFolderResponse addRoomToFolder(Long folderId, Long roomId, User user) {
        ChatFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        if (!folder.getUser().equals(user)) {
            throw new UnauthorizedOperationException("Cannot add room to another user's folder.");
        }

        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (room.isPrivate()) {
            throw new UnauthorizedOperationException("Cannot add Direct Messages to folders.");
        }

        folder.getRooms().add(room);
        ChatFolder updatedFolder = folderRepository.save(folder);

        return folderMapper.toChatFolderResponse(updatedFolder);
    }

    @Transactional
    public void removeRoomFromFolder(Long folderId, Long roomId, User user) {
        ChatFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        if (!folder.getUser().equals(user)) {
            throw new UnauthorizedOperationException("Unauthorized action.");
        }

        folder.getRooms().removeIf(room -> room.getId().equals(roomId));
        folderRepository.save(folder);
    }
}