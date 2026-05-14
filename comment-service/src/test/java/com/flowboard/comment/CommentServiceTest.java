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

    // ─── resolveUserId branches ───────────────────────────────────────────────

    @Test
    void addComment_authReturnsNull_throwsUnauthorized() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Hi");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(null);

        assertThrows(com.flowboard.comment.exception.UnauthorizedException.class,
                () -> commentService.addComment(req, "x@y.com"));
    }

    @Test
    void addComment_authFeignNotFound_throwsUnauthorized() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Hi");
        feign.FeignException.NotFound nf = mock(feign.FeignException.NotFound.class);
        when(authServiceClient.getUserByEmail(anyString())).thenThrow(nf);

        assertThrows(com.flowboard.comment.exception.UnauthorizedException.class,
                () -> commentService.addComment(req, "x@y.com"));
    }

    @Test
    void addComment_authGenericException_throwsIllegalState() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Hi");
        when(authServiceClient.getUserByEmail(anyString()))
                .thenThrow(new RuntimeException("net"));

        assertThrows(IllegalStateException.class,
                () -> commentService.addComment(req, "x@y.com"));
    }

    // ─── verifyCard branches ──────────────────────────────────────────────────

    @Test
    void addComment_cardNull_throwsResourceNotFound() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(99L); req.setContent("Hi");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(cardServiceClient.getCardById(99L)).thenReturn(null);

        assertThrows(com.flowboard.comment.exception.ResourceNotFoundException.class,
                () -> commentService.addComment(req, "x@y.com"));
    }

    @Test
    void addComment_cardFeignNotFound_throwsResourceNotFound() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(99L); req.setContent("Hi");
        feign.FeignException.NotFound nf = mock(feign.FeignException.NotFound.class);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(cardServiceClient.getCardById(99L)).thenThrow(nf);

        assertThrows(com.flowboard.comment.exception.ResourceNotFoundException.class,
                () -> commentService.addComment(req, "x@y.com"));
    }

    @Test
    void addComment_cardServiceError_throwsIllegalState() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(99L); req.setContent("Hi");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(cardServiceClient.getCardById(99L)).thenThrow(new RuntimeException("down"));

        assertThrows(IllegalStateException.class,
                () -> commentService.addComment(req, "x@y.com"));
    }

    // ─── addComment with parent (replies) ─────────────────────────────────────

    @Test
    void addComment_validReply_publishesNotification() {
        Comment parent = Comment.builder().commentId(2L).cardId(1L).authorId(99L).isDeleted(false).build();
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Reply"); req.setParentCommentId(2L);

        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        when(commentRepository.findByCommentIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        commentService.addComment(req, "x@y.com");
        verify(notificationPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void addComment_replyParentDifferentCard_throws() {
        Comment parent = Comment.builder().commentId(2L).cardId(99L).authorId(2L).isDeleted(false).build();
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Reply"); req.setParentCommentId(2L);

        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        when(commentRepository.findByCommentIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(parent));

        assertThrows(IllegalArgumentException.class,
                () -> commentService.addComment(req, "x@y.com"));
    }

    @Test
    void addComment_replyParentNotFound_throws() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Reply"); req.setParentCommentId(99L);

        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        when(commentRepository.findByCommentIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.comment.exception.ResourceNotFoundException.class,
                () -> commentService.addComment(req, "x@y.com"));
    }

    @Test
    void addComment_withMention_publishesMentionNotification() {
        UserResponseDTO mentioned = new UserResponseDTO();
        mentioned.setUserId(42L);
        mentioned.setUsername("bob");

        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Hello @bob check this");

        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(authServiceClient.getUserByUsername("bob")).thenReturn(mentioned);
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        commentService.addComment(req, "x@y.com");
        verify(notificationPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void addComment_mentionAuthorIsSelf_skips() {
        UserResponseDTO mentioned = new UserResponseDTO();
        mentioned.setUserId(1L); // same as testUser
        mentioned.setUsername("alice");

        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Note to @alice");

        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(authServiceClient.getUserByUsername("alice")).thenReturn(mentioned);
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        commentService.addComment(req, "x@y.com");
        verify(notificationPublisher, never()).publish(any());
    }

    @Test
    void addComment_mentionLookupFails_swallowed() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setCardId(1L); req.setContent("Hello @ghost");

        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(authServiceClient.getUserByUsername("ghost")).thenThrow(new RuntimeException("nope"));
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // should NOT throw
        commentService.addComment(req, "x@y.com");
    }

    // ─── getReplies / getCommentById ──────────────────────────────────────────

    @Test
    void getReplies_returnsList() {
        when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(testComment));
        when(commentRepository.countByParentCommentIdAndIsDeletedFalse(anyLong())).thenReturn(0);

        List<CommentResponseDTO> result = commentService.getReplies(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getCommentById_success() {
        when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testComment));

        CommentResponseDTO result = commentService.getCommentById(1L);
        assertEquals(1L, result.getCommentId());
    }

    // ─── updateComment ────────────────────────────────────────────────────────

    @Test
    void updateComment_success() {
        CommentUpdateDTO dto = new CommentUpdateDTO();
        dto.setContent("Updated");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResponseDTO result = commentService.updateComment(1L, dto, "x@y.com");
        assertEquals("Updated", result.getContent());
    }

    @Test
    void updateComment_notAuthor_throwsUnauthorized() {
        CommentUpdateDTO dto = new CommentUpdateDTO();
        dto.setContent("X");
        UserResponseDTO other = new UserResponseDTO();
        other.setUserId(99L);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(other);
        when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testComment));

        assertThrows(com.flowboard.comment.exception.UnauthorizedException.class,
                () -> commentService.updateComment(1L, dto, "x@y.com"));
    }

    // ─── deleteComment ────────────────────────────────────────────────────────

    @Test
    void deleteComment_notAuthor_throws() {
        UserResponseDTO other = new UserResponseDTO();
        other.setUserId(99L);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(other);
        when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testComment));

        assertThrows(com.flowboard.comment.exception.UnauthorizedException.class,
                () -> commentService.deleteComment(1L, "x@y.com"));
    }

    @Test
    void deleteComment_notFound_throws() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.comment.exception.ResourceNotFoundException.class,
                () -> commentService.deleteComment(1L, "x@y.com"));
    }

    // ─── Attachments ──────────────────────────────────────────────────────────

    @Test
    void addAttachment_success() {
        AttachmentRequestDTO req = new AttachmentRequestDTO();
        req.setCardId(1L); req.setFileName("a.txt");
        req.setFileUrl("http://x"); req.setFileType("txt"); req.setSizeKb(10L);

        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        com.flowboard.comment.entity.Attachment att = com.flowboard.comment.entity.Attachment.builder()
                .attachmentId(5L).cardId(1L).uploaderId(1L)
                .fileName("a.txt").fileUrl("http://x").fileType("txt").sizeKb(10L).build();
        when(attachmentRepository.save(any())).thenReturn(att);

        AttachmentResponseDTO result = commentService.addAttachment(req, "x@y.com");
        assertNotNull(result);
        verify(notificationPublisher).publish(any());
    }

    @Test
    void getAttachmentsByCard_returnsList() {
        com.flowboard.comment.entity.Attachment att = com.flowboard.comment.entity.Attachment.builder()
                .attachmentId(5L).cardId(1L).uploaderId(1L).fileName("a").fileUrl("u").fileType("t").sizeKb(1L).build();
        when(cardServiceClient.getCardById(1L)).thenReturn(testCard);
        when(attachmentRepository.findByCardIdOrderByUploadedAtDesc(1L)).thenReturn(List.of(att));

        assertEquals(1, commentService.getAttachmentsByCard(1L).size());
    }

    @Test
    void deleteAttachment_success() {
        com.flowboard.comment.entity.Attachment att = com.flowboard.comment.entity.Attachment.builder()
                .attachmentId(5L).cardId(1L).uploaderId(1L).fileName("a").fileUrl("u").fileType("t").sizeKb(1L).build();
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(attachmentRepository.findByAttachmentId(5L)).thenReturn(Optional.of(att));

        commentService.deleteAttachment(5L, "x@y.com");
        verify(attachmentRepository).delete(att);
    }

    @Test
    void deleteAttachment_notUploader_throws() {
        com.flowboard.comment.entity.Attachment att = com.flowboard.comment.entity.Attachment.builder()
                .attachmentId(5L).cardId(1L).uploaderId(99L).fileName("a").fileUrl("u").fileType("t").sizeKb(1L).build();
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(attachmentRepository.findByAttachmentId(5L)).thenReturn(Optional.of(att));

        assertThrows(com.flowboard.comment.exception.UnauthorizedException.class,
                () -> commentService.deleteAttachment(5L, "x@y.com"));
    }

    @Test
    void deleteAttachment_notFound_throws() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(attachmentRepository.findByAttachmentId(5L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.comment.exception.ResourceNotFoundException.class,
                () -> commentService.deleteAttachment(5L, "x@y.com"));
    }
}
