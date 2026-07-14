package com.jejulocaltime.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class NtsApiDto {

    private NtsApiDto() {
    }

    public record ValidateRequest(List<BusinessEntry> businesses) {
    }

    public record BusinessEntry(
            @JsonProperty("b_no") String businessNumber,
            @JsonProperty("start_dt") String openDate,
            @JsonProperty("p_nm") String representativeName,
            @JsonProperty("p_nm2") String representativeName2,
            @JsonProperty("b_nm") String businessName,
            @JsonProperty("corp_no") String corporationNumber,
            @JsonProperty("b_sector") String businessSector,
            @JsonProperty("b_type") String businessType
    ) {
        public static BusinessEntry of(String businessNumber, String openDate, String representativeName) {
            return new BusinessEntry(businessNumber, openDate, representativeName, "", "", "", "", "");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidateResponse(
            @JsonProperty("status_code") String statusCode,
            List<ValidateResult> data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValidateResult(
            @JsonProperty("b_no") String businessNumber,
            String valid,
            @JsonProperty("valid_msg") String validMessage
    ) {
    }

    public record StatusRequest(@JsonProperty("b_no") List<String> businessNumbers) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusResponse(
            @JsonProperty("status_code") String statusCode,
            List<StatusResult> data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusResult(
            @JsonProperty("b_no") String businessNumber,
            @JsonProperty("b_stt") String businessStatusName,
            @JsonProperty("b_stt_cd") String businessStatusCode,
            @JsonProperty("tax_type") String taxType,
            @JsonProperty("end_dt") String closedDate
    ) {
    }
}
