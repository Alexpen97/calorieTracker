package com.nutritrack.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UpdateMessageAckId implements Serializable {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "message_id", nullable = false)
  private UUID messageId;

  public UpdateMessageAckId() {}

  public UpdateMessageAckId(UUID userId, UUID messageId) {
    this.userId = userId;
    this.messageId = messageId;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getMessageId() {
    return messageId;
  }

  public void setMessageId(UUID messageId) {
    this.messageId = messageId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UpdateMessageAckId that)) {
      return false;
    }
    return Objects.equals(userId, that.userId) && Objects.equals(messageId, that.messageId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, messageId);
  }
}
