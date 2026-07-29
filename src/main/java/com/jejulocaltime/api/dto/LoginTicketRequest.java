package com.jejulocaltime.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginTicketRequest(@NotBlank String ticket) {
}
