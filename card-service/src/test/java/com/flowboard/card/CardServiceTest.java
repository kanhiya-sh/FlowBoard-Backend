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
}
