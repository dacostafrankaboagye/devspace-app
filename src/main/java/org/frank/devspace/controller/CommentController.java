package org.frank.devspace.controller;

import org.frank.devspace.model.Comment;
import org.frank.devspace.model.Post;
import org.frank.devspace.model.User;
import org.frank.devspace.repository.CommentRepository;
import org.frank.devspace.repository.PostRepository;
import org.frank.devspace.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/posts")
public class CommentController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    private int getCommentDepth(Comment comment) {
        int depth = 1;
        Comment current = comment.getParent();
        while (current != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    private void buildCommentTree(List<Comment> comments, int depth) {
        if (depth > 4) return;
        for (Comment comment : comments) {
            List<Comment> children = commentRepository.findByParentOrderByCreatedAtAsc(comment);
            comment.setChildren(new java.util.LinkedHashSet<>(children));
            buildCommentTree(children, depth + 1);
        }
    }

    @PostMapping("/{id}/comment")
    public String addComment(@PathVariable Long id, @RequestParam("content") String content, @RequestParam(value = "parentId", required = false) Long parentId, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to comment.");
            return "redirect:/login";
        }
        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Comment cannot be empty.");
            return "redirect:/posts/" + id;
        }
        Optional<Post> postOpt = postRepository.findById(id);
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        Comment parent = null;
        int depth = 1;
        if (parentId != null) {
            parent = commentRepository.findById(parentId).orElse(null);
            if (parent != null) {
                depth = getCommentDepth(parent) + 1;
                if (depth > 4) {
                    redirectAttributes.addFlashAttribute("error", "Replies can only be nested up to 4 levels.");
                    return "redirect:/posts/" + id;
                }
            }
        }
        if (postOpt.isPresent() && userOpt.isPresent()) {
            Comment comment = Comment.builder()
                    .content(content)
                    .author(userOpt.get())
                    .post(postOpt.get())
                    .parent(parent)
                    .build();
            commentRepository.save(comment);
            redirectAttributes.addFlashAttribute("success", "Comment added!");
        }
        return "redirect:/posts/" + id;
    }

    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id, org.springframework.ui.Model model) {
        Optional<Post> postOpt = postRepository.findById(id);
        if (postOpt.isEmpty()) {
            return "redirect:/";
        }
        Post post = postOpt.get();
        // Fetch only top-level comments for display
        List<Comment> topLevelComments = commentRepository.findByPostAndParentIsNullOrderByCreatedAtAsc(post);
        buildCommentTree(topLevelComments, 1);
        long commentCount = commentRepository.countByPost(post);
        model.addAttribute("post", post);
        model.addAttribute("comments", topLevelComments);
        model.addAttribute("commentCount", commentCount);
        return "post";
    }

    @GetMapping("/{id}/comments-fragment")
    public String commentsFragment(@PathVariable Long id, org.springframework.ui.Model model) {
        Optional<Post> postOpt = postRepository.findById(id);
        if (postOpt.isEmpty()) {
            return "::empty";
        }
        Post post = postOpt.get();
        List<Comment> topLevelComments = commentRepository.findByPostAndParentIsNullOrderByCreatedAtAsc(post);
        model.addAttribute("post", post);
        model.addAttribute("comments", topLevelComments);
        return "fragments/comments :: comments";
    }

    @GetMapping("/{postId}/comment/{commentId}/replies")
    public String loadReplies(@PathVariable Long postId, @PathVariable Long commentId, @RequestParam(value = "depth", required = false) Integer depth, org.springframework.ui.Model model) {
        Optional<Post> postOpt = postRepository.findById(postId);
        Optional<Comment> parentOpt = commentRepository.findById(commentId);
        if (postOpt.isEmpty() || parentOpt.isEmpty()) {
            return "::empty";
        }
        List<Comment> replies = commentRepository.findByParentOrderByCreatedAtAsc(parentOpt.get());
        int d = (depth != null) ? depth : getCommentDepth(parentOpt.get()) + 1;
        model.addAttribute("comments", replies);
        model.addAttribute("post", postOpt.get());
        model.addAttribute("parentId", commentId);
        model.addAttribute("depth", d);
        return "fragments/comments :: comments";
    }
} 