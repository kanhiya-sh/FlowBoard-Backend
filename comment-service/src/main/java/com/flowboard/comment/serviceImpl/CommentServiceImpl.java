package com.flowboard.comment.serviceImpl;

import com.flowboard.comment.config.AuthServiceClient;
import com.flowboard.comment.config.CardServiceClient;
import com.flowboard.comment.dto.*;
import com.flowboard.comment.entity.Attachment;
import com.flowboard.comment.entity.Comment;
import com.flowboard.comment.exception.ResourceNotFoundException;
import com.flowboard.comment.exception.UnauthorizedException;
import com.flowboard.comment.mapper.CommentMapper;
import com.flowboard.comment.messaging.NotificationPublisher;
import com.flowboard.comment.repository.AttachmentRepository;
import com.flowboard.comment.repository.CommentRepository;
import com.flowboard.comment.service.CommentService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final AuthServiceClient authServiceClient;
    private final CardServiceClient cardServiceClient;
    private final NotificationPublisher notificationPublisher;

    // Internal helpers
    private Long resolveUserId(String email) {
        try {
            UserResponseDTO user = authServiceClient.getUserByEmail(email);
            if (user == null || user.getUserId() == null) {
                throw new UnauthorizedException("Could not resolve user from Auth service");
            }
            return user.getUserId();
        }
        catch (FeignException.NotFound e) {
            throw new UnauthorizedException("User not found: " + email);
        }
        catch (UnauthorizedException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Auth service error for email {}: {}", email, e.getMessage());
            throw new IllegalStateException("Auth service is currently unavailable.");
        }
    }

    private CardResponseDTO verifyCard(Long cardId) {
        try {
            CardResponseDTO card = cardServiceClient.getCardById(cardId);
            if (card == null) {
                throw new ResourceNotFoundException("Card not found with id: " + cardId);
            }
            return card;
        }
        catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Card not found with id: " + cardId);
        }
        catch (ResourceNotFoundException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Card service error for cardId {}: {}", cardId, e.getMessage());
            throw new IllegalStateException("Card service is currently unavailable.");
        }
    }

    private Comment findActiveComment(Long commentId) {
        return commentRepository
                .findByCommentIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
    }

    private void publishMentionNotifications(Long authorId, Long cardId, String content) {
        Pattern pattern = Pattern.compile("@(\\w+)");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            try {
                // Use email lookup proxy — auth-service exposes /internal/users/username/{username}
                // If that endpoint does not yet exist, this call is safely swallowed below.
                UserResponseDTO mentioned = authServiceClient.getUserByUsername(username);
                if (mentioned != null && mentioned.getUserId() != null
                        && !mentioned.getUserId().equals(authorId)) {

                    notificationPublisher.publish(NotificationEventDTO.builder()
                            .actorId(authorId)
                            .recipientId(mentioned.getUserId())
                            .type("MENTION")
                            .title("You were mentioned in a comment")
                            .message("@" + username + " was mentioned on card #" + cardId)
                            .relatedId(cardId)
                            .relatedType("CARD")
                            .deepLinkUrl("/cards/" + cardId)
                            .build());
                }
            }
            catch (Exception e) {
                log.warn("Could not resolve mention for @{}: {}", username, e.getMessage());
            }
        }
    }

    private void publishCommentNotification(Long actorId, Long cardId, Long parentAuthorId, String type) {
        if (parentAuthorId != null && !parentAuthorId.equals(actorId)) {
            notificationPublisher.publish(NotificationEventDTO.builder()
                    .actorId(actorId)
                    .recipientId(parentAuthorId)
                    .type(type)
                    .title("New reply on your comment")
                    .message("Someone replied to your comment on card #" + cardId)
                    .relatedId(cardId)
                    .relatedType("CARD")
                    .deepLinkUrl("/cards/" + cardId)
                    .build());
        }
    }

    // Comment CRUD

    @Override
    @Transactional
    public CommentResponseDTO addComment(CommentRequestDTO dto, String userEmail) {
        Long authorId = resolveUserId(userEmail);
        verifyCard(dto.getCardId());

        Long parentCommentId = dto.getParentCommentId();
        Comment parent = null;

        // Validate parent exists and belongs to the same card
        if (parentCommentId != null) {
            parent = findActiveComment(parentCommentId);

            if (!parent.getCardId().equals(dto.getCardId())) {
                throw new IllegalArgumentException("Parent comment does not belong to card " + dto.getCardId());
            }

            if (parent.getIsDeleted()) {
                throw new IllegalStateException("Cannot reply to a deleted comment");
            }
        }

        Comment comment = Comment.builder()
                .cardId(dto.getCardId())
                .authorId(authorId)
                .content(dto.getContent())
                .parentCommentId(parentCommentId)
                .isDeleted(false)
                .build();

        Comment saved = commentRepository.save(comment);

        log.info("Comment added: commentId={} cardId={} authorId={} isReply={}",
                saved.getCommentId(), dto.getCardId(), authorId, parentCommentId != null);

        // Notify parent comment author (NO extra DB call)
        if (parent != null) {
            publishCommentNotification(authorId, dto.getCardId(), parent.getAuthorId(), "COMMENT");
        }

        // Notify @mentioned users
        publishMentionNotifications(authorId, dto.getCardId(), dto.getContent());

        int replyCount = commentRepository
                .findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(saved.getCommentId())
                .size();

        return CommentMapper.toResponseDTO(saved, replyCount);
    }

    @Override
    public List<CommentResponseDTO> getByCard(Long cardId) {
        verifyCard(cardId);
        List<Comment> topLevel = commentRepository
                .findByCardIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(cardId);

        return topLevel.stream().map(c -> {
            int replyCount = commentRepository
                    .findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(c.getCommentId()).size();
            return CommentMapper.toResponseDTO(c, replyCount);
        }).toList();
    }

    @Override
    public CommentResponseDTO getCommentById(Long commentId) {
        Comment comment = findActiveComment(commentId);
        int replyCount = commentRepository
                .findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(commentId).size();
        return CommentMapper.toResponseDTO(comment, replyCount);
    }

    @Override
    public List<CommentResponseDTO> getReplies(Long parentCommentId) {
        // check parent comment exists
        findActiveComment(parentCommentId);

        return commentRepository
                .findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentCommentId)
                .stream()
                .map(comment -> {
                    int replyCount = commentRepository.countByParentCommentIdAndIsDeletedFalse(comment.getCommentId());
                    return CommentMapper.toResponseDTO(comment, replyCount);
                })
                .toList();
    }

    @Override
    @Transactional
    public CommentResponseDTO updateComment(Long commentId, CommentUpdateDTO dto, String userEmail) {
        Long userId = resolveUserId(userEmail);
        Comment comment = findActiveComment(commentId);

        if (comment.getIsDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted comment");
        }
        if (!comment.getAuthorId().equals(userId)) {
            throw new UnauthorizedException("You can only edit your own comments");
        }

        comment.setContent(dto.getContent());
        Comment saved = commentRepository.save(comment);
        log.info("Comment updated: commentId={} by userId={}", commentId, userId);

        // Re-notify on new mentions after edit
        publishMentionNotifications(userId, comment.getCardId(), dto.getContent());

        int replyCount = commentRepository.countByParentCommentIdAndIsDeletedFalse(saved.getCommentId());
        return CommentMapper.toResponseDTO(saved, replyCount);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String userEmail) {
        Long userId = resolveUserId(userEmail);
        Comment comment = findActiveComment(commentId);

        if (comment.getIsDeleted()) {
            throw new IllegalStateException("Comment is already deleted");
        }
        if (!comment.getAuthorId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own comments");
        }

        // Soft-delete: preserve for moderation history
        comment.setIsDeleted(true);
        commentRepository.save(comment);
        log.info("Comment soft-deleted: commentId={} by userId={}", commentId, userId);
    }

    @Override
    public long getCommentCount(Long cardId) {
        verifyCard(cardId);
        return commentRepository.countByCardIdAndIsDeletedFalse(cardId);
    }

    // Attachment CRUD

    @Override
    @Transactional
    public AttachmentResponseDTO addAttachment(AttachmentRequestDTO dto, String userEmail) {
        Long uploaderId = resolveUserId(userEmail);
        verifyCard(dto.getCardId());

        Attachment attachment = Attachment.builder()
                .cardId(dto.getCardId())
                .uploaderId(uploaderId)
                .fileName(dto.getFileName())
                .fileUrl(dto.getFileUrl())
                .fileType(dto.getFileType())
                .sizeKb(dto.getSizeKb())
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        log.info("Attachment added: attachmentId={} cardId={} uploaderId={} file={}",
                saved.getAttachmentId(), dto.getCardId(), uploaderId, dto.getFileName());

        // Notify card watchers (notification-service picks this up from the queue)
        notificationPublisher.publish(NotificationEventDTO.builder()
                .actorId(uploaderId)
                .recipientId(null)          // notification-service resolves card watchers
                .type("ATTACHMENT")
                .title("New attachment on card")
                .message(dto.getFileName() + " was attached to card #" + dto.getCardId())
                .relatedId(dto.getCardId())
                .relatedType("CARD")
                .deepLinkUrl("/cards/" + dto.getCardId())
                .build());

        return CommentMapper.toResponseDTO(saved);
    }

    @Override
    public List<AttachmentResponseDTO> getAttachmentsByCard(Long cardId) {
        verifyCard(cardId);
        return attachmentRepository.findByCardIdOrderByUploadedAtDesc(cardId)
                .stream()
                .map(CommentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, String userEmail) {
        Long userId = resolveUserId(userEmail);
        Attachment attachment = attachmentRepository.findByAttachmentId(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + attachmentId));

        if (!attachment.getUploaderId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own attachments");
        }

        attachmentRepository.delete(attachment);
        log.info("Attachment deleted: attachmentId={} by userId={}", attachmentId, userId);
    }
}
