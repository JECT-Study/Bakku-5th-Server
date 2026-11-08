package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.TermsApi;
import com.ject.user.api.dto.enums.TermsConsent;
import com.ject.user.api.dto.enums.TermsType;
import com.ject.user.api.dto.response.TermsListResponse;
import com.ject.user.api.dto.response.TermsResponse;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class TermsController implements TermsApi {

    private static final String VERSION = "1.0";
    private static final LocalDate EFFECTIVE_AT = LocalDate.of(2026, 11, 27);

    @Override
    public ApiResponse<TermsListResponse> getTerms() {
        return ApiResponse.success(new TermsListResponse(List.of(
                terms(TermsType.SERVICE, "서비스 이용약관", TermsConsent.REQUIRED, "service"),
                terms(TermsType.PRIVACY_COLLECTION, "개인정보 수집 · 이용 동의", TermsConsent.REQUIRED, "privacy-collection"),
                terms(TermsType.AGE_14, "만 14세 이상입니다", TermsConsent.REQUIRED, null),
                terms(TermsType.MARKETING, "마케팅 정보 수신 동의 (선택)", TermsConsent.OPTIONAL, "marketing"),
                terms(TermsType.PRIVACY_POLICY, "개인정보 처리방침", TermsConsent.NONE, "privacy-policy"))));
    }

    private static TermsResponse terms(TermsType type, String title, TermsConsent consent, String path) {
        String url = path == null ? null : "https://{도메인}/terms/" + path + "/" + VERSION;
        return new TermsResponse(type, title, VERSION, consent, url, EFFECTIVE_AT);
    }
}
