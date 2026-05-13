package com.flowboard.card;

import com.flowboard.card.dto.*;
import com.flowboard.card.entity.Card;
import com.flowboard.card.enums.Priority;
import com.flowboard.card.enums.Status;
import com.flowboard.card.client.AuthServiceClient;
import com.flowboard.card.client.BoardServiceClient;
import com.flowboard.card.client.ListServiceClient;
import com.flowboard.card.repository.CardRepository;
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
    @InjectMocks private CardServiceImpl cardService;

    private Card testCard;

    @BeforeEach
    void setUp() {
        testCard = Card.builder()
                .cardId(1L).title("Test Card").listId(1L).boardId(1L)
                .position(0).priority(Priority.MEDIUM).status(Status.TO_DO).build();
    }

    @Test
    void createCard_savesAndReturns() {
        CardRequestDTO req = new CardRequestDTO();
        req.setTitle("New Card"); req.setListId(1L); req.setBoardId(1L);

        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardRepository.findByListIdOrderByPosition(1L)).thenReturn(List.of());

        Card result = cardService.createCard(req, "user@test.com");
        assertNotNull(result);
        assertEquals("Test Card", result.getTitle());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void getCardById_notFound_throws() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> cardService.getCardById(99L));
    }

    @Test
    void getCardsByList_returnsList() {
        when(cardRepository.findByListIdOrderByPosition(1L)).thenReturn(List.of(testCard));
        List<Card> result = cardService.getCardsByList(1L);
        assertEquals(1, result.size());
        assertEquals("Test Card", result.get(0).getTitle());
    }

    @Test
    void deleteCard_callsRepository() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        cardService.deleteCard(1L, "user@test.com");
        verify(cardRepository).delete(testCard);
    }
}
