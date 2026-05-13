package com.flowboard.label.controller;

import com.flowboard.label.dto.*;
import com.flowboard.label.service.LabelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final LabelService labelService;

    private String getEmail(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        if (email == null) throw new IllegalStateException("User not authenticated");
        return email;
    }

    // Checklist CRUD
    @PostMapping
    public ResponseEntity<ChecklistResponseDTO> createChecklist(
            @Valid @RequestBody ChecklistRequestDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(labelService.createChecklist(dto, getEmail(request)));
    }


    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<ChecklistResponseDTO>> getChecklistsByCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(labelService.getChecklistsByCard(cardId));
    }

    @DeleteMapping("/{checklistId}")
    public ResponseEntity<String> deleteChecklist(
            @PathVariable Long checklistId,
            HttpServletRequest request) {
        labelService.deleteChecklist(checklistId, getEmail(request));
        return ResponseEntity.ok("Checklist deleted successfully");
    }

    @GetMapping("/{checklistId}/progress")
    public ResponseEntity<ChecklistProgressDTO> getChecklistProgress(@PathVariable Long checklistId) {
        return ResponseEntity.ok(labelService.getChecklistProgress(checklistId));
    }

    // Checklist Items
    @PostMapping("/items")
    public ResponseEntity<ChecklistItemResponseDTO> addItem(
            @Valid @RequestBody ChecklistItemRequestDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(labelService.addItem(dto, getEmail(request)));
    }

    @PutMapping("/items/{itemId}/toggle")
    public ResponseEntity<ChecklistItemResponseDTO> toggleItem(
            @PathVariable Long itemId,
            HttpServletRequest request) {
        return ResponseEntity.ok(labelService.toggleItem(itemId, getEmail(request)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ChecklistItemResponseDTO> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistItemRequestDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(labelService.updateItem(itemId, dto, getEmail(request)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<String> deleteItem(
            @PathVariable Long itemId,
            HttpServletRequest request) {
        labelService.deleteItem(itemId, getEmail(request));
        return ResponseEntity.ok("Checklist item deleted successfully");
    }

    // Internal Endpoints (no JWT)
    @GetMapping("/internal/card/{cardId}")
    public ResponseEntity<List<ChecklistResponseDTO>> getChecklistsByCardInternal(@PathVariable Long cardId) {
        return ResponseEntity.ok(labelService.getChecklistsByCard(cardId));
    }
}

