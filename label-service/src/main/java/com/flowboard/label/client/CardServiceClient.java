package com.flowboard.label.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "card-service")
public interface CardServiceClient {

    @GetMapping("/cards/internal/{cardId}/exists")
    Object getCardById(@PathVariable("cardId") Long cardId);
}
