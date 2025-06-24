package org.frank.devspace.controller;

import org.frank.devspace.model.Post;
import org.frank.devspace.repository.PostRepository;
import org.frank.devspace.repository.CommentRepository;
import org.frank.devspace.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class HomeController {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Autowired
    public HomeController(PostRepository postRepository, CommentRepository commentRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "landing";
        }
        List<Post> posts = postRepository.findAll();
        posts.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        Map<Long, Long> commentCounts = new HashMap<>();
        for (Post post : posts) {
            commentCounts.put(post.getId(), commentRepository.countByPost(post));
        }
        model.addAttribute("posts", posts);
        model.addAttribute("commentCounts", commentCounts);
        Optional<org.frank.devspace.model.User> userOpt = userDetails != null ? userRepository.findByUsername(userDetails.getUsername()) : Optional.empty();
        userOpt.ifPresent(user -> model.addAttribute("currentUser", user));
        return "home";
    }
} 