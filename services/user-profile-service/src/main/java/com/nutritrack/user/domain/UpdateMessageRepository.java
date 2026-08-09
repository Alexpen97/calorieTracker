package com.nutritrack.user.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UpdateMessageRepository extends JpaRepository<UpdateMessage, UUID> {

  @Query(
      """
      select m from UpdateMessage m
      where not exists (
        select 1 from UpdateMessageAck a
        where a.id.messageId = m.id and a.id.userId = :userId
      )
      order by m.pushedAt asc
      """)
  List<UpdateMessage> findPendingForUser(@Param("userId") UUID userId, Pageable pageable);
}
