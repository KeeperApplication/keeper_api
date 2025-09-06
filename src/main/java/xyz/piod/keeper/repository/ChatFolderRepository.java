package xyz.piod.keeper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.piod.keeper.entity.ChatFolder;
import xyz.piod.keeper.entity.User;

import java.util.List;

public interface ChatFolderRepository extends JpaRepository<ChatFolder, Long> {
    List<ChatFolder> findByUser(User user);
}