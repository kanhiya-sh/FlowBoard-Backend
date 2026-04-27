package com.flowboard.list.controller;

import com.flowboard.list.dto.*;
import com.flowboard.list.mapper.ListMapper;
import com.flowboard.list.service.ListService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/lists")
@RequiredArgsConstructor
public class ListController {
    private final ListService listService;
    // ------ Helper ----------
    private String getEmail(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        if (email == null) throw new IllegalStateException("User not authenticated");
        return email;
    }

    // -------- CRUD --------

//  Create a new list/column on a board.
//  Caller must be a member of the board
    @PostMapping
    public ResponseEntity<ListResponseDTO> createList(
            @Valid @RequestBody ListRequestDTO dto,
            HttpServletRequest request) {
        var saved = listService.createList(dto, getEmail(request));
        return ResponseEntity.ok(ListMapper.toResponseDTO(saved));
    }

//  Get a single list by its ID
    @GetMapping("/{listId}")
    public ResponseEntity<ListResponseDTO> getListById(@PathVariable Long listId) {
        return ResponseEntity.ok(ListMapper.toResponseDTO(listService.getListById(listId)));
    }

//  Get all active (non-archived) lists for a board, ordered by position.
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<ListResponseDTO>> getListsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(
                listService.getListsByBoardOrdered(boardId)
                        .stream()
                        .filter(l -> !l.getIsArchived())
                        .map(ListMapper::toResponseDTO)
                        .toList()
        );
    }

//  Get ALL lists for a board including archived, ordered by position.
    @GetMapping("/board/{boardId}/all")
    public ResponseEntity<List<ListResponseDTO>> getAllListsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(
                listService.getListsByBoardOrdered(boardId)
                        .stream()
                        .map(ListMapper::toResponseDTO)
                        .toList()
        );
    }

//  Get all archived lists for a board (for restoration view).
    @GetMapping("/board/{boardId}/archived")
    public ResponseEntity<List<ListResponseDTO>> getArchivedLists(@PathVariable Long boardId) {
        return ResponseEntity.ok(
                listService.getArchivedLists(boardId)
                        .stream()
                        .map(ListMapper::toResponseDTO)
                        .toList()
        );
    }

//  Rename a list or change its color.
//  Caller must be a member of the board.
    @PutMapping("/{listId}")
    public ResponseEntity<ListResponseDTO> updateList(
            @PathVariable Long listId,
            @Valid @RequestBody ListRequestDTO dto,
            HttpServletRequest request) {
        var updated = listService.updateList(listId, dto, getEmail(request));
        return ResponseEntity.ok(ListMapper.toResponseDTO(updated));
    }

//  Permanently delete a list.
//  Caller must be a board ADMIN.
    @DeleteMapping("/{listId}")
    public ResponseEntity<String> deleteList(
            @PathVariable Long listId,
            HttpServletRequest request) {
        listService.deleteList(listId, getEmail(request));
        return ResponseEntity.ok("List deleted successfully");
    }

    // --- Position Management -----
    /**
     * Atomically reorder all lists on a board via drag-and-drop.
     * Body: { "orderedListIds": [3, 1, 2] } — index = new position.
     * Caller must be a board member.
     */
    @PutMapping("/board/{boardId}/reorder")
    public ResponseEntity<List<ListResponseDTO>> reorderLists(
            @PathVariable Long boardId,
            @Valid @RequestBody ReorderRequestDTO dto,
            HttpServletRequest request) {
        var reordered = listService.reorderLists(boardId, dto, getEmail(request));
        return ResponseEntity.ok(reordered.stream().map(ListMapper::toResponseDTO).toList());
    }

    // ---- Archival ----
    /**
     * Soft-archive a list (hides from board view, recoverable).
     * Caller must be a board member.
     */
    @PostMapping("/{listId}/archive")
    public ResponseEntity<ListResponseDTO> archiveList(
            @PathVariable Long listId,
            HttpServletRequest request) {
        var archived = listService.archiveList(listId, getEmail(request));
        return ResponseEntity.ok(ListMapper.toResponseDTO(archived));
    }

    /**
     * Restore an archived list back to the board.
     * Caller must be a board member.
     */
    @PostMapping("/{listId}/unarchive")
    public ResponseEntity<ListResponseDTO> unarchiveList(
            @PathVariable Long listId,
            HttpServletRequest request) {
        var unarchived = listService.unarchiveList(listId, getEmail(request));
        return ResponseEntity.ok(ListMapper.toResponseDTO(unarchived));
    }

    // ---- Board Transfer ----
    /**
     * Move a list to a different board (within the same workspace).
     * Caller must be ADMIN of the source board and a member of the target board.
     */
    @PutMapping("/{listId}/move")
    public ResponseEntity<ListResponseDTO> moveList(
            @PathVariable Long listId,
            @Valid @RequestBody MoveListRequestDTO dto,
            HttpServletRequest request) {
        var moved = listService.moveList(listId, dto, getEmail(request));
        return ResponseEntity.ok(ListMapper.toResponseDTO(moved));
    }

    // ---- Internal Endpoint ----
    /**
     * Internal endpoint used by other services (e.g. card-service) to get
     * the list count for a board. No JWT needed (called service-to-service).
     */
    @GetMapping("/internal/board/{boardId}/count")
    public ResponseEntity<Long> getListCountByBoard(@PathVariable Long boardId) {
        long count = listService.getListsByBoard(boardId).size();
        return ResponseEntity.ok(count);
    }

    /**
     * Internal endpoint used by card-service to verify a listId exists and
     * retrieve its boardId. No JWT needed (called service-to-service).
     */
    @GetMapping("/internal/{listId}/exists")
    public ResponseEntity<ListResponseDTO> checkListExists(@PathVariable Long listId) {
        var list = listService.getListById(listId);
        return ResponseEntity.ok(ListMapper.toResponseDTO(list));
    }
}
