package com.flowboard.label;

import com.flowboard.label.client.AuthServiceClient;
import com.flowboard.label.client.BoardServiceClient;
import com.flowboard.label.client.CardServiceClient;
import com.flowboard.label.dto.*;
import com.flowboard.label.entity.Label;
import com.flowboard.label.repository.CardLabelRepository;
import com.flowboard.label.repository.ChecklistItemRepository;
import com.flowboard.label.repository.ChecklistRepository;
import com.flowboard.label.repository.LabelRepository;
import com.flowboard.label.serviceImpl.LabelServiceImpl;
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
class LabelServiceTest {

    @Mock private LabelRepository labelRepository;
    @Mock private CardLabelRepository cardLabelRepository;
    @Mock private ChecklistRepository checklistRepository;
    @Mock private ChecklistItemRepository checklistItemRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private BoardServiceClient boardServiceClient;
    @Mock private CardServiceClient cardServiceClient;
    @InjectMocks private LabelServiceImpl labelService;

    private Label testLabel;

    @BeforeEach
    void setUp() {
        testLabel = Label.builder()
                .labelId(1L).name("Bug").boardId(1L).color("#FF0000").build();
    }

    @Test
    void createLabel_savesAndReturns() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("Bug"); req.setBoardId(1L); req.setColor("#FF0000");

        when(labelRepository.save(any(Label.class))).thenReturn(testLabel);

        LabelResponseDTO result = labelService.createLabel(req, "user@test.com");
        assertNotNull(result);
        verify(labelRepository).save(any(Label.class));
    }

    @Test
    void getLabelsByBoard_returnsList() {
        when(labelRepository.findByBoardId(1L)).thenReturn(List.of(testLabel));
        List<LabelResponseDTO> result = labelService.getLabelsByBoard(1L);
        assertEquals(1, result.size());
    }

    @Test
    void deleteLabel_callsRepository() {
        when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));
        labelService.deleteLabel(1L, "user@test.com");
        verify(labelRepository).delete(testLabel);
    }

    @Test
    void deleteLabel_notFound_throws() {
        when(labelRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> labelService.deleteLabel(99L, "user@test.com"));
    }
}
