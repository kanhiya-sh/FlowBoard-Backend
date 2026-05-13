package com.flowboard.list;

import com.flowboard.list.dto.*;
import com.flowboard.list.entity.TaskList;
import com.flowboard.list.client.AuthServiceClient;
import com.flowboard.list.client.BoardServiceClient;
import com.flowboard.list.repository.ListRepository;
import com.flowboard.list.serviceImpl.ListServiceImpl;
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
class ListServiceTest {

    @Mock private ListRepository listRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private BoardServiceClient boardServiceClient;
    @InjectMocks private ListServiceImpl listService;

    private TaskList testList;

    @BeforeEach
    void setUp() {
        testList = TaskList.builder()
                .listId(1L).name("To Do").boardId(1L).position(0).isArchived(false).build();
    }

    private void mockUserAndMembership(String role) {
        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(10L);
        user.setEmail("user@test.com");
        when(authServiceClient.getUserByEmail("user@test.com")).thenReturn(user);
        when(boardServiceClient.checkBoardMembership(1L, 10L))
                .thenReturn(new BoardMemberCheckDTO(true, role));
    }

    @Test
    void createList_savesAndReturns() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("To Do"); req.setBoardId(1L);

        mockUserAndMembership("MEMBER");
        when(listRepository.save(any(TaskList.class))).thenReturn(testList);
        when(listRepository.findMaxPositionByBoardId(1L)).thenReturn(Optional.empty());

        TaskList result = listService.createList(req, "user@test.com");
        assertNotNull(result);
        assertEquals("To Do", result.getName());
        verify(listRepository).save(any(TaskList.class));
    }

    @Test
    void getListById_notFound_throws() {
        when(listRepository.findByListId(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> listService.getListById(99L));
    }

    @Test
    void getListsByBoard_returnsList() {
        when(listRepository.findByBoardIdOrderByPosition(1L)).thenReturn(List.of(testList));
        List<TaskList> result = listService.getListsByBoardOrdered(1L);
        assertEquals(1, result.size());
    }

    @Test
    void deleteList_callsRepository() {
        mockUserAndMembership("ADMIN");
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        listService.deleteList(1L, "user@test.com");
        verify(listRepository).delete(testList);
    }

    @Test
    void updateList_updatesName() {
        ListRequestDTO req = new ListRequestDTO();
        req.setName("Updated");

        mockUserAndMembership("MEMBER");
        when(listRepository.findByListId(1L)).thenReturn(Optional.of(testList));
        when(listRepository.save(any())).thenReturn(testList);

        TaskList result = listService.updateList(1L, req, "user@test.com");
        assertNotNull(result);
        verify(listRepository).save(any());
    }
}
