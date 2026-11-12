package com.ject.user.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.DevTokenApi;
import com.ject.user.api.dto.request.DevTokenRequest;
import com.ject.user.api.dto.response.DevTokenResponse;
import com.ject.user.api.dto.response.LoginMemberResponse;
import com.ject.user.application.dto.DevToken;
import com.ject.user.application.service.DevTokenService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

/**
 * local · dev 프로필에서만 등록된다. staging · 운영에는 이 경로가 없다.
 */
@RestController
@Profile({"local", "dev"})
public class DevTokenController implements DevTokenApi {

    private final DevTokenService devTokenService;
    private final ServiceTime serviceTime;

    public DevTokenController(DevTokenService devTokenService, ServiceTime serviceTime) {
        this.devTokenService = devTokenService;
        this.serviceTime = serviceTime;
    }

    @Override
    public ApiResponse<DevTokenResponse> issueToken(DevTokenRequest request) {
        DevToken token = devTokenService.issue(request == null ? null : request.nickname());
        return ApiResponse.success(new DevTokenResponse(
                token.accessToken().value(),
                serviceTime.toOffset(token.accessToken().expiresAt()),
                new LoginMemberResponse(token.memberId(), token.nickname())
        ));
    }
}
