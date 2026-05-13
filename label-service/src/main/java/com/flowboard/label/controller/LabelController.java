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
@RequestMapping("/labels")
@RequiredArgsConstructor
public class LabelController {
    private final LabelService labelService;
    private String getEmail(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        if (email == null) throw new IllegalStateException("User not authenticated");
        return email;
    }

    // Label CRUD
    @PostMapping
    public ResponseEntity<LabelResponseDTO> createLabel(
            @Valid @RequestBody LabelRequestDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(labelService.createLabel(dto, getEmail(request)));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<LabelResponseDTO>> getLabelsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(labelService.getLabelsByBoard(boardId));
    }

    @PutMapping("/{labelId}")
    public ResponseEntity<LabelResponseDTO> updateLabel(
            @PathVariable Long labelId,
            @Valid @RequestBody LabelRequestDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(labelService.updateLabel(labelId, dto, getEmail(request)));
    }

    @DeleteMapping("/{labelId}")
    public ResponseEntity<String> deleteLabel(
            @PathVariable Long labelId,
            HttpServletRequest request) {
        labelService.deleteLabel(labelId, getEmail(request));
        return ResponseEntity.ok("Label deleted successfully");
    }

    // Card-Label Association
    @PostMapping("/{labelId}/card/{cardId}")
    public ResponseEntity<String> addLabelToCard(
            @PathVariable Long labelId,
            @PathVariable Long cardId,
            HttpServletRequest request) {
        labelService.addLabelToCard(cardId, labelId, getEmail(request));
        return ResponseEntity.ok("Label added to card successfully");
    }

    @DeleteMapping("/{labelId}/card/{cardId}")
    public ResponseEntity<String> removeLabelFromCard(
            @PathVariable Long labelId,
            @PathVariable Long cardId,
            HttpServletRequest request) {
        labelService.removeLabelFromCard(cardId, labelId, getEmail(request));
        return ResponseEntity.ok("Label removed from card successfully");
    }

    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<LabelResponseDTO>> getLabelsForCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(labelService.getLabelsForCard(cardId));
    }

    // Internal Endpoints (service-to-service, no JWT)
    @GetMapping("/internal/card/{cardId}")
    public ResponseEntity<List<LabelResponseDTO>> getLabelsForCardInternal(@PathVariable Long cardId) {
        return ResponseEntity.ok(labelService.getLabelsForCard(cardId));
    }
}

