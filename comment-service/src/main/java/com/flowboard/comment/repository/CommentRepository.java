package com.flowboard.comment.repository;

import com.flowboard.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // All non-deleted top-level comments for a card, ordered by creation time
    List<Comment> findByCardIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(Long cardId);

    // All comments (including soft-deleted) for a card — used by moderation / count
    List<Comment> findByCardId(Long cardId);

    // All comments by a specific author
    List<Comment> findByAuthorId(Long authorId);

    // Fetch a single comment (regardless of deleted state — business logic decides)
    Optional<Comment> findByCommentId(Long commentId);

    // Replies to a parent comment (non-deleted), ordered chronologically
    List<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentCommentId);

    // Count active (non-deleted) comments on a card
    long countByCardIdAndIsDeletedFalse(Long cardId);

    // Exists check for internal use
    boolean existsByCommentIdAndIsDeletedFalse(Long commentId);

    // Used for hard-delete (admin purge)
    void deleteByCommentId(Long commentId);

    int countByParentCommentIdAndIsDeletedFalse(Long parentCommentId);

    Optional<Comment> findByCommentIdAndIsDeletedFalse(Long commentId);
}
