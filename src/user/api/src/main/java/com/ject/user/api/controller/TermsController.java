package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.config.TermsProperties;
import com.ject.user.api.controller.api.TermsApi;
import com.ject.user.api.dto.enums.TermsConsent;
import com.ject.user.api.dto.enums.TermsType;
import com.ject.user.api.dto.response.TermsListResponse;
import com.ject.user.api.dto.response.TermsResponse;
import com.ject.user.application.dto.TermsInfo;
import com.ject.user.application.service.TermsService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TermsController implements TermsApi {

    private final TermsService termsService;
    private final TermsProperties termsProperties;

    public TermsController(TermsService termsService, TermsProperties termsProperties) {
        this.termsService = termsService;
        this.termsProperties = termsProperties;
    }

    @Override
    public ApiResponse<TermsListResponse> getTerms() {
        return ApiResponse.success(new TermsListResponse(termsService.getCurrentTerms().stream()
                .map(this::toResponse)
                .toList()));
    }

    private TermsResponse toResponse(TermsInfo terms) {
        String url = terms.documentPath() == null
                ? null
                : termsProperties.documentBaseUrl() + "/terms/" + terms.documentPath() + "/" + terms.version();
        return new TermsResponse(TermsType.valueOf(terms.termsType()), terms.title(), terms.version(),
                TermsConsent.valueOf(terms.signupConsent()), url, terms.effectiveAt());
    }
}
