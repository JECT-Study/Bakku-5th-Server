package com.ject.user.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.BlockApi;
import com.ject.user.api.dto.request.BlockRequest;
import com.ject.user.api.dto.response.BlockResponse;
import com.ject.user.api.dto.response.BlockedMemberResponse;
import com.ject.user.api.dto.response.CursorPageResponse;
import com.ject.user.application.dto.BlockResult;
import com.ject.user.application.dto.BlockedMemberPage;
import com.ject.user.application.service.BlockService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ApiResponse<CursorPageResponse<BlockedMemberResponse>> getBlocks(Long memberId, String cursor,
                                                                           Integer size) {
        BlockedMemberPage page = blockService.getBlocks(memberId, BlockCursor.decode(cursor), size);
        List<BlockedMemberResponse> content = page.content().stream()
                .map(member -> new BlockedMemberResponse(member.userId(), member.nickname(),
                        serviceTime.toOffset(member.blockedAt())))
                .toList();
        return ApiResponse.success(new CursorPageResponse<>(content, BlockCursor.encode(page.nextCursorId()),
                page.hasNext()));
    }

    @Override
    public ApiResponse<Void> unblock(Long memberId, Long userId) {
        blockService.unblock(memberId, userId);
        return ApiResponse.success(null);
    }
}
