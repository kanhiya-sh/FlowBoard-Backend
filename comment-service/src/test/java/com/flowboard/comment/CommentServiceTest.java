package com.flowboard.comment;

import com.flowboard.comment.config.AuthServiceClient;
import com.flowboard.comment.config.CardServiceClient;
import com.flowboard.comment.dto.*;
import com.flowboard.comment.entity.Comment;
import com.flowboard.comment.messaging.NotificationPublisher;
import com.flowboard.comment.repository.AttachmentRepository;
import com.flowboard.comment.repository.CommentRepository;
import com.flowboard.comment.serviceImpl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private CardServiceClient cardServiceClient;
    @Mock private NotificationPublisher notificationPublisher;
    @InjectMocks private CommentServiceImpl commentService;

    private Comment testComment;
    private UserResponseDTO testUser;
    private CardResponseDTO testCard;

    @BeforeEach
    void setUp() {
        testComment = new Comment();
        testComment.setCommentId(1L);
        testComment.setCardId(1L);
        testComment.setAuthorId(1L);
        testComment.setContent("Test comment");
        testComment.setIsDeleted(false);

        testUser = new UserResponseDTO();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");

        testCard = new CardResponseDTO();
        testCard.setCardId(1L);
    }

    @Test
    void getByCard_returnsList() {
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        when(commentRepository
            .findByCardIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(1L))
            .thenReturn(List.of(testComment));

        List<CommentResponseDTO> result = commentService.getByCard(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getCommentById_notFound_throws() {
        when(commentRepository.findByCommentIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> commentService.getCommentById(99L));
    }

    @Test
    void addComment_savesAndReturns() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Hello");

        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        CommentResponseDTO result = commentService.addComment(req, "author@test.com");
        assertNotNull(result);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void deleteComment_authorCanDelete() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(commentRepository.findByCommentIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(testComment));
        commentService.deleteComment(1L, "author@test.com");
        verify(commentRepository).save(any());
    }

    @Test
    void getCommentCount_returnsCount() {
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        when(commentRepository.countByCardIdAndIsDeletedFalse(1L)).thenReturn(1L);
        long count = commentService.getCommentCount(1L);
        assertEquals(1L, count);
    }
}
