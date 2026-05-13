package com.flowboard.label.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "board-service")
public interface BoardServiceClient {

    @GetMapping("/boards/{boardId}")
    Object getBoardById(@PathVariable("boardId") Long boardId);
}
