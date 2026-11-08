package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.MemberApi;
import com.ject.user.api.dto.enums.Gender;
import com.ject.user.api.dto.enums.NicknameUnavailableReason;
import com.ject.user.api.dto.enums.NoShowStatus;
import com.ject.user.api.dto.request.MarketingConsentRequest;
import com.ject.user.api.dto.request.NicknameUpdateRequest;
import com.ject.user.api.dto.request.WithdrawalRequest;
import com.ject.user.api.dto.response.CursorPageResponse;
import com.ject.user.api.dto.response.MarketingConsentResponse;
import com.ject.user.api.dto.response.MemberProfileResponse;
import com.ject.user.api.dto.response.MyInfoResponse;
import com.ject.user.api.dto.response.NicknameAvailabilityResponse;
import com.ject.user.api.dto.response.NicknameUpdateResponse;
import com.ject.user.api.dto.response.NoShowResponse;
import com.ject.user.api.dto.response.WithdrawalNoticeResponse;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.ject.user.api.controller.UserStubData.MY_ID;
import static com.ject.user.api.controller.UserStubData.MY_NICKNAME;
import static com.ject.user.api.controller.UserStubData.NOW;

@RestController
public class MemberController implements MemberApi {

    private static final Pattern NICKNAME_FORMAT = Pattern.compile("^[가-힣a-zA-Z0-9]{2,10}$");

    @Override
    public ApiResponse<NicknameAvailabilityResponse> checkNickname(String nickname) {
        if (!NICKNAME_FORMAT.matcher(nickname).matches()) {
            return ApiResponse.success(new NicknameAvailabilityResponse(false, NicknameUnavailableReason.INVALID_FORMAT, false));
        }
        if (nickname.toLowerCase(Locale.ROOT).equals(MY_NICKNAME.toLowerCase(Locale.ROOT))) {
            return ApiResponse.success(new NicknameAvailabilityResponse(false, NicknameUnavailableReason.DUPLICATE, false));
        }
        return ApiResponse.success(new NicknameAvailabilityResponse(true, null, false));
    }

    @Override
    public ApiResponse<MyInfoResponse> getMe() {
        return ApiResponse.success(new MyInfoResponse(MY_ID, MY_NICKNAME, LocalDate.of(2001, 3, 15), Gender.FEMALE, 12,
                null, false, NOW.minusDays(30), null, null, null, null, null));
    }

    @Override
    public ApiResponse<WithdrawalNoticeResponse> getWithdrawalNotice() {
        return ApiResponse.success(new WithdrawalNoticeResponse(1));
    }

    @Override
    public ApiResponse<Void> withdraw(WithdrawalRequest request) {
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<MarketingConsentResponse> updateMarketingConsent(MarketingConsentRequest request) {
        return ApiResponse.success(new MarketingConsentResponse(request.agreed(), NOW));
    }

    @Override
    public ApiResponse<NicknameUpdateResponse> updateNickname(NicknameUpdateRequest request) {
        return ApiResponse.success(new NicknameUpdateResponse(request.nickname(), NOW.plusDays(30)));
    }

    @Override
    public ApiResponse<MemberProfileResponse> getProfile(Long userId) {
        if (MY_ID.equals(userId)) {
            return ApiResponse.success(new MemberProfileResponse(MY_ID, MY_NICKNAME, 12, true, null));
        }
        return ApiResponse.success(new MemberProfileResponse(userId, "하치와레러버", 5, false, null));
    }

    @Override
    public ApiResponse<CursorPageResponse<NoShowResponse>> getNoShows(String cursor, Integer size) {
        return ApiResponse.success(new CursorPageResponse<>(List.of(
                new NoShowResponse(7L, NOW.plusDays(23), "홍대입구역 9번 출구", "하치와레러버", NoShowStatus.PENDING)), null, false));
    }
}
