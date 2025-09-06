package xyz.piod.keeper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import xyz.piod.keeper.dto.ChatFolderResponse;
import xyz.piod.keeper.dto.FolderCreateRequest;
import xyz.piod.keeper.dto.FolderRoomRequest;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.service.ChatFolderService;
import xyz.piod.keeper.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class ChatFolderController {

    private final ChatFolderService folderService;
    private final UserService userService;

    private User getAuthenticatedUser(UserDetails principal) {
        return userService.findUserByUsername(principal.getUsername());
    }

    @GetMapping
    public ResponseEntity<List<ChatFolderResponse>> getFolders(@AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        return ResponseEntity.ok(folderService.getFoldersForUser(user));
    }

    @PostMapping
    public ResponseEntity<ChatFolderResponse> createFolder(@RequestBody FolderCreateRequest request,
                                                           @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        ChatFolderResponse newFolder = folderService.createFolder(request.name(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(newFolder);
    }

    @PutMapping("/{folderId}")
    public ResponseEntity<ChatFolderResponse> renameFolder(@PathVariable Long folderId,
                                                           @RequestBody FolderCreateRequest request,
                                                           @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        ChatFolderResponse updatedFolder = folderService.renameFolder(folderId, request.name(), user);
        return ResponseEntity.ok(updatedFolder);
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long folderId,
                                             @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        folderService.deleteFolder(folderId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{folderId}/rooms")
    public ResponseEntity<ChatFolderResponse> addRoomToFolder(@PathVariable Long folderId,
                                                              @RequestBody FolderRoomRequest request,
                                                              @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        ChatFolderResponse updatedFolder = folderService.addRoomToFolder(folderId, request.roomId(), user);
        return ResponseEntity.ok(updatedFolder);
    }

    @DeleteMapping("/{folderId}/rooms/{roomId}")
    public ResponseEntity<Void> removeRoomFromFolder(@PathVariable Long folderId,
                                                     @PathVariable Long roomId,
                                                     @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        folderService.removeRoomFromFolder(folderId, roomId, user);
        return ResponseEntity.noContent().build();
    }
}