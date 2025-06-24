package org.frank.devspace.repository;

import org.frank.devspace.model.Like;
import org.frank.devspace.model.Post;
import org.frank.devspace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndPost(User user, Post post);
    List<Like> findByPost(Post post);
} 