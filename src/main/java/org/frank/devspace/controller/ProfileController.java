package org.frank.devspace.controller;

import org.frank.devspace.model.User;
import org.frank.devspace.model.Post;
import org.frank.devspace.model.Follow;
import org.frank.devspace.model.AvatarType;
import org.frank.devspace.repository.UserRepository;
import org.frank.devspace.repository.PostRepository;
import org.frank.devspace.repository.FollowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class ProfileController {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;

    @Autowired
    public ProfileController(UserRepository userRepository, PostRepository postRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.followRepository = followRepository;
    }

    @GetMapping("/{username}")
    public String viewProfile(@PathVariable String username, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return "redirect:/";
        }
        User profileUser = userOpt.get();
        List<Post> posts = postRepository.findByAuthor(profileUser);
        boolean isFollowing = false;
        boolean isOwnProfile = false;
        if (userDetails != null) {
            Optional<User> currentUser = userRepository.findByUsername(userDetails.getUsername());
            if (currentUser.isPresent()) {
                isOwnProfile = currentUser.get().getId().equals(profileUser.getId());
                isFollowing = followRepository.findByFollowerAndFollowing(currentUser.get(), profileUser).isPresent();
            }
        }
        long followers = followRepository.findByFollowing(profileUser).size();
        long following = followRepository.findByFollower(profileUser).size();
        model.addAttribute("profileUser", profileUser);
        model.addAttribute("posts", posts);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("followers", followers);
        model.addAttribute("following", following);
        return "profile";
    }

    @PostMapping("/{username}/follow")
    public String follow(@PathVariable String username, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        Optional<User> toFollow = userRepository.findByUsername(username);
        Optional<User> follower = userRepository.findByUsername(userDetails.getUsername());
        if (toFollow.isPresent() && follower.isPresent() && !toFollow.get().getId().equals(follower.get().getId())) {
            followRepository.findByFollowerAndFollowing(follower.get(), toFollow.get())
                .or(() -> Optional.of(followRepository.save(Follow.builder().follower(follower.get()).following(toFollow.get()).build())));
        }
        return "redirect:/profile/" + username;
    }

    @PostMapping("/{username}/unfollow")
    public String unfollow(@PathVariable String username, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        Optional<User> toUnfollow = userRepository.findByUsername(username);
        Optional<User> follower = userRepository.findByUsername(userDetails.getUsername());
        if (toUnfollow.isPresent() && follower.isPresent()) {
            followRepository.findByFollowerAndFollowing(follower.get(), toUnfollow.get())
                .ifPresent(followRepository::delete);
        }
        return "redirect:/profile/" + username;
    }

    @GetMapping("/edit")
    public String editProfileForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) return "redirect:/";
        model.addAttribute("user", userOpt.get());
        model.addAttribute("avatarTypes", AvatarType.values());
        // Example system avatars (filenames in static/avatars/)
        model.addAttribute("systemAvatars", Arrays.asList("avatar1.png", "avatar2.png", "avatar3.png"));
        return "edit-profile";
    }

    @PostMapping("/edit")
    public String editProfileSubmit(@ModelAttribute("user") User formUser,
                                    @RequestParam(value = "avatarType") AvatarType avatarType,
                                    @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                    @RequestParam(value = "systemAvatar", required = false) String systemAvatar,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) return "redirect:/";
        User user = userOpt.get();
        user.setBio(formUser.getBio());
        user.setAvatarType(avatarType);
        if (avatarType == AvatarType.UPLOADED && avatarFile != null && !avatarFile.isEmpty()) {
            // Validate file type and size
            String contentType = avatarFile.getContentType();
            if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
                redirectAttributes.addFlashAttribute("error", "Only JPG and PNG images are allowed.");
                return "redirect:/profile/edit";
            }
            if (avatarFile.getSize() > 1_048_576) { // 1MB
                redirectAttributes.addFlashAttribute("error", "File size must be less than 1MB.");
                return "redirect:/profile/edit";
            }
            // Save file
            String ext = contentType.equals("image/png") ? ".png" : ".jpg";
            String filename = UUID.randomUUID() + ext;
            Path uploadDir = Paths.get("uploads/avatars");
            try {
                Files.createDirectories(uploadDir);
                Path filePath = uploadDir.resolve(filename);
                avatarFile.transferTo(filePath);
                user.setAvatarPath("/uploads/avatars/" + filename);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Failed to upload avatar.");
                return "redirect:/profile/edit";
            }
        } else if (avatarType == AvatarType.SYSTEM && systemAvatar != null) {
            user.setAvatarPath("/avatars/" + systemAvatar);
        } else {
            user.setAvatarPath(null);
        }
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Profile updated!");
        return "redirect:/profile/" + user.getUsername();
    }
} 