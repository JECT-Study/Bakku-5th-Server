package com.ject.user.api.controller;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.response.ApiResponse;
import com.ject.common.web.response.ApiResponse.FieldError;
import com.ject.user.api.controller.api.AuthApi;
import com.ject.user.api.dto.enums.Gender;
import com.ject.user.api.dto.enums.LoginResult;
import com.ject.user.api.dto.request.LogoutRequest;
import com.ject.user.api.dto.request.SignupRequest;
import com.ject.user.api.dto.request.SocialLoginRequest;
import com.ject.user.api.dto.request.TokenRefreshRequest;
import com.ject.user.api.dto.response.LoginMemberResponse;
import com.ject.user.api.dto.response.LoginResponse;
import com.ject.user.api.dto.response.SignupPrefillResponse;
import com.ject.user.api.dto.response.TokenResponse;
import com.ject.user.application.dto.LoginTokens;
import com.ject.user.application.dto.SocialLoginResult;
import com.ject.user.application.service.SocialLoginService;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class AuthController implements AuthApi {

    private final SocialLoginService socialLoginService;
    private final ServiceTime serviceTime;

    public AuthController(SocialLoginService socialLoginService, ServiceTime serviceTime) {
        this.socialLoginService = socialLoginService;
        this.serviceTime = serviceTime;
    }

    @Override
    public ApiResponse<LoginResponse> login(String provider, SocialLoginRequest request) {
        List<FieldError> errors = missingTokenFields(provider, request);
        if (!errors.isEmpty()) {
            CommonErrorCode errorCode = CommonErrorCode.INVALID_INPUT;
            return ApiResponse.error(errorCode, errorCode.getMessage(), errors);
        }

        String token = "apple".equals(provider) ? request.identityToken() : request.socialAccessToken();
        SocialLoginResult result = socialLoginService.login(provider, token, request.deviceId());
        if (result.signupRequired()) {
            SignupPrefillResponse prefill = new SignupPrefillResponse(result.prefillBirthDate(),
                    result.prefillGender() == null ? null : Gender.valueOf(result.prefillGender()));
            return ApiResponse.success(new LoginResponse(LoginResult.SIGNUP_REQUIRED, null, null, null, null,
                    result.signupToken().value(), serviceTime.toOffset(result.signupToken().expiresAt()), prefill));
        }

        LoginTokens tokens = result.tokens();
        return ApiResponse.success(new LoginResponse(LoginResult.LOGIN, tokens.accessToken().value(),
                serviceTime.toOffset(tokens.accessToken().expiresAt()), tokens.refreshToken(),
                new LoginMemberResponse(tokens.memberId(), tokens.nickname()), null, null, null));
    }

    @Override
    public ApiResponse<TokenResponse> signup(SignupRequest request) {
        TokenResponse token = UserStubData.token();
        return ApiResponse.created(new TokenResponse(token.accessToken(), token.accessTokenExpiresAt(), token.refreshToken(),
                new LoginMemberResponse(UserStubData.MY_ID, request.nickname())));
    }

    @Override
    public ApiResponse<TokenResponse> refresh(TokenRefreshRequest request) {
        return ApiResponse.success(UserStubData.token());
    }

    @Override
    public ApiResponse<Void> logout(LogoutRequest request) {
        return ApiResponse.success(null);
    }

    private static List<FieldError> missingTokenFields(String provider, SocialLoginRequest request) {
        List<FieldError> errors = new ArrayList<>();
        if ("apple".equals(provider)) {
            if (isBlank(request.identityToken())) {
                errors.add(new FieldError("identityToken", "apple은 필수 값입니다."));
            }
            if (isBlank(request.authorizationCode())) {
                errors.add(new FieldError("authorizationCode", "apple은 필수 값입니다."));
            }
        } else if (isBlank(request.socialAccessToken())) {
            errors.add(new FieldError("socialAccessToken", "naver · kakao는 필수 값입니다."));
        }
        return errors;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
