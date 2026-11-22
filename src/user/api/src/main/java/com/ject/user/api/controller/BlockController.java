package com.ject.user.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.BlockApi;
import com.ject.user.api.dto.request.BlockRequest;
import com.ject.user.api.dto.response.BlockResponse;
import com.ject.user.api.dto.response.BlockedMemberResponse;
import com.ject.user.api.dto.response.CursorPageResponse;
import com.ject.user.application.dto.BlockResult;
import com.ject.user.application.service.BlockService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.user.api.controller.UserStubData.NOW;

@RestController
public class BlockController implements BlockApi {

    private final BlockService blockService;
    private final ServiceTime serviceTime;

    public BlockController(BlockService blockService, ServiceTime serviceTime) {
        this.blockService = blockService;
        this.serviceTime = serviceTime;
    }

    @Override
    public ApiResponse<BlockResponse> block(Long memberId, BlockRequest request) {
        BlockResult result = blockService.block(memberId, request.userId());
        BlockResponse response = new BlockResponse(result.userId(), serviceTime.toOffset(result.blockedAt()));
        return result.created() ? ApiResponse.created(response) : ApiResponse.success(response);
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
