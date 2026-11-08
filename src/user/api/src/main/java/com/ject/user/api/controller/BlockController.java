package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.BlockApi;
import com.ject.user.api.dto.request.BlockRequest;
import com.ject.user.api.dto.response.BlockResponse;
import com.ject.user.api.dto.response.BlockedMemberResponse;
import com.ject.user.api.dto.response.CursorPageResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.user.api.controller.UserStubData.NOW;

@RestController
public class BlockController implements BlockApi {

    @Override
    public ApiResponse<BlockResponse> block(BlockRequest request) {
        return ApiResponse.created(new BlockResponse(request.userId(), NOW));
    }

    @Override
    public ApiResponse<CursorPageResponse<BlockedMemberResponse>> getBlocks(String cursor, Integer size) {
        return ApiResponse.success(new CursorPageResponse<>(List.of(
                new BlockedMemberResponse(202L, "하치와레러버", NOW),
                new BlockedMemberResponse(203L, "우사기", NOW.minusDays(3))), null, false));
    }

    @Override
    public ApiResponse<Void> unblock(Long userId) {
        return ApiResponse.success(null);
    }
}
