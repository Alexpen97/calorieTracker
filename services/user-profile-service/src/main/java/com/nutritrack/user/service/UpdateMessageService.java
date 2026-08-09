package com.nutritrack.user.service;

import com.nutritrack.user.domain.AppUser;
import com.nutritrack.user.domain.AppUserRepository;
import com.nutritrack.user.domain.UpdateMessage;
import com.nutritrack.user.domain.UpdateMessageAck;
import com.nutritrack.user.domain.UpdateMessageAckId;
import com.nutritrack.user.domain.UpdateMessageAckRepository;
import com.nutritrack.user.domain.UpdateMessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UpdateMessageService {

  private final UpdateMessageRepository messageRepository;
  private final UpdateMessageAckRepository ackRepository;
  private final AppUserRepository userRepository;

  public UpdateMessageService(
      UpdateMessageRepository messageRepository,
      UpdateMessageAckRepository ackRepository,
      AppUserRepository userRepository) {
    this.messageRepository = messageRepository;
    this.ackRepository = ackRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public UpdateMessage push(
      String title, String body, String imageUrl, String actionLabel, String actionUrl) {
    Instant now = Instant.now();
    UpdateMessage message = new UpdateMessage();
    message.setId(UUID.randomUUID());
    message.setTitle(title);
    message.setBody(body);
    message.setImageUrl(blankToNull(imageUrl));
    message.setActionLabel(blankToNull(actionLabel));
    message.setActionUrl(blankToNull(actionUrl));
    message.setPushedAt(now);
    message.setCreatedAt(now);
    return messageRepository.save(message);
  }

  @Transactional(readOnly = true)
  public Optional<UpdateMessage> findPendingForUser(UUID userId) {
    requireUser(userId);
    List<UpdateMessage> pending =
        messageRepository.findPendingForUser(userId, PageRequest.of(0, 1));
    return pending.stream().findFirst();
  }

  @Transactional
  public void acknowledge(UUID userId, UUID messageId) {
    requireUser(userId);
    if (!messageRepository.existsById(messageId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Update message not found");
    }
    UpdateMessageAckId ackId = new UpdateMessageAckId(userId, messageId);
    if (ackRepository.existsById(ackId)) {
      return;
    }
    UpdateMessageAck ack = new UpdateMessageAck();
    ack.setId(ackId);
    ack.setAcknowledgedAt(Instant.now());
    ackRepository.save(ack);
  }

  private AppUser requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }
}
