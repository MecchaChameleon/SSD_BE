package com.jejulocaltime.api.service;

import com.jejulocaltime.api.common.exception.BusinessException;
import com.jejulocaltime.api.common.exception.ErrorCode;
import com.jejulocaltime.api.dto.NtsApiDto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class NtsBusinessVerificationClient implements BusinessVerificationClient {

    private static final String VALID_MATCH_CODE = "01";

    private final RestClient client;
    private final String serviceKey;

    public NtsBusinessVerificationClient(
            @Value("${nts.base-url}") String baseUrl,
            @Value("${nts.service-key}") String serviceKey) {
        this.serviceKey = serviceKey;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public VerificationResult verify(String businessNumber, String openDate, String representativeName) {
        NtsApiDto.ValidateResult validateResult = validate(businessNumber, openDate, representativeName);
        boolean valid = VALID_MATCH_CODE.equals(validateResult.valid());
        String validMessage = validateResult.validMessage() != null
                ? validateResult.validMessage()
                : (valid ? "사업자등록정보가 일치합니다." : "사업자등록정보가 일치하지 않습니다.");

        if (!valid) {
            return new VerificationResult(false, validMessage, null, null);
        }

        NtsApiDto.StatusResult statusResult = fetchStatus(businessNumber);
        return new VerificationResult(
                true,
                validMessage,
                statusResult.businessStatusCode(),
                statusResult.businessStatusName());
    }

    private NtsApiDto.ValidateResult validate(String businessNumber, String openDate, String representativeName) {
        NtsApiDto.ValidateRequest request = new NtsApiDto.ValidateRequest(
                List.of(NtsApiDto.BusinessEntry.of(businessNumber, openDate, representativeName)));

        try {
            NtsApiDto.ValidateResponse response = client.post()
                    .uri(uri -> uri.path("/validate")
                            .queryParam("serviceKey", serviceKey)
                            .build())
                    .body(request)
                    .retrieve()
                    .body(NtsApiDto.ValidateResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new BusinessException(ErrorCode.NTS_SERVICE_ERROR, "국세청 진위확인 응답이 비어 있습니다.");
            }
            return response.data().get(0);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.NTS_SERVICE_ERROR, "국세청 진위확인 서버 호출에 실패했습니다.");
        }
    }

    private NtsApiDto.StatusResult fetchStatus(String businessNumber) {
        NtsApiDto.StatusRequest request = new NtsApiDto.StatusRequest(List.of(businessNumber));

        try {
            NtsApiDto.StatusResponse response = client.post()
                    .uri(uri -> uri.path("/status")
                            .queryParam("serviceKey", serviceKey)
                            .build())
                    .body(request)
                    .retrieve()
                    .body(NtsApiDto.StatusResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new BusinessException(ErrorCode.NTS_SERVICE_ERROR, "국세청 상태조회 응답이 비어 있습니다.");
            }
            return response.data().get(0);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.NTS_SERVICE_ERROR, "국세청 상태조회 서버 호출에 실패했습니다.");
        }
    }
}
