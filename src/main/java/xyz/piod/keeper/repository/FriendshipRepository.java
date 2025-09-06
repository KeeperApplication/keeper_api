package xyz.piod.keeper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.piod.keeper.entity.Friendship;
import xyz.piod.keeper.entity.FriendshipStatus;
import xyz.piod.keeper.entity.User;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByAddresseeAndStatus(User addressee, FriendshipStatus status);

    List<Friendship> findByRequesterAndStatus(User requester, FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE f.status = 'ACCEPTED' AND (f.requester = :user OR f.addressee = :user)")
    List<Friendship> findFriends(@Param("user") User user);

    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user1 AND f.addressee = :user2) OR (f.requester = :user2 AND f.addressee = :user1)")
    Optional<Friendship> findFriendshipBetween(@Param("user1") User user1, @Param("user2") User user2);

    Optional<Friendship> findByRequesterAndAddresseeAndStatus(User requester, User addressee, FriendshipStatus status);
}