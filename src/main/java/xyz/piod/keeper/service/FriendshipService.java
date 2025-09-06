package xyz.piod.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.piod.keeper.dto.ChatMessage;
import xyz.piod.keeper.dto.FriendshipResponse;
import xyz.piod.keeper.dto.PendingRequestResponse;
import xyz.piod.keeper.dto.UserResponse;
import xyz.piod.keeper.entity.Friendship;
import xyz.piod.keeper.entity.FriendshipStatus;
import xyz.piod.keeper.entity.User;
import xyz.piod.keeper.exception.ResourceNotFoundException;
import xyz.piod.keeper.mapper.UserMapper;
import xyz.piod.keeper.repository.FriendshipRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final BroadcastService broadcastService;
    private final UserMapper userMapper;
    private final UserService userService;

    @Transactional
    public void sendFriendRequest(String requesterUsername, String addresseePublicId) {
        User requester = findUserByUsername(requesterUsername);
        User addressee = userService.findUserByPublicId(addresseePublicId);

        if (requester.equals(addressee)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself.");
        }

        friendshipRepository.findFriendshipBetween(requester, addressee).ifPresent(f -> {
            throw new IllegalStateException("A friendship or pending request already exists between these users.");
        });

        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);

        notifyUser(addressee.getUsername(), "FRIEND_REQUEST_RECEIVED", userMapper.toUserResponse(requester));
    }

    @Transactional
    public void acceptFriendRequest(String currentUsername, String requesterUsername) {
        User currentUser = findUserByUsername(currentUsername);
        User requester = findUserByUsername(requesterUsername);

        Friendship friendship = friendshipRepository.findFriendshipBetween(currentUser, requester)
                .filter(f -> f.getStatus() == FriendshipStatus.PENDING && f.getAddressee().equals(currentUser))
                .orElseThrow(() -> new ResourceNotFoundException("Pending friend request not found."));

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        notifyUser(requesterUsername, "FRIEND_REQUEST_ACCEPTED", userMapper.toUserResponse(currentUser));
    }

    @Transactional
    public void declineFriendRequest(String currentUsername, String requesterUsername) {
        User currentUser = findUserByUsername(currentUsername);
        User requester = findUserByUsername(requesterUsername);

        Friendship friendship = friendshipRepository.findFriendshipBetween(currentUser, requester)
                .filter(f -> f.getStatus() == FriendshipStatus.PENDING && f.getAddressee().equals(currentUser))
                .orElseThrow(() -> new ResourceNotFoundException("Pending friend request not found."));

        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void removeFriend(String currentUsername, String friendUsername) {
        User currentUser = findUserByUsername(currentUsername);
        User friend = findUserByUsername(friendUsername);

        Friendship friendship = friendshipRepository.findFriendshipBetween(currentUser, friend)
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found."));

        friendshipRepository.delete(friendship);

        notifyUser(friendUsername, "FRIEND_REMOVED", userMapper.toUserResponse(currentUser));
    }

    public List<FriendshipResponse> getFriends(String username) {
        User user = findUserByUsername(username);
        return friendshipRepository.findFriends(user).stream()
                .map(friendship -> {
                    User friend = friendship.getRequester().equals(user) ? friendship.getAddressee() : friendship.getRequester();
                    return new FriendshipResponse(friend.getId(), friend.getUsername(), friend.getProfilePicture());
                })
                .collect(Collectors.toList());
    }

    public List<PendingRequestResponse> getPendingRequests(String username) {
        User user = findUserByUsername(username);

        List<PendingRequestResponse> incoming = friendshipRepository.findByAddresseeAndStatus(user, FriendshipStatus.PENDING)
                .stream()
                .map(friendship -> new PendingRequestResponse(
                        userMapper.toUserResponse(friendship.getRequester()),
                        PendingRequestResponse.RequestDirection.INCOMING
                ))
                .toList();

        List<PendingRequestResponse> outgoing = friendshipRepository.findByRequesterAndStatus(user, FriendshipStatus.PENDING)
                .stream()
                .map(friendship -> new PendingRequestResponse(
                        userMapper.toUserResponse(friendship.getAddressee()),
                        PendingRequestResponse.RequestDirection.OUTGOING
                ))
                .toList();

        return Stream.concat(incoming.stream(), outgoing.stream()).collect(Collectors.toList());
    }

    @Transactional
    public void cancelFriendRequest(String requesterUsername, String addresseeUsername) {
        User requester = findUserByUsername(requesterUsername);
        User addressee = findUserByUsername(addresseeUsername);

        Friendship friendship = friendshipRepository.findByRequesterAndAddresseeAndStatus(requester, addressee, FriendshipStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Outgoing friend request not found."));

        friendshipRepository.delete(friendship);
    }

    private User findUserByUsername(String username) {
        return userService.findUserByUsername(username);
    }

    private void notifyUser(String username, String type, UserResponse payload) {
        ChatMessage notification = new ChatMessage();
        notification.setType(ChatMessage.MessageType.valueOf(type));
        notification.setUserActionParticipant(payload);

        String topic = "user:" + username;
        broadcastService.broadcast(topic, "new_event", notification);
    }
}