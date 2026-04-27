package com.flowboard.comment.config;

import com.flowboard.comment.dto.CardResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "card-service", url = "${card.service.url}")
public interface CardServiceClient {
    // Internal endpoint — no JWT required (see card-service SecurityConfig)
    @GetMapping("/cards/internal/{cardId}/exists")
    CardResponseDTO getCardById(@PathVariable("cardId") Long cardId);
}
