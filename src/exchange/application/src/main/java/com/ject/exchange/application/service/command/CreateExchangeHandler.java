package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.exchange.application.dto.command.CreateExchangeCommand;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.application.port.UserPort;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.domain.exchange.vo.OfferedPostIds;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateExchangeHandler implements CommandHandler<CreateExchangeCommand, Long> {

    private final ExchangeRepository exchangeRepository;
    private final PostPort postPort;
    private final UserPort userPort;

    @Transactional
    public Long handle(CreateExchangeCommand command) {
        PostInfo post = postPort.findById(command.postId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.POST_NOT_FOUND));

        if (!post.isRequestable()) {
            throw new ExchangeException(ExchangeErrorCode.POST_NOT_REQUESTABLE);
        }
        // Todo: 중복 요청, 나간 채팅방 재요청 검증 (chat BC 채팅방 정보가 생기면)

        Exchange exchange =
                switch (command.offerType()) {
                    case POST -> {
                        Exchange request = Exchange.createWithPosts(
                                post.id(),
                                command.requesterId(),
                                post.ownerId(),
                                new OfferedPostIds(command.offeredPostIds())
                        );
                        // 본인 글 확인(생성 규칙) 다음, 첨부 글 확인 전에 본다 (spec 결정 3)
                        validateNotBlocked(command.requesterId(), post.ownerId());
                        validAttachedPostIds(command.requesterId(), command.offeredPostIds());
                        yield request;
                    }
                    // Todo: 이미지 tmp key 검증 및 저장
                    case NEW -> {
                        Exchange request = Exchange.createWithNewItem(
                                post.id(),
                                command.requesterId(),
                                post.ownerId(),
                                new ItemName(command.newItem().name()),
                                command.newItem().opened(),
                                new ItemDescription(command.newItem().description())
                        );
                        validateNotBlocked(command.requesterId(), post.ownerId());
                        yield request;
                    }
                };
        // Todo: 채팅방 생성
        return exchangeRepository.save(exchange).getId();
    }

    private void validAttachedPostIds(Long requesterId, List<Long> offeredPostIds) {
        List<PostInfo> postInfos = postPort.findAllByIds(offeredPostIds);
        if (postInfos.size() != offeredPostIds.size()
            || !postInfos.stream().allMatch(p -> p.isAttachableBy(requesterId))) {
            throw new ExchangeException(ExchangeErrorCode.POST_NOT_ATTACHABLE);
        }
    }

    /**
     * 요청자와 게시자 중 한쪽이라도 상대를 차단했으면 요청할 수 없다 (C-07-1 예외 "차단 관계", C-09-2)
     */
    private void validateNotBlocked(Long requesterId, Long ownerId) {
        if (userPort.isBlocked(requesterId, ownerId)) {
            throw new ExchangeException(ExchangeErrorCode.BLOCKED_USER);
        }
    }
}
