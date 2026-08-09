package com.nutritrack.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "update_message_ack")
public class UpdateMessageAck {

  @EmbeddedId private UpdateMessageAckId id;

  @Column(name = "acknowledged_at", nullable = false)
  private Instant acknowledgedAt;

  public UpdateMessageAckId getId() {
    return id;
  }

  public void setId(UpdateMessageAckId id) {
    this.id = id;
  }

  public Instant getAcknowledgedAt() {
    return acknowledgedAt;
  }

  public void setAcknowledgedAt(Instant acknowledgedAt) {
    this.acknowledgedAt = acknowledgedAt;
  }
}
