package com.flowboard.label.serviceImpl;

import com.flowboard.label.dto.*;
import com.flowboard.label.entity.*;
import com.flowboard.label.exception.ResourceNotFoundException;
import com.flowboard.label.exception.UnauthorizedException;
import com.flowboard.label.mapper.LabelMapper;
import com.flowboard.label.repository.*;
import com.flowboard.label.service.LabelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {
    private final LabelRepository labelRepository;
    private final CardLabelRepository cardLabelRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${card.service.url}")
    private String cardServiceUrl;

    @Value("${board.service.url}")
    private String boardServiceUrl;

    // Internal helpers
    private Long resolveUserIdFromEmail(String email) {
        String url = authServiceUrl + "/auth/internal/users/email/" + email;
        try {
            UserResponseDTO user = restTemplate.getForObject(url, UserResponseDTO.class);
            if (user == null || user.getUserId() == null) {
                throw new UnauthorizedException("Could not resolve user from Auth service");
            }
            return user.getUserId();
        }
        catch (UnauthorizedException e) {
            throw e;
        }
        catch (ResourceAccessException e) {
            log.error("Auth service is DOWN: {}", e.getMessage());
            throw new IllegalStateException("Auth service is currently unavailable.");
        }
        catch (HttpClientErrorException e) {
            log.error("Auth service error for email {}: {}", email, e.getStatusCode());
            throw new UnauthorizedException("User not found in Auth service");
        }
        catch (Exception e) {
            log.error("Unexpected error calling Auth service: {}", e.getMessage());
            throw new IllegalStateException("Auth service error: " + e.getMessage());
        }
    }

    private void verifyBoardExists(Long boardId) {
        // board-service internal endpoint to check board exists
        String url = boardServiceUrl + "/boards/" + boardId;
        try {
            restTemplate.getForObject(url, Object.class);
        }
        catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Board not found with id: " + boardId);
        }
        catch (ResourceAccessException e) {
            log.error("Board service is DOWN: {}", e.getMessage());
            throw new IllegalStateException("Board service is currently unavailable.");
        }
        catch (Exception e) {
            log.warn("Board service call failed for boardId {}: {}", boardId, e.getMessage());
            // Proceed — board may exist, connectivity issue
        }
    }

    private void verifyCardExists(Long cardId) {
        String url = cardServiceUrl + "/cards/internal/" + cardId + "/exists";
        try {
            restTemplate.getForObject(url, Object.class);
        }
        catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Card not found with id: " + cardId);
        }
        catch (ResourceAccessException e) {
            log.error("Card service is DOWN: {}", e.getMessage());
            throw new IllegalStateException("Card service is currently unavailable.");
        }
        catch (Exception e) {
            log.warn("Card service call failed for cardId {}: {}", cardId, e.getMessage());
        }
    }

    // Label CRUD
    @Override
    @Transactional
    public LabelResponseDTO createLabel(LabelRequestDTO dto, String userEmail) {
        resolveUserIdFromEmail(userEmail); // validate user exists
        verifyBoardExists(dto.getBoardId());

        if (labelRepository.existsByBoardIdAndNameIgnoreCase(dto.getBoardId(), dto.getName())) {
            throw new IllegalArgumentException("Label with name '" + dto.getName()
                    + "' already exists on this board");
        }
        Label label = Label.builder()
                .boardId(dto.getBoardId())
                .name(dto.getName())
                .color(dto.getColor())
                .build();

        Label saved = labelRepository.save(label);
        log.info("Label created: labelId={} boardId={}", saved.getLabelId(), dto.getBoardId());
        return LabelMapper.toResponseDTO(saved);
    }

    @Override
    public List<LabelResponseDTO> getLabelsByBoard(Long boardId) {
        return labelRepository.findByBoardId(boardId)
                .stream()
                .map(LabelMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public LabelResponseDTO updateLabel(Long labelId, LabelRequestDTO dto, String userEmail) {
        resolveUserIdFromEmail(userEmail);
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found with id: " + labelId));

        if (dto.getName() != null && !dto.getName().isBlank()) label.setName(dto.getName());
        if (dto.getColor() != null && !dto.getColor().isBlank()) label.setColor(dto.getColor());

        Label saved = labelRepository.save(label);
        log.info("Label updated: labelId={}", labelId);
        return LabelMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void deleteLabel(Long labelId, String userEmail) {
        resolveUserIdFromEmail(userEmail);
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found with id: " + labelId));

        // Remove all card-label associations first
        cardLabelRepository.deleteByLabelId(labelId);
        labelRepository.delete(label);
        log.info("Label deleted: labelId={}", labelId);
    }

    // Card-Label Association
    @Override
    @Transactional
    public void addLabelToCard(Long cardId, Long labelId, String userEmail) {
        resolveUserIdFromEmail(userEmail);
        verifyCardExists(cardId);

        labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found with id: " + labelId));

        if (cardLabelRepository.existsByCardIdAndLabelId(cardId, labelId)) {
            throw new IllegalArgumentException("Label is already attached to this card");
        }

        CardLabel cardLabel = CardLabel.builder()
                .cardId(cardId)
                .labelId(labelId)
                .build();

        cardLabelRepository.save(cardLabel);
        log.info("Label {} added to card {}", labelId, cardId);
    }

    @Override
    @Transactional
    public void removeLabelFromCard(Long cardId, Long labelId, String userEmail) {
        resolveUserIdFromEmail(userEmail);

        CardLabel cardLabel = cardLabelRepository.findByCardIdAndLabelId(cardId, labelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Label " + labelId + " is not attached to card " + cardId));

        cardLabelRepository.delete(cardLabel);
        log.info("Label {} removed from card {}", labelId, cardId);
    }

    @Override
    public List<LabelResponseDTO> getLabelsForCard(Long cardId) {
        List<Long> labelIds = cardLabelRepository.findByCardId(cardId)
                .stream()
                .map(CardLabel::getLabelId)
                .toList();

        if (labelIds.isEmpty()) return List.of();

        return labelRepository.findAllById(labelIds)
                .stream()
                .map(LabelMapper::toResponseDTO)
                .toList();
    }

    // Checklist CRUD
    @Override
    @Transactional
    public ChecklistResponseDTO createChecklist(ChecklistRequestDTO dto, String userEmail) {
        resolveUserIdFromEmail(userEmail);
        verifyCardExists(dto.getCardId());

        int nextPosition = checklistRepository.findMaxPositionByCardId(dto.getCardId())
                .map(max -> max + 1)
                .orElse(0);

        Checklist checklist = Checklist.builder()
                .cardId(dto.getCardId())
                .title(dto.getTitle())
                .position(nextPosition)
                .build();

        Checklist saved = checklistRepository.save(checklist);
        log.info("Checklist created: checklistId={} cardId={}", saved.getChecklistId(), dto.getCardId());

        return LabelMapper.toResponseDTO(saved, List.of());
    }

    @Override
    public List<ChecklistResponseDTO> getChecklistsByCard(Long cardId) {
        return checklistRepository.findByCardIdOrderByPositionAsc(cardId)
                .stream()
                .map(cl -> {
                    List<ChecklistItem> items = checklistItemRepository.findByChecklistId(cl.getChecklistId());
                    return LabelMapper.toResponseDTO(cl, items);
                })
                .toList();
    }

    @Override
    @Transactional
    public void deleteChecklist(Long checklistId, String userEmail) {
        resolveUserIdFromEmail(userEmail);
        Checklist checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found with id: " + checklistId));

        checklistItemRepository.deleteByChecklistId(checklistId);
        checklistRepository.delete(checklist);
        log.info("Checklist deleted: checklistId={}", checklistId);
    }

    // Checklist Items
    @Override
    @Transactional
    public ChecklistItemResponseDTO addItem(ChecklistItemRequestDTO dto, String userEmail) {
        resolveUserIdFromEmail(userEmail);
        checklistRepository.findById(dto.getChecklistId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Checklist not found with id: " + dto.getChecklistId()));

        ChecklistItem item = ChecklistItem.builder()
                .checklistId(dto.getChecklistId())
                .text(dto.getText())
                .isCompleted(false)
                .assigneeId(dto.getAssigneeId())
                .dueDate(dto.getDueDate())
                .build();

        ChecklistItem saved = checklistItemRepository.save(item);
        log.info("Checklist item added: itemId={} checklistId={}", saved.getItemId(), dto.getChecklistId());
        return LabelMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ChecklistItemResponseDTO toggleItem(Long itemId, String userEmail) {
        resolveUserIdFromEmail(userEmail);
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found with id: " + itemId));

        item.setIsCompleted(!item.getIsCompleted());
        ChecklistItem saved = checklistItemRepository.save(item);
        log.info("Checklist item toggled: itemId={} isCompleted={}", itemId, saved.getIsCompleted());
        return LabelMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ChecklistItemResponseDTO updateItem(Long itemId, ChecklistItemRequestDTO dto, String userEmail) {
        resolveUserIdFromEmail(userEmail);
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found with id: " + itemId));

        if (dto.getText() != null && !dto.getText().isBlank()) item.setText(dto.getText());
        item.setAssigneeId(dto.getAssigneeId());
        item.setDueDate(dto.getDueDate());

        ChecklistItem saved = checklistItemRepository.save(item);
        log.info("Checklist item updated: itemId={}", itemId);
        return LabelMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void deleteItem(Long itemId, String userEmail) {
        resolveUserIdFromEmail(userEmail);
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found with id: " + itemId));

        checklistItemRepository.delete(item);
        log.info("Checklist item deleted: itemId={}", itemId);
    }

    // Progress
    @Override
    public ChecklistProgressDTO getChecklistProgress(Long checklistId) {
        Checklist checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found with id: " + checklistId));

        long total = checklistItemRepository.countByChecklistId(checklistId);
        long completed = checklistItemRepository.countByChecklistIdAndIsCompletedTrue(checklistId);
        double percentage = total == 0 ? 0.0 : (completed * 100.0) / total;

        return ChecklistProgressDTO.builder()
                .checklistId(checklistId)
                .title(checklist.getTitle())
                .totalItems((int) total)
                .completedItems((int) completed)
                .completionPercentage(Math.round(percentage * 100.0) / 100.0)
                .build();
    }
}

