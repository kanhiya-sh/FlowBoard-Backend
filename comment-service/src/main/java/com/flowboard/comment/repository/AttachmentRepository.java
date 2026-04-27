package com.flowboard.comment.repository;

import com.flowboard.comment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByCardIdOrderByUploadedAtDesc(Long cardId);

    Optional<Attachment> findByAttachmentId(Long attachmentId);

    long countByCardId(Long cardId);

    void deleteByAttachmentId(Long attachmentId);
}
