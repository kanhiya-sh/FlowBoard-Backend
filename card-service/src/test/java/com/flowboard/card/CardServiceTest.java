package com.flowboard.card;

import com.flowboard.card.client.AuthServiceClient;
import com.flowboard.card.client.BoardServiceClient;
import com.flowboard.card.client.ListServiceClient;
import com.flowboard.card.dto.*;
import com.flowboard.card.entity.Card;
import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import com.flowboard.card.repository.CardRepository;
import com.flowboard.card.service.NotificationPublisher;
import com.flowboard.card.serviceImpl.CardServiceImpl;
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
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private BoardServiceClient boardServiceClient;
    @Mock private ListServiceClient listServiceClient;
    @Mock private NotificationPublisher notificationPublisher;
    @InjectMocks private CardServiceImpl cardService;

    private Card testCard;
    private UserResponseDTO testUser;

    @BeforeEach
    void setUp() {
        testCard = Card.builder()
                .cardId(1L).title("Test Card").listId(1L).boardId(1L)
                .position(0).priority(Priority.MEDIUM).status(Status.TO_DO)
                .isArchived(false)
                .build();

        testUser = new UserResponseDTO();
        testUser.setUserId(10L);
        testUser.setEmail("user@test.com");
    }

    @Test
    void createCard_savesAndReturns() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("New Card"); req.setListId(1L); req.setBoardId(1L);

        ListResponseDTO list = new ListResponseDTO();
        list.setListId(1L); list.setBoardId(1L); list.setIsArchived(false);

        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenReturn(list);
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(true, "MEMBER"));
        when(cardRepository.findMaxPositionByListId(1L)).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        Card result = cardService.createCard(req, "user@test.com");
        assertNotNull(result);
        assertEquals("Test Card", result.getTitle());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void getCardById_notFound_throws() {
        when(cardRepository.findByCardId(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> cardService.getCardById(99L));
    }

    @Test
    void getCardsByList_returnsList() {
        when(cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(1L))
                .thenReturn(List.of(testCard));
        List<Card> result = cardService.getCardsByList(1L);
        assertEquals(1, result.size());
        assertEquals("Test Card", result.get(0).getTitle());
    }

    @Test
    void deleteCard_archived_callsRepository() {
        Card archived = Card.builder()
                .cardId(1L).title("Test Card").listId(1L).boardId(1L)
                .position(0).priority(Priority.MEDIUM).status(Status.TO_DO)
                .isArchived(true)
                .build();

        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(archived));
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(true, "MEMBER"));

        cardService.deleteCard(1L, "user@test.com");
        verify(cardRepository).delete(archived);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private ListResponseDTO list(boolean archived) {
        ListResponseDTO l = new ListResponseDTO();
        l.setListId(1L); l.setBoardId(1L); l.setIsArchived(archived);
        return l;
    }

    private void mockAuthAndMember(String role) {
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(true, role));
    }

    // ─── resolveUserIdFromEmail branches ──────────────────────────────────────

    @Test
    void createCard_authReturnsNull_throwsUnauthorized() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(null);

        assertThrows(com.flowboard.card.exception.UnauthorizedException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    @Test
    void createCard_authFeignNotFound_throwsUnauthorized() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        feign.FeignException.NotFound nf = mock(feign.FeignException.NotFound.class);
        when(authServiceClient.getUserByEmail("user@test.com")).thenThrow(nf);

        assertThrows(com.flowboard.card.exception.UnauthorizedException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    @Test
    void createCard_authFeignError_throwsIllegalState() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        feign.FeignException fe = mock(feign.FeignException.class);
        when(fe.getMessage()).thenReturn("500");
        when(authServiceClient.getUserByEmail("user@test.com")).thenThrow(fe);

        assertThrows(IllegalStateException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    @Test
    void createCard_authGenericException_throwsIllegalState() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com"))
                .thenThrow(new RuntimeException("net"));

        assertThrows(IllegalStateException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    // ─── verifyListExists branches ────────────────────────────────────────────

    @Test
    void createCard_listNull_throwsResourceNotFound() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenReturn(null);

        assertThrows(com.flowboard.card.exception.ResourceNotFoundException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    @Test
    void createCard_listFeignNotFound_throwsResourceNotFound() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        feign.FeignException.NotFound nf = mock(feign.FeignException.NotFound.class);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenThrow(nf);

        assertThrows(com.flowboard.card.exception.ResourceNotFoundException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    @Test
    void createCard_listServiceError_throwsIllegalState() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenThrow(new RuntimeException("down"));

        assertThrows(IllegalStateException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    @Test
    void createCard_archivedList_throws() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenReturn(list(true));

        assertThrows(IllegalStateException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    // ─── Board membership branches ────────────────────────────────────────────

    @Test
    void createCard_notBoardMember_throws() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenReturn(list(false));
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(false, null));

        assertThrows(com.flowboard.card.exception.UnauthorizedException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    @Test
    void createCard_boardServiceReturnsNull_treatedAsNonMember() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenReturn(list(false));
        when(boardServiceClient.checkBoardMembership(1L, 10L)).thenReturn(null);

        assertThrows(com.flowboard.card.exception.UnauthorizedException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    @Test
    void createCard_boardFeignNotFound_treatedAsNonMember() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        feign.FeignException.NotFound nf = mock(feign.FeignException.NotFound.class);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenReturn(list(false));
        when(boardServiceClient.checkBoardMembership(1L, 10L)).thenThrow(nf);

        assertThrows(com.flowboard.card.exception.UnauthorizedException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    @Test
    void createCard_boardFeignError_throwsIllegalState() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        feign.FeignException fe = mock(feign.FeignException.class);
        when(fe.getMessage()).thenReturn("500");
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenReturn(list(false));
        when(boardServiceClient.checkBoardMembership(1L, 10L)).thenThrow(fe);

        assertThrows(IllegalStateException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    @Test
    void createCard_boardGenericException_throwsIllegalState() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenReturn(list(false));
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(IllegalStateException.class,
                () -> cardService.createCard(req, "user@test.com"));
    }

    // ─── createCard - position/defaults ───────────────────────────────────────

    @Test
    void createCard_setsNextPositionFromExisting() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("New"); req.setListId(1L); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(listServiceClient.getListById(1L)).thenReturn(list(false));
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(true, "MEMBER"));
        when(cardRepository.findMaxPositionByListId(1L)).thenReturn(Optional.of(7));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.createCard(req, "user@test.com");
        assertEquals(8, result.getPosition());
        assertEquals(Priority.MEDIUM, result.getPriority());
        assertEquals(Status.TO_DO, result.getStatus());
    }

    // ─── getters ──────────────────────────────────────────────────────────────

    @Test
    void getCardById_success() {
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));
        assertEquals("Test Card", cardService.getCardById(1L).getTitle());
    }

    @Test
    void getCardsByBoard_success() {
        when(cardRepository.findByBoardIdAndIsArchivedFalse(1L)).thenReturn(List.of(testCard));
        assertEquals(1, cardService.getCardsByBoard(1L).size());
    }

    @Test
    void getCardsByAssignee_success() {
        when(cardRepository.findByAssigneeIdAndIsArchivedFalse(5L)).thenReturn(List.of(testCard));
        assertEquals(1, cardService.getCardsByAssignee(5L).size());
    }

    @Test
    void getArchivedCardsByBoard_success() {
        when(cardRepository.findByBoardIdAndIsArchivedTrue(1L)).thenReturn(List.of(testCard));
        assertEquals(1, cardService.getArchivedCardsByBoard(1L).size());
    }

    @Test
    void getArchivedCardsByList_success() {
        when(cardRepository.findByListIdAndIsArchivedTrue(1L)).thenReturn(List.of(testCard));
        assertEquals(1, cardService.getArchivedCardsByList(1L).size());
    }

    @Test
    void getOverdueCards_success() {
        when(cardRepository.findOverdueCards(any())).thenReturn(List.of(testCard));
        assertEquals(1, cardService.getOverdueCards().size());
    }

    @Test
    void getOverdueCardsByBoard_success() {
        when(cardRepository.findOverdueCardsByBoard(eq(1L), any())).thenReturn(List.of(testCard));
        assertEquals(1, cardService.getOverdueCardsByBoard(1L).size());
    }

    @Test
    void getCardsByBoardAndPriority_success() {
        when(cardRepository.findByBoardIdAndPriorityAndIsArchivedFalse(1L, Priority.HIGH))
                .thenReturn(List.of(testCard));
        assertEquals(1, cardService.getCardsByBoardAndPriority(1L, Priority.HIGH).size());
    }

    @Test
    void getCardsByBoardAndStatus_success() {
        when(cardRepository.findByBoardIdAndStatusAndIsArchivedFalse(1L, Status.TO_DO))
                .thenReturn(List.of(testCard));
        assertEquals(1, cardService.getCardsByBoardAndStatus(1L, Status.TO_DO).size());
    }

    // ─── updateCard ───────────────────────────────────────────────────────────

    @Test
    void updateCard_archived_throws() {
        Card archived = Card.builder().cardId(1L).boardId(1L).isArchived(true).build();
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(archived));

        assertThrows(IllegalStateException.class,
                () -> cardService.updateCard(1L, req, "user@test.com"));
    }

    @Test
    void updateCard_changesAssignee_publishesNotification() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("X"); req.setListId(1L); req.setBoardId(1L);
        req.setAssigneeId(42L);
        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.updateCard(1L, req, "user@test.com");
        assertEquals(42L, result.getAssigneeId());
        verify(notificationPublisher).sendAssignmentNotification(eq(10L), eq(42L), eq(1L), anyString());
    }

    @Test
    void updateCard_clearDueDateAndStartDate() {
        Card c = Card.builder().cardId(1L).boardId(1L).listId(1L)
                .dueDate(java.time.LocalDate.now()).startDate(java.time.LocalDate.now())
                .isArchived(false).build();
        CardRequestDTO req = new CardRequestDTO();
        req.setListId(1L); req.setBoardId(1L); req.setTitle("X");
        req.setClearDueDate(true); req.setClearStartDate(true);
        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(c));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.updateCard(1L, req, "user@test.com");
        assertNull(result.getDueDate());
        assertNull(result.getStartDate());
    }

    @Test
    void updateCard_setDueDateAndStartDate() {
        java.time.LocalDate d = java.time.LocalDate.now();
        CardRequestDTO req = new CardRequestDTO();
        req.setListId(1L); req.setBoardId(1L); req.setTitle("X");
        req.setDueDate(d); req.setStartDate(d);
        req.setPriority(Priority.HIGH); req.setStatus(Status.IN_PROGRESS);
        req.setDescription("desc"); req.setCoverColor("#fff");
        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.updateCard(1L, req, "user@test.com");
        assertEquals(d, result.getDueDate());
        assertEquals(d, result.getStartDate());
        assertEquals(Priority.HIGH, result.getPriority());
    }

    @Test
    void updateCard_blankTitle_keepsOld() {
        CardRequestDTO req = new CardRequestDTO();
        req.setListId(1L); req.setBoardId(1L); req.setTitle("   ");
        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.updateCard(1L, req, "user@test.com");
        assertEquals("Test Card", result.getTitle());
    }

    // ─── deleteCard ───────────────────────────────────────────────────────────

    @Test
    void deleteCard_notArchived_throws() {
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));

        assertThrows(IllegalStateException.class,
                () -> cardService.deleteCard(1L, "user@test.com"));
    }

    // ─── moveCard ─────────────────────────────────────────────────────────────

    @Test
    void moveCard_archivedCard_throws() {
        Card archived = Card.builder().cardId(1L).boardId(1L).isArchived(true).build();
        MoveCardRequestDTO req = new MoveCardRequestDTO();
        req.setTargetListId(2L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(archived));

        assertThrows(IllegalStateException.class,
                () -> cardService.moveCard(1L, req, "user@test.com"));
    }

    @Test
    void moveCard_archivedTargetList_throws() {
        MoveCardRequestDTO req = new MoveCardRequestDTO();
        req.setTargetListId(2L);
        ListResponseDTO target = new ListResponseDTO();
        target.setListId(2L); target.setBoardId(1L); target.setIsArchived(true);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));
        when(listServiceClient.getListById(2L)).thenReturn(target);

        assertThrows(IllegalStateException.class,
                () -> cardService.moveCard(1L, req, "user@test.com"));
    }

    @Test
    void moveCard_toDifferentList_appendsAtEnd() {
        Card c = Card.builder().cardId(1L).listId(1L).boardId(1L).position(2).isArchived(false).build();
        MoveCardRequestDTO req = new MoveCardRequestDTO();
        req.setTargetListId(2L);
        ListResponseDTO target = new ListResponseDTO();
        target.setListId(2L); target.setBoardId(1L); target.setIsArchived(false);

        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(c));
        when(listServiceClient.getListById(2L)).thenReturn(target);
        when(cardRepository.findByListIdOrderByPosition(1L)).thenReturn(List.of(c));
        when(cardRepository.findMaxPositionByListId(2L)).thenReturn(Optional.of(3));
        when(cardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.moveCard(1L, req, "user@test.com");
        assertEquals(2L, result.getListId());
        assertEquals(4, result.getPosition());
    }

    @Test
    void moveCard_specificPosition_shiftsTarget() {
        Card c = Card.builder().cardId(1L).listId(1L).boardId(1L).position(1).isArchived(false).build();
        MoveCardRequestDTO req = new MoveCardRequestDTO();
        req.setTargetListId(2L); req.setPosition(2);
        ListResponseDTO target = new ListResponseDTO();
        target.setListId(2L); target.setBoardId(1L); target.setIsArchived(false);

        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(c));
        when(listServiceClient.getListById(2L)).thenReturn(target);
        when(cardRepository.findByListIdOrderByPosition(1L)).thenReturn(List.of(c));
        when(cardRepository.findByListIdOrderByPosition(2L)).thenReturn(List.of());
        when(cardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.moveCard(1L, req, "user@test.com");
        assertEquals(2, result.getPosition());
    }

    // ─── reorderCards ─────────────────────────────────────────────────────────

    @Test
    void reorderCards_emptyIds_throws() {
        ReorderCardsRequestDTO req = new ReorderCardsRequestDTO();
        req.setOrderedCardIds(List.of());
        mockAuthAndMember("MEMBER");
        when(listServiceClient.getListById(1L)).thenReturn(list(false));

        assertThrows(IllegalArgumentException.class,
                () -> cardService.reorderCards(1L, req, "user@test.com"));
    }

    @Test
    void reorderCards_invalidId_throws() {
        ReorderCardsRequestDTO req = new ReorderCardsRequestDTO();
        req.setOrderedCardIds(List.of(99L));
        mockAuthAndMember("MEMBER");
        when(listServiceClient.getListById(1L)).thenReturn(list(false));
        when(cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(1L)).thenReturn(List.of(testCard));

        assertThrows(IllegalArgumentException.class,
                () -> cardService.reorderCards(1L, req, "user@test.com"));
    }

    @Test
    void reorderCards_success() {
        Card c1 = Card.builder().cardId(1L).listId(1L).position(2).isArchived(false).build();
        Card c2 = Card.builder().cardId(2L).listId(1L).position(1).isArchived(false).build();
        ReorderCardsRequestDTO req = new ReorderCardsRequestDTO();
        req.setOrderedCardIds(List.of(1L, 2L));
        mockAuthAndMember("MEMBER");
        when(listServiceClient.getListById(1L)).thenReturn(list(false));
        when(cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(1L)).thenReturn(List.of(c1, c2));
        when(cardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Card> result = cardService.reorderCards(1L, req, "user@test.com");
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getCardId());
    }

    // ─── archive/unarchive ────────────────────────────────────────────────────

    @Test
    void archiveCard_success() {
        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.archiveCard(1L, "user@test.com");
        assertTrue(result.getIsArchived());
    }

    @Test
    void archiveCard_alreadyArchived_throws() {
        Card archived = Card.builder().cardId(1L).boardId(1L).isArchived(true).build();
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(archived));

        assertThrows(IllegalStateException.class,
                () -> cardService.archiveCard(1L, "user@test.com"));
    }

    @Test
    void unarchiveCard_success() {
        Card archived = Card.builder().cardId(1L).listId(1L).boardId(1L).isArchived(true).build();
        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(archived));
        when(cardRepository.findMaxPositionByListId(1L)).thenReturn(Optional.of(3));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.unarchiveCard(1L, "user@test.com");
        assertFalse(result.getIsArchived());
        assertEquals(4, result.getPosition());
    }

    @Test
    void unarchiveCard_notArchived_throws() {
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));

        assertThrows(IllegalStateException.class,
                () -> cardService.unarchiveCard(1L, "user@test.com"));
    }

    // ─── setAssignee / setPriority / setStatus ────────────────────────────────

    @Test
    void setAssignee_changes_publishesNotification() {
        AssigneeRequestDTO req = new AssigneeRequestDTO();
        req.setAssigneeId(42L);
        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        cardService.setAssignee(1L, req, "user@test.com");
        verify(notificationPublisher).sendAssignmentNotification(eq(10L), eq(42L), eq(1L), any());
    }

    @Test
    void setAssignee_unassign_skipsNotification() {
        Card c = Card.builder().cardId(1L).boardId(1L).assigneeId(42L).isArchived(false).build();
        AssigneeRequestDTO req = new AssigneeRequestDTO();
        req.setAssigneeId(null);
        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(c));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        cardService.setAssignee(1L, req, "user@test.com");
        verify(notificationPublisher, never()).sendAssignmentNotification(any(), any(), any(), any());
    }

    @Test
    void setAssignee_archived_throws() {
        Card archived = Card.builder().cardId(1L).boardId(1L).isArchived(true).build();
        AssigneeRequestDTO req = new AssigneeRequestDTO();
        req.setAssigneeId(42L);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(archived));

        assertThrows(IllegalStateException.class,
                () -> cardService.setAssignee(1L, req, "user@test.com"));
    }

    @Test
    void setPriority_success() {
        PriorityRequestDTO req = new PriorityRequestDTO();
        req.setPriority(Priority.HIGH);
        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.setPriority(1L, req, "user@test.com");
        assertEquals(Priority.HIGH, result.getPriority());
    }

    @Test
    void setPriority_archived_throws() {
        Card archived = Card.builder().cardId(1L).boardId(1L).isArchived(true).build();
        PriorityRequestDTO req = new PriorityRequestDTO();
        req.setPriority(Priority.HIGH);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(archived));

        assertThrows(IllegalStateException.class,
                () -> cardService.setPriority(1L, req, "user@test.com"));
    }

    @Test
    void setStatus_success() {
        StatusRequestDTO req = new StatusRequestDTO();
        req.setStatus(Status.IN_PROGRESS);
        mockAuthAndMember("MEMBER");
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.setStatus(1L, req, "user@test.com");
        assertEquals(Status.IN_PROGRESS, result.getStatus());
    }

    @Test
    void setStatus_archived_throws() {
        Card archived = Card.builder().cardId(1L).boardId(1L).isArchived(true).build();
        StatusRequestDTO req = new StatusRequestDTO();
        req.setStatus(Status.DONE);
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(testUser);
        when(cardRepository.findByCardId(1L)).thenReturn(Optional.of(archived));

        assertThrows(IllegalStateException.class,
                () -> cardService.setStatus(1L, req, "user@test.com"));
    }
}
