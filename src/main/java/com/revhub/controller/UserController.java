package com.revhub.controller;
import com.revhub.model.User;
import com.revhub.service.BucketService;
import com.revhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {
    private final UserService userService;
    private final BucketService bucketService;
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(user);
    }
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    @PutMapping("/me")
    public ResponseEntity<User> updateProfile(Authentication authentication, @RequestBody User updateRequest) {
        User updatedUser = userService.updateProfile(authentication, updateRequest);
        return ResponseEntity.ok(updatedUser);
    }
    @PostMapping("/me/profile-picture")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            if (!file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }
            // Upload to S3
            String fileUrl = bucketService.uploadFile(file, bucketName);
            // Update user profile
            User user = userService.getCurrentUser(authentication);
            user.setProfilePicture(fileUrl);
            userService.updateProfile(authentication, user);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/me/cover-photo")
    public ResponseEntity<Map<String, String>> uploadCoverPhoto(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            if (!file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }
            // Upload to S3
            String fileUrl = bucketService.uploadFile(file, bucketName);
            // Update user cover photo
            User user = userService.getCurrentUser(authentication);
            user.setCoverPhoto(fileUrl);
            userService.updateProfile(authentication, user);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String q) {
        List<User> users = userService.searchUsers(q);
        return ResponseEntity.ok(users);
    }
    @GetMapping("/suggested")
    public ResponseEntity<List<User>> getSuggestedUsers(Authentication authentication) {
        List<User> users = userService.getSuggestedUsers(authentication);
        return ResponseEntity.ok(users);
    }
}
