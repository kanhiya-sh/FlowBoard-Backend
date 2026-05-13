package com.flowboard.board.client;

import com.flowboard.board.dto.WorkspaceMemberCheckDTO;
import com.flowboard.board.dto.WorkspaceMemberDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "workspace-service")
public interface WorkspaceServiceClient {

    @GetMapping("/workspaces/internal/{workspaceId}/members/{userId}/check")
    WorkspaceMemberCheckDTO checkMembership(
            @PathVariable("workspaceId") Long workspaceId,
            @PathVariable("userId") Long userId);

    // Used to populate the "assignable users" list for a board — unions board
    // members with workspace members so invited users can be assigned even
    // when they haven't been added to board_members explicitly.
    @GetMapping("/workspaces/internal/{workspaceId}/members")
    List<WorkspaceMemberDTO> listMembers(@PathVariable("workspaceId") Long workspaceId);
}
