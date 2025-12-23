package com.revhub.controller;
import com.revhub.dto.MessageResponse;
import com.revhub.model.Connection;
import com.revhub.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ConnectionController {
    private final ConnectionService connectionService;
    @PostMapping("/request/{userId}")
    public ResponseEntity<Connection> sendConnectionRequest(Authentication authentication, @PathVariable Long userId) {
        Connection connection = connectionService.sendConnectionRequest(authentication, userId);
        return ResponseEntity.ok(connection);
    }
    @PostMapping("/accept/{connectionId}")
    public ResponseEntity<Connection> acceptConnectionRequest(Authentication authentication,
            @PathVariable Long connectionId) {
        Connection connection = connectionService.acceptConnectionRequest(authentication, connectionId);
        return ResponseEntity.ok(connection);
    }
    @PostMapping("/reject/{connectionId}")
    public ResponseEntity<MessageResponse> rejectConnectionRequest(Authentication authentication,
            @PathVariable Long connectionId) {
        connectionService.rejectConnectionRequest(authentication, connectionId);
        return ResponseEntity.ok(new MessageResponse("Connection request rejected"));
    }
    @GetMapping
    public ResponseEntity<List<Connection>> getConnections(Authentication authentication) {
        List<Connection> connections = connectionService.getConnections(authentication);
        return ResponseEntity.ok(connections);
    }
    @GetMapping("/pending")
    public ResponseEntity<List<Connection>> getPendingRequests(Authentication authentication) {
        List<Connection> requests = connectionService.getPendingRequests(authentication);
        return ResponseEntity.ok(requests);
    }
    @DeleteMapping("/{connectionId}")
    public ResponseEntity<MessageResponse> removeConnection(Authentication authentication,
            @PathVariable Long connectionId) {
        connectionService.removeConnection(authentication, connectionId);
        return ResponseEntity.ok(new MessageResponse("Connection removed successfully"));
    }
    @GetMapping("/followers/count/{userId}")
    public ResponseEntity<Long> getFollowersCount(@PathVariable Long userId) {
        long count = connectionService.getFollowersCount(userId);
        return ResponseEntity.ok(count);
    }
    @GetMapping("/following/count/{userId}")
    public ResponseEntity<Long> getFollowingCount(@PathVariable Long userId) {
        long count = connectionService.getFollowingCount(userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<Boolean> getConnectionStatus(Authentication authentication, @PathVariable Long userId) {
        boolean isConnected = connectionService.getConnectionStatus(authentication, userId);
        return ResponseEntity.ok(isConnected);
    }
}
