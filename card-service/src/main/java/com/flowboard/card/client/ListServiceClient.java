package com.flowboard.card.client;

import com.flowboard.card.dto.ListResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "list-service")
public interface ListServiceClient {

    @GetMapping("/lists/internal/{listId}/exists")
    ListResponseDTO getListById(@PathVariable("listId") Long listId);
}
