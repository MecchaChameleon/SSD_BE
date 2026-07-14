package com.jejulocaltime.api.service;

public interface BusinessVerificationClient {

    VerificationResult verify(String businessNumber, String openDate, String representativeName);

    record VerificationResult(
            boolean valid,
            String validMessage,
            String businessStatusCode,
            String businessStatusName
    ) {
        public boolean isActiveBusiness() {
            return "01".equals(businessStatusCode);
        }
    }
}
