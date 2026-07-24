package com.nutritrack.nevo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "nevo_alias")
public class NevoAlias {

  @Id private UUID id;

  @Column(name = "alias_term", nullable = false)
  private String aliasTerm;

  @Column(name = "canonical_term", nullable = false)
  private String canonicalTerm;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getAliasTerm() {
    return aliasTerm;
  }

  public void setAliasTerm(String aliasTerm) {
    this.aliasTerm = aliasTerm;
  }

  public String getCanonicalTerm() {
    return canonicalTerm;
  }

  public void setCanonicalTerm(String canonicalTerm) {
    this.canonicalTerm = canonicalTerm;
  }
}
