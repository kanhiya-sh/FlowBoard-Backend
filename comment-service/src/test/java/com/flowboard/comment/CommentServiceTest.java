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

    @BeforeEach
    void setUp() {
        testComment = new Comment();
        testComment.setCommentId(1L);
        testComment.setCardId(1L);
        testComment.setAuthorId(1L);
        testComment.setContent("Test comment");
        testComment.setIsDeleted(false);
    }

    @Test
    void getByCard_returnsList() {
        when(commentRepository
            .findByCardIdAndParentCommentIdIsNullAndIsDeletedFalseOrderByCreatedAtAsc(1L))
            .thenReturn(List.of(testComment));

        List<CommentResponseDTO> result = commentService.getByCard(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getCommentById_notFound_throws() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> commentService.getCommentById(99L));
    }

    @Test
    void addComment_savesAndReturns() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Hello");

        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(1L); user.setUsername("testuser");

        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        CommentResponseDTO result = commentService.addComment(req, "author@test.com");
        assertNotNull(result);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void deleteComment_authorCanDelete() {
        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(1L);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testComment));
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user);
        commentService.deleteComment(1L, "author@test.com");
        verify(commentRepository).save(any());
    }

    @Test
    void getCommentCount_returnsCount() {
        when(commentRepository.findByCardId(1L)).thenReturn(List.of(testComment));
        long count = commentService.getCommentCount(1L);
        assertEquals(1L, count);
    }
}
