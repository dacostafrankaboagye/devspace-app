package org.frank.devspace.controller;

import org.frank.devspace.model.Like;
import org.frank.devspace.model.Post;
import org.frank.devspace.model.User;
import org.frank.devspace.repository.LikeRepository;
import org.frank.devspace.repository.PostRepository;
import org.frank.devspace.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LikeController {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Autowired
    public LikeController(LikeRepository likeRepository, PostRepository postRepository, UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/posts/{id}/like")
    public String likePost(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, @RequestHeader(value = "Referer", required = false) String referer, RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to like posts.");
            return "redirect:/login";
        }
        Optional<Post> postOpt = postRepository.findById(id);
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        if (postOpt.isPresent() && userOpt.isPresent()) {
            Optional<Like> likeOpt = likeRepository.findByUserAndPost(userOpt.get(), postOpt.get());
            if (likeOpt.isEmpty()) {
                Like like = Like.builder().user(userOpt.get()).post(postOpt.get()).build();
                likeRepository.save(like);
                redirectAttributes.addFlashAttribute("success", "Post liked!");
            } else {
                likeRepository.delete(likeOpt.get());
                redirectAttributes.addFlashAttribute("info", "Like removed.");
            }
        }
        return referer != null ? "redirect:" + referer : "redirect:/";
    }
} 