package com.flowboard.board;

import com.flowboard.board.dto.*;
import com.flowboard.board.entity.Board;
import com.flowboard.board.entity.BoardMember;
import com.flowboard.board.enums.BoardRole;
import com.flowboard.board.enums.Visibility;
import com.flowboard.board.client.AuthServiceClient;
import com.flowboard.board.client.WorkspaceServiceClient;
import com.flowboard.board.repository.BoardMemberRepository;
import com.flowboard.board.repository.BoardRepository;
import com.flowboard.board.serviceImpl.BoardServiceImpl;
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
class BoardServiceTest {

    @Mock private BoardRepository boardRepository;
    @Mock private BoardMemberRepository boardMemberRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private WorkspaceServiceClient workspaceServiceClient;
    @InjectMocks private BoardServiceImpl boardService;

    private Board testBoard;

    @BeforeEach
    void setUp() {
        testBoard = Board.builder()
                .boardId(1L).name("Test Board")
                .workspaceId(1L).createdById(1L)
                .visibility(Visibility.PRIVATE).isClosed(false).build();
    }

    @Test
    void getBoardById_returnsBoard() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        Board result = boardService.getBoardById(1L);
        assertNotNull(result);
        assertEquals("Test Board", result.getName());
    }

    @Test
    void getBoardById_notFound_throws() {
        when(boardRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> boardService.getBoardById(99L));
    }

    @Test
    void createBoard_savesBoard() {
        BoardRequestDTO req = new BoardRequestDTO();
        req.setName("New Board"); req.setWorkspaceId(1L);

        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(1L);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user);
        when(workspaceServiceClient.checkMembership(anyLong(), anyLong()))
                .thenReturn(new WorkspaceMemberCheckDTO(true, "ADMIN"));
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(boardMemberRepository.save(any(BoardMember.class))).thenReturn(new BoardMember());

        Board result = boardService.createBoard(req, "user@test.com");
        assertNotNull(result);
        verify(boardRepository).save(any(Board.class));
    }

    @Test
    void getBoardsByWorkspace_returnsList() {
        when(boardRepository.findByWorkspaceId(1L)).thenReturn(List.of(testBoard));
        List<Board> results = boardService.getBoardsByWorkspace(1L);
        assertEquals(1, results.size());
    }

    @Test
    void deleteBoard_callsRepository() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        // owner deleting
        testBoard.setCreatedById(1L);
        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(1L);
        when(authServiceClient.getUserByEmail(anyString())).thenReturn(user);
        BoardMember adminMember = BoardMember.builder().boardId(1L).userId(1L).role(BoardRole.ADMIN).build();
        when(boardMemberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(boardMemberRepository.findByBoardId(1L)).thenReturn(List.of(adminMember));
        boardService.deleteBoard(1L, "user@test.com");
        verify(boardRepository).delete(testBoard);
    }
}
