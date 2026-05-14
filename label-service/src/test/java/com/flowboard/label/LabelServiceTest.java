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
    private UserResponseDTO testUser;

    @BeforeEach
    void setUp() {
        testLabel = Label.builder()
                .labelId(1L).name("Bug").boardId(1L).color("#FF0000").build();

        testUser = new UserResponseDTO();
        testUser.setUserId(10L);
    }

    @Test
    void createLabel_savesAndReturns() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("Bug"); req.setBoardId(1L); req.setColor("#FF0000");

        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(labelRepository.existsByBoardIdAndNameIgnoreCase(1L, "Bug")).thenReturn(false);
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
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));
        labelService.deleteLabel(1L, "user@test.com");
        verify(labelRepository).delete(testLabel);
    }

    @Test
    void deleteLabel_notFound_throws() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(labelRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> labelService.deleteLabel(99L, "user@test.com"));
    }

    // ─── createLabel branches ─────────────────────────────────────────────────

    @Test
    void createLabel_duplicateName_throwsIllegalArgument() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("Bug"); req.setBoardId(1L); req.setColor("#FF0000");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(labelRepository.existsByBoardIdAndNameIgnoreCase(1L, "Bug")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> labelService.createLabel(req, "user@test.com"));
        verify(labelRepository, never()).save(any());
    }

    @Test
    void createLabel_authReturnsNull_throwsUnauthorized() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("X"); req.setBoardId(1L); req.setColor("#000");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(null);

        assertThrows(com.flowboard.label.exception.UnauthorizedException.class,
                () -> labelService.createLabel(req, "user@test.com"));
    }

    @Test
    void createLabel_authFeignNotFound_throwsUnauthorized() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("X"); req.setBoardId(1L); req.setColor("#000");
        feign.FeignException.NotFound notFound = mock(feign.FeignException.NotFound.class);
        when(authServiceClient.getUserByEmail(anyString())).thenThrow(notFound);

        assertThrows(com.flowboard.label.exception.UnauthorizedException.class,
                () -> labelService.createLabel(req, "user@test.com"));
    }

    @Test
    void createLabel_authFeignGenericError_throwsIllegalState() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("X"); req.setBoardId(1L); req.setColor("#000");
        feign.FeignException feignErr = mock(feign.FeignException.class);
        when(feignErr.getMessage()).thenReturn("503");
        when(authServiceClient.getUserByEmail(anyString())).thenThrow(feignErr);

        assertThrows(IllegalStateException.class,
                () -> labelService.createLabel(req, "user@test.com"));
    }

    @Test
    void createLabel_authGenericException_throwsIllegalState() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("X"); req.setBoardId(1L); req.setColor("#000");
        when(authServiceClient.getUserByEmail(anyString()))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(IllegalStateException.class,
                () -> labelService.createLabel(req, "user@test.com"));
    }

    @Test
    void createLabel_boardFeignNotFound_throwsResourceNotFound() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("X"); req.setBoardId(1L); req.setColor("#000");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        feign.FeignException.NotFound notFound = mock(feign.FeignException.NotFound.class);
        when(boardServiceClient.getBoardById(1L)).thenThrow(notFound);

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.createLabel(req, "user@test.com"));
    }

    @Test
    void createLabel_boardFeignError_throwsIllegalState() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("X"); req.setBoardId(1L); req.setColor("#000");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        feign.FeignException feignErr = mock(feign.FeignException.class);
        when(feignErr.getMessage()).thenReturn("500");
        when(boardServiceClient.getBoardById(1L)).thenThrow(feignErr);

        assertThrows(IllegalStateException.class,
                () -> labelService.createLabel(req, "user@test.com"));
    }

    @Test
    void createLabel_boardGenericException_isSwallowed_andCreateProceeds() {
        // Service code: generic catch in verifyBoardExists just logs and continues
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("Bug"); req.setBoardId(1L); req.setColor("#FF0000");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(boardServiceClient.getBoardById(1L)).thenThrow(new RuntimeException("net"));
        when(labelRepository.existsByBoardIdAndNameIgnoreCase(1L, "Bug")).thenReturn(false);
        when(labelRepository.save(any(Label.class))).thenReturn(testLabel);

        LabelResponseDTO result = labelService.createLabel(req, "user@test.com");
        assertNotNull(result);
    }

    // ─── updateLabel ──────────────────────────────────────────────────────────

    @Test
    void updateLabel_updatesNameAndColor() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName("NewName"); req.setColor("#00FF00"); req.setBoardId(1L);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));
        when(labelRepository.save(any(Label.class))).thenAnswer(inv -> inv.getArgument(0));

        LabelResponseDTO result = labelService.updateLabel(1L, req, "user@test.com");
        assertEquals("NewName", result.getName());
        assertEquals("#00FF00", result.getColor());
    }

    @Test
    void updateLabel_skipsBlankFields() {
        LabelRequestDTO req = new LabelRequestDTO();
        req.setName(""); req.setColor("  ");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));
        when(labelRepository.save(any(Label.class))).thenAnswer(inv -> inv.getArgument(0));

        LabelResponseDTO result = labelService.updateLabel(1L, req, "user@test.com");
        // unchanged because blank
        assertEquals("Bug", result.getName());
        assertEquals("#FF0000", result.getColor());
    }

    @Test
    void updateLabel_notFound_throws() {
        LabelRequestDTO req = new LabelRequestDTO();
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(labelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.updateLabel(99L, req, "user@test.com"));
    }

    // ─── addLabelToCard ───────────────────────────────────────────────────────

    @Test
    void addLabelToCard_success() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));
        when(cardLabelRepository.existsByCardIdAndLabelId(10L, 1L)).thenReturn(false);

        labelService.addLabelToCard(10L, 1L, "user@test.com");

        verify(cardLabelRepository).save(any());
    }

    @Test
    void addLabelToCard_alreadyAttached_throws() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));
        when(cardLabelRepository.existsByCardIdAndLabelId(10L, 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> labelService.addLabelToCard(10L, 1L, "user@test.com"));
        verify(cardLabelRepository, never()).save(any());
    }

    @Test
    void addLabelToCard_labelNotFound_throws() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(labelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.addLabelToCard(10L, 99L, "user@test.com"));
    }

    @Test
    void addLabelToCard_cardNotFound_throws() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        feign.FeignException.NotFound notFound = mock(feign.FeignException.NotFound.class);
        when(cardServiceClient.getCardById(10L)).thenThrow(notFound);

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.addLabelToCard(10L, 1L, "user@test.com"));
    }

    // ─── removeLabelFromCard ──────────────────────────────────────────────────

    @Test
    void removeLabelFromCard_success() {
        com.flowboard.label.entity.CardLabel cl =
                com.flowboard.label.entity.CardLabel.builder().cardId(10L).labelId(1L).build();
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(cardLabelRepository.findByCardIdAndLabelId(10L, 1L)).thenReturn(Optional.of(cl));

        labelService.removeLabelFromCard(10L, 1L, "user@test.com");
        verify(cardLabelRepository).delete(cl);
    }

    @Test
    void removeLabelFromCard_notAttached_throws() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(cardLabelRepository.findByCardIdAndLabelId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.removeLabelFromCard(10L, 1L, "user@test.com"));
    }

    // ─── getLabelsForCard ─────────────────────────────────────────────────────

    @Test
    void getLabelsForCard_returnsLabels() {
        com.flowboard.label.entity.CardLabel cl =
                com.flowboard.label.entity.CardLabel.builder().cardId(10L).labelId(1L).build();
        when(cardLabelRepository.findByCardId(10L)).thenReturn(List.of(cl));
        when(labelRepository.findAllById(List.of(1L))).thenReturn(List.of(testLabel));

        List<LabelResponseDTO> result = labelService.getLabelsForCard(10L);
        assertEquals(1, result.size());
    }

    @Test
    void getLabelsForCard_emptyAssociations_returnsEmpty() {
        when(cardLabelRepository.findByCardId(10L)).thenReturn(List.of());

        List<LabelResponseDTO> result = labelService.getLabelsForCard(10L);
        assertTrue(result.isEmpty());
        verify(labelRepository, never()).findAllById(any());
    }

    // ─── Checklist CRUD ───────────────────────────────────────────────────────

    @Test
    void createChecklist_firstChecklist_positionZero() {
        ChecklistRequestDTO req = new ChecklistRequestDTO();
        req.setCardId(10L); req.setTitle("My Checklist");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistRepository.findMaxPositionByCardId(10L)).thenReturn(Optional.empty());
        com.flowboard.label.entity.Checklist saved =
                com.flowboard.label.entity.Checklist.builder()
                        .checklistId(1L).cardId(10L).title("My Checklist").position(0).build();
        when(checklistRepository.save(any())).thenReturn(saved);

        ChecklistResponseDTO result = labelService.createChecklist(req, "user@test.com");
        assertNotNull(result);
        assertEquals(0, result.getPosition());
    }

    @Test
    void createChecklist_existingChecklists_positionIncrements() {
        ChecklistRequestDTO req = new ChecklistRequestDTO();
        req.setCardId(10L); req.setTitle("Second");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistRepository.findMaxPositionByCardId(10L)).thenReturn(Optional.of(2));
        com.flowboard.label.entity.Checklist saved =
                com.flowboard.label.entity.Checklist.builder()
                        .checklistId(2L).cardId(10L).title("Second").position(3).build();
        when(checklistRepository.save(any())).thenReturn(saved);

        ChecklistResponseDTO result = labelService.createChecklist(req, "user@test.com");
        assertEquals(3, result.getPosition());
    }

    @Test
    void getChecklistsByCard_returnsListWithItems() {
        com.flowboard.label.entity.Checklist cl =
                com.flowboard.label.entity.Checklist.builder()
                        .checklistId(1L).cardId(10L).title("X").position(0).build();
        when(checklistRepository.findByCardIdOrderByPositionAsc(10L)).thenReturn(List.of(cl));
        when(checklistItemRepository.findByChecklistId(1L)).thenReturn(List.of());

        List<ChecklistResponseDTO> result = labelService.getChecklistsByCard(10L);
        assertEquals(1, result.size());
    }

    @Test
    void deleteChecklist_success() {
        com.flowboard.label.entity.Checklist cl =
                com.flowboard.label.entity.Checklist.builder().checklistId(1L).build();
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(cl));

        labelService.deleteChecklist(1L, "user@test.com");
        verify(checklistItemRepository).deleteByChecklistId(1L);
        verify(checklistRepository).delete(cl);
    }

    @Test
    void deleteChecklist_notFound_throws() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.deleteChecklist(99L, "user@test.com"));
    }

    // ─── Checklist Items ──────────────────────────────────────────────────────

    @Test
    void addItem_success() {
        ChecklistItemRequestDTO req = new ChecklistItemRequestDTO();
        req.setChecklistId(1L); req.setText("Item 1");
        com.flowboard.label.entity.Checklist cl =
                com.flowboard.label.entity.Checklist.builder().checklistId(1L).build();
        com.flowboard.label.entity.ChecklistItem saved =
                com.flowboard.label.entity.ChecklistItem.builder()
                        .itemId(1L).checklistId(1L).text("Item 1").isCompleted(false).build();
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(cl));
        when(checklistItemRepository.save(any())).thenReturn(saved);

        ChecklistItemResponseDTO result = labelService.addItem(req, "user@test.com");
        assertNotNull(result);
        assertEquals("Item 1", result.getText());
    }

    @Test
    void addItem_checklistNotFound_throws() {
        ChecklistItemRequestDTO req = new ChecklistItemRequestDTO();
        req.setChecklistId(99L); req.setText("X");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.addItem(req, "user@test.com"));
    }

    @Test
    void toggleItem_flipsCompleted() {
        com.flowboard.label.entity.ChecklistItem item =
                com.flowboard.label.entity.ChecklistItem.builder()
                        .itemId(1L).checklistId(1L).text("X").isCompleted(false).build();
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(checklistItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChecklistItemResponseDTO result = labelService.toggleItem(1L, "user@test.com");
        assertTrue(result.getIsCompleted());
    }

    @Test
    void toggleItem_notFound_throws() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.toggleItem(99L, "user@test.com"));
    }

    @Test
    void updateItem_updatesFields() {
        com.flowboard.label.entity.ChecklistItem item =
                com.flowboard.label.entity.ChecklistItem.builder()
                        .itemId(1L).checklistId(1L).text("old").isCompleted(false).build();
        ChecklistItemRequestDTO req = new ChecklistItemRequestDTO();
        req.setText("new text"); req.setAssigneeId(99L);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(checklistItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChecklistItemResponseDTO result = labelService.updateItem(1L, req, "user@test.com");
        assertEquals("new text", result.getText());
        assertEquals(99L, result.getAssigneeId());
    }

    @Test
    void updateItem_blankText_keepsOld() {
        com.flowboard.label.entity.ChecklistItem item =
                com.flowboard.label.entity.ChecklistItem.builder()
                        .itemId(1L).text("keep").isCompleted(false).build();
        ChecklistItemRequestDTO req = new ChecklistItemRequestDTO();
        req.setText("   ");
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(checklistItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChecklistItemResponseDTO result = labelService.updateItem(1L, req, "user@test.com");
        assertEquals("keep", result.getText());
    }

    @Test
    void updateItem_notFound_throws() {
        ChecklistItemRequestDTO req = new ChecklistItemRequestDTO();
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.updateItem(99L, req, "user@test.com"));
    }

    @Test
    void deleteItem_success() {
        com.flowboard.label.entity.ChecklistItem item =
                com.flowboard.label.entity.ChecklistItem.builder().itemId(1L).build();
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistItemRepository.findById(1L)).thenReturn(Optional.of(item));

        labelService.deleteItem(1L, "user@test.com");
        verify(checklistItemRepository).delete(item);
    }

    @Test
    void deleteItem_notFound_throws() {
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(testUser);
        when(checklistItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.deleteItem(99L, "user@test.com"));
    }

    // ─── Progress ─────────────────────────────────────────────────────────────

    @Test
    void getChecklistProgress_calculatesPercentage() {
        com.flowboard.label.entity.Checklist cl =
                com.flowboard.label.entity.Checklist.builder()
                        .checklistId(1L).title("X").build();
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(cl));
        when(checklistItemRepository.countByChecklistId(1L)).thenReturn(4L);
        when(checklistItemRepository.countByChecklistIdAndIsCompletedTrue(1L)).thenReturn(2L);

        ChecklistProgressDTO result = labelService.getChecklistProgress(1L);
        assertEquals(4, result.getTotalItems());
        assertEquals(2, result.getCompletedItems());
        assertEquals(50.0, result.getCompletionPercentage());
    }

    @Test
    void getChecklistProgress_emptyChecklist_zeroPercentage() {
        com.flowboard.label.entity.Checklist cl =
                com.flowboard.label.entity.Checklist.builder().checklistId(1L).title("X").build();
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(cl));
        when(checklistItemRepository.countByChecklistId(1L)).thenReturn(0L);
        when(checklistItemRepository.countByChecklistIdAndIsCompletedTrue(1L)).thenReturn(0L);

        ChecklistProgressDTO result = labelService.getChecklistProgress(1L);
        assertEquals(0.0, result.getCompletionPercentage());
    }

    @Test
    void getChecklistProgress_notFound_throws() {
        when(checklistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.flowboard.label.exception.ResourceNotFoundException.class,
                () -> labelService.getChecklistProgress(99L));
    }
}
