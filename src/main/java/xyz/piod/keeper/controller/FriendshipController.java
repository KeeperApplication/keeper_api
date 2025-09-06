package xyz.piod.keeper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import xyz.piod.keeper.dto.FriendshipResponse;
import xyz.piod.keeper.dto.PendingRequestResponse;
import xyz.piod.keeper.service.FriendshipService;

import java.util.List;

@RestController
@RequestMapping("/api/friendships")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/requests/{addresseePublicId}")
    public ResponseEntity<Void> sendFriendRequest(
            @PathVariable String addresseePublicId,
            @AuthenticationPrincipal UserDetails principal) {
        friendshipService.sendFriendRequest(principal.getUsername(), addresseePublicId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/requests/{requesterUsername}/accept")
    public ResponseEntity<Void> acceptFriendRequest(
            @PathVariable String requesterUsername,
            @AuthenticationPrincipal UserDetails principal) {
        friendshipService.acceptFriendRequest(principal.getUsername(), requesterUsername);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/requests/{requesterUsername}/decline")
    public ResponseEntity<Void> declineFriendRequest(
            @PathVariable String requesterUsername,
            @AuthenticationPrincipal UserDetails principal) {
        friendshipService.declineFriendRequest(principal.getUsername(), requesterUsername);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{friendUsername}")
    public ResponseEntity<Void> removeFriend(
            @PathVariable String friendUsername,
            @AuthenticationPrincipal UserDetails principal) {
        friendshipService.removeFriend(principal.getUsername(), friendUsername);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<FriendshipResponse>> getFriends(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(friendshipService.getFriends(principal.getUsername()));
    }

    @DeleteMapping("/requests/{addresseeUsername}/cancel")
    public ResponseEntity<Void> cancelFriendRequest(
            @PathVariable String addresseeUsername,
            @AuthenticationPrincipal UserDetails principal) {
        friendshipService.cancelFriendRequest(principal.getUsername(), addresseeUsername);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<PendingRequestResponse>> getPendingRequests(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(friendshipService.getPendingRequests(principal.getUsername()));
    }
}