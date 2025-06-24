package org.frank.devspace.repository;

import org.frank.devspace.model.Post;
import org.frank.devspace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByAuthor(User author);
} 