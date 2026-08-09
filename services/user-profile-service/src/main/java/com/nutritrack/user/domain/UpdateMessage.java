package com.nutritrack.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "update_message")
public class UpdateMessage {

  @Id private UUID id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String body;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "action_label")
  private String actionLabel;

  @Column(name = "action_url")
  private String actionUrl;

  @Column(name = "pushed_at", nullable = false)
  private Instant pushedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getActionLabel() {
    return actionLabel;
  }

  public void setActionLabel(String actionLabel) {
    this.actionLabel = actionLabel;
  }

  public String getActionUrl() {
    return actionUrl;
  }

  public void setActionUrl(String actionUrl) {
    this.actionUrl = actionUrl;
  }

  public Instant getPushedAt() {
    return pushedAt;
  }

  public void setPushedAt(Instant pushedAt) {
    this.pushedAt = pushedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
