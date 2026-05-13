package com.flowboard.label.service;

import com.flowboard.label.dto.*;
import java.util.List;

public interface LabelService {

    // Label CRUD
    LabelResponseDTO createLabel(LabelRequestDTO dto, String userEmail);
    List<LabelResponseDTO> getLabelsByBoard(Long boardId);
    LabelResponseDTO updateLabel(Long labelId, LabelRequestDTO dto, String userEmail);
    void deleteLabel(Long labelId, String userEmail);

    // Card-Label Association
    void addLabelToCard(Long cardId, Long labelId, String userEmail);
    void removeLabelFromCard(Long cardId, Long labelId, String userEmail);
    List<LabelResponseDTO> getLabelsForCard(Long cardId);

    // Checklist CRUD
    ChecklistResponseDTO createChecklist(ChecklistRequestDTO dto, String userEmail);
    List<ChecklistResponseDTO> getChecklistsByCard(Long cardId);
    void deleteChecklist(Long checklistId, String userEmail);

    // Checklist Items
    ChecklistItemResponseDTO addItem(ChecklistItemRequestDTO dto, String userEmail);
    ChecklistItemResponseDTO toggleItem(Long itemId, String userEmail);
    ChecklistItemResponseDTO updateItem(Long itemId, ChecklistItemRequestDTO dto, String userEmail);
    void deleteItem(Long itemId, String userEmail);

    // Progress
    ChecklistProgressDTO getChecklistProgress(Long checklistId);
}

