package com.flowboard.card.client;

import com.flowboard.card.dto.BoardMemberCheckDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "board-service")
public interface BoardServiceClient {

    @GetMapping("/boards/internal/{boardId}/members/{userId}/check")
    BoardMemberCheckDTO checkBoardMembership(
            @PathVariable("boardId") Long boardId,
            @PathVariable("userId") Long userId);
}
