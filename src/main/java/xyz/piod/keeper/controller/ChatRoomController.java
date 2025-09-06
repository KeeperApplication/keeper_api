package xyz.piod.keeper.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import xyz.piod.keeper.dto.ChatRoomCreateRequest;
import xyz.piod.keeper.dto.ChatRoomResponse;
import xyz.piod.keeper.dto.MessageResponse;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.repository.ChatRoomRepository;
import xyz.piod.keeper.service.ChatRoomService;
import xyz.piod.keeper.service.MessageService;
import xyz.piod.keeper.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final ChatRoomRepository chatRoomRepository;
    private final UserService userService;

    private User getAuthenticatedUser(UserDetails principal) {
        return userService.findUserByUsername(principal.getUsername());
    }

    @PostMapping("/groups")
    public ResponseEntity<ChatRoomResponse> createGroupChat(@Valid @RequestBody ChatRoomCreateRequest request,
                                                            @AuthenticationPrincipal UserDetails principal) {
        User owner = getAuthenticatedUser(principal);
        ChatRoomResponse newRoom = chatRoomService.createGroupChat(request.getName(), owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(newRoom);
    }

    @GetMapping("/groups")
    public ResponseEntity<Page<ChatRoomResponse>> getGroupChatsForUser(
            @AuthenticationPrincipal UserDetails principal,
            Pageable pageable) {
        User user = getAuthenticatedUser(principal);
        Page<ChatRoomResponse> rooms = chatRoomService.getGroupChatsForUser(user, pageable);
        return ResponseEntity.ok(rooms);
    }

    @PostMapping("/groups/{inviteCode}/join")
    public ResponseEntity<ChatRoomResponse> joinGroupChat(@PathVariable String inviteCode,
                                                          @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        ChatRoomResponse updatedRoom = chatRoomService.joinGroupChat(inviteCode, user);
        return ResponseEntity.ok(updatedRoom);
    }

    @PutMapping("/groups/{roomId}")
    public ResponseEntity<ChatRoomResponse> updateGroupChat(@PathVariable Long roomId,
                                                            @Valid @RequestBody ChatRoomCreateRequest request,
                                                            @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getAuthenticatedUser(principal);
        ChatRoomResponse updatedRoom = chatRoomService.updateGroupChat(roomId, request.getName(), currentUser);
        return ResponseEntity.ok(updatedRoom);
    }

    @DeleteMapping("/groups/{roomId}")
    public ResponseEntity<Void> deleteGroupChat(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getAuthenticatedUser(principal);
        chatRoomService.deleteGroupChat(roomId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/groups/{roomId}/participants/{userIdToKick}")
    public ResponseEntity<Void> kickParticipant(@PathVariable Long roomId,
                                                @PathVariable Long userIdToKick,
                                                @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getAuthenticatedUser(principal);
        chatRoomService.kickParticipant(roomId, userIdToKick, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/dms/{username}")
    public ResponseEntity<ChatRoomResponse> getOrCreateDirectMessageChannel(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails principal) {
        ChatRoomResponse room = chatRoomService.getOrCreateDirectMessageChannel(principal.getUsername(), username);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/dms")
    public ResponseEntity<Page<ChatRoomResponse>> getDirectMessagesForUser(
            @AuthenticationPrincipal UserDetails principal,
            Pageable pageable) {
        User user = getAuthenticatedUser(principal);
        Page<ChatRoomResponse> dms = chatRoomService.getDirectMessagesForUser(user, pageable);
        return ResponseEntity.ok(dms);
    }

    @DeleteMapping("/dms/{roomId}/hide")
    public ResponseEntity<Void> hideDirectMessageChannel(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getAuthenticatedUser(principal);
        chatRoomService.hideDirectMessageChannel(roomId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessagesForRoom(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<MessageResponse> messages = messageService.getMessagesForRoom(roomId, page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{roomId}/authorize-join")
    public ResponseEntity<Void> authorizeJoin(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails principal) {
        User user = getAuthenticatedUser(principal);
        boolean isParticipant = chatRoomRepository.isUserParticipant(roomId, user.getId());

        if (isParticipant) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/{roomId}/pinned")
    public ResponseEntity<List<MessageResponse>> getPinnedMessages(@PathVariable Long roomId) {
        List<MessageResponse> pinnedMessages = messageService.getPinnedMessagesForRoom(roomId);
        return ResponseEntity.ok(pinnedMessages);
    }
}