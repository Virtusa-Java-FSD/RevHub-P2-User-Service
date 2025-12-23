package com.revhub.service;

import com.revhub.exception.BadRequestException;
import com.revhub.exception.ResourceNotFoundException;
import com.revhub.model.Connection;
import com.revhub.model.User;
import com.revhub.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConnectionService {
    private final ConnectionRepository connectionRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public Connection sendConnectionRequest(Authentication authentication, Long receiverId) {
        User requester = userService.getCurrentUser(authentication);
        if (requester.getId().equals(receiverId)) {
            throw new BadRequestException("Cannot send connection request to yourself");
        }
        // Check if connection already exists in EITHER direction
        // This prevents duplicate connections and handles bidirectional following
        List<Connection> existingConnections = connectionRepository
                .findConnectionBetweenUsers(requester.getId(), receiverId);

        if (!existingConnections.isEmpty()) {
            // Get the most recent connection (first in list due to ORDER BY createdAt DESC)
            Connection existing = existingConnections.get(0);
            if (existing.getStatus() == Connection.ConnectionStatus.PENDING) {
                throw new BadRequestException("Connection request already pending");
            } else if (existing.getStatus() == Connection.ConnectionStatus.ACCEPTED) {
                throw new BadRequestException("Already connected");
            }
            // If REJECTED, we can allow a new request (fall through)
        }
        // Verify receiver exists
        User receiver = userService.getUserById(receiverId);

        Connection connection = new Connection();
        connection.setRequesterId(requester.getId());
        connection.setReceiverId(receiverId);
        connection.setStatus(Connection.ConnectionStatus.PENDING); // Require approval for follow requests
                                                                    // User must accept before connection is established
        Connection saved = connectionRepository.save(connection);

        // Create notification for the receiver
        try {
            notificationService.createNotification(
                    com.revhub.model.Notification.NotificationType.FOLLOWER,
                    receiverId,
                    requester.getId(),
                    requester.getFirstName() + " " + requester.getLastName() + " sent you a follow request",
                    saved.getId().toString());
        } catch (Exception e) {
            // Log the error but don't fail the connection request
            System.err.println("Failed to create notification: " + e.getMessage());
        }

        return saved;
    }

    @Transactional
    public Connection acceptConnectionRequest(Authentication authentication, Long connectionId) {
        User user = userService.getCurrentUser(authentication);
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));
        if (!connection.getReceiverId().equals(user.getId())) {
            throw new RuntimeException("You can only accept connection requests sent to you");
        }
        connection.setStatus(Connection.ConnectionStatus.ACCEPTED);
        connection.setUpdatedAt(LocalDateTime.now());
        Connection saved = connectionRepository.save(connection);
        // Create notification for the requester
        notificationService.createNotification(
                com.revhub.model.Notification.NotificationType.FOLLOWER,
                connection.getRequesterId(),
                user.getId(),
                user.getFirstName() + " " + user.getLastName() + " accepted your connection request",
                connectionId.toString());
        return saved;
    }

    @Transactional
    public void rejectConnectionRequest(Authentication authentication, Long connectionId) {
        User user = userService.getCurrentUser(authentication);
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));
        if (!connection.getReceiverId().equals(user.getId())) {
            throw new RuntimeException("You can only reject connection requests sent to you");
        }
        connectionRepository.delete(connection);
    }

    public List<Connection> getConnections(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        return connectionRepository.findByUserIdAndStatus(user.getId(), Connection.ConnectionStatus.ACCEPTED);
    }

    public List<Connection> getPendingRequests(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        return connectionRepository.findByReceiverIdAndStatus(user.getId(), Connection.ConnectionStatus.PENDING);
    }

    @Transactional
    public void removeConnection(Authentication authentication, Long connectionId) {
        User user = userService.getCurrentUser(authentication);
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));
        if (!connection.getRequesterId().equals(user.getId()) &&
                !connection.getReceiverId().equals(user.getId())) {
            throw new RuntimeException("You can only remove your own connections");
        }
        connectionRepository.delete(connection);
    }

    public long getFollowersCount(Long userId) {
        // Count accepted connections where the user is the receiver
        return connectionRepository.findByReceiverIdAndStatus(userId, Connection.ConnectionStatus.ACCEPTED).size();
    }

    public long getFollowingCount(Long userId) {
        // Count accepted connections where the user is the requester
        return connectionRepository.findByRequesterIdAndStatus(userId, Connection.ConnectionStatus.ACCEPTED).size();
    }

    public boolean getConnectionStatus(Authentication authentication, Long userId) {
        User currentUser = userService.getCurrentUser(authentication);
        List<Connection> connections = connectionRepository
                .findConnectionBetweenUsers(currentUser.getId(), userId);
        
        if (connections.isEmpty()) {
            return false;
        }
        
        // Check if there's an accepted connection
        return connections.stream()
                .anyMatch(conn -> conn.getStatus() == Connection.ConnectionStatus.ACCEPTED);
    }
}
