package com.ject.user.api.controller;

import com.ject.common.core.exception.CommonErrorCode;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.ject.user.api.controller.UserStubData.NOW;

@RestController
public class AuthController implements AuthApi {

    private static final String NEW_MEMBER_TOKEN_PREFIX = "new";

    @Override
    public ApiResponse<LoginResponse> login(String provider, SocialLoginRequest request) {
        List<FieldError> errors = missingTokenFields(provider, request);
        if (!errors.isEmpty()) {
            CommonErrorCode errorCode = CommonErrorCode.INVALID_INPUT;
            return ApiResponse.error(errorCode, errorCode.getMessage(), errors);
        }

        String token = "apple".equals(provider) ? request.identityToken() : request.socialAccessToken();
        if (token.startsWith(NEW_MEMBER_TOKEN_PREFIX)) {
            SignupPrefillResponse prefill = "naver".equals(provider)
                    ? new SignupPrefillResponse(LocalDate.of(2001, 3, 15), Gender.FEMALE)
                    : new SignupPrefillResponse(null, null);
            return ApiResponse.success(new LoginResponse(LoginResult.SIGNUP_REQUIRED, null, null, null, null,
                    "eyJhbGciOiJIUzI1NiJ9.signup", NOW.plusDays(7), prefill));
        }

        TokenResponse issued = UserStubData.token();
        return ApiResponse.success(new LoginResponse(LoginResult.LOGIN, issued.accessToken(), issued.accessTokenExpiresAt(),
                issued.refreshToken(), issued.member(), null, null, null));
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
