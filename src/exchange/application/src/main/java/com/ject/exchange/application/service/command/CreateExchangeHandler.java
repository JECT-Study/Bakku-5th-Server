package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.exchange.application.dto.command.CreateExchangeCommand;
import com.ject.exchange.application.dto.dto.ChatRoomInfo;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.ChatRoomPort;
import com.ject.exchange.application.port.ExchangeImageRejectedException;
import com.ject.exchange.application.port.ImagePort;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.application.port.UserPort;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferType;
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
    private final ImagePort imagePort;
    private final ChatRoomPort chatRoomPort;

    @Transactional
    public Long handle(CreateExchangeCommand command) {
        PostInfo post = requestablePostOf(command.postId());

        ChatRoomInfo chatRoomLink = chatRoomPort.lockAndFind(command.postId(), command.requesterId());
        if (chatRoomLink.status() == ChatRoomInfo.Status.ACTIVE) {
            // 동일 게시글·동일 요청자의 현재 방이 이미 있으면 새로 만들지 않고 기존 교환을 돌려준다 (spec 결정 3)
            return chatRoomLink.exchangeId();
        }
        if (chatRoomLink.status() == ChatRoomInfo.Status.LEFT) {
            throw new ExchangeException(ExchangeErrorCode.LEFT_CHAT_ROOM_REQUEST);
        }

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
        Exchange saved = exchangeRepository.save(exchange);
        List<String> newItemImageKeys = null;
        if (saved.getOfferType() == OfferType.NEW) {
            // 정식 사진 경로에 교환 ID가 들어가므로 저장한 뒤 확정한다 (spec 결정 4). 거부되면 트랜잭션째 롤백된다
            newItemImageKeys = attachImages(command.requesterId(), saved.getId(), command.newItem().imageKeys());
            saved = exchangeRepository.save(saved.withNewItemImages(newItemImageKeys));
        }
        createChatRoom(saved.getId(), command.postId(), command.requesterId(), post.ownerId(), newItemImageKeys);
        return saved.getId();
    }

    /**
     * 채팅방 생성 실패는 삼키지 않는다. 외부 트랜잭션이 교환·사진 확정까지 함께 롤백한다 (AC-8).
     * 다만 NEW 방식으로 이미 확정한 이미지 객체는 저장소 쪽 변경이라 DB 롤백으로 되돌아가지 않으므로 보상 삭제한다
     */
    private void createChatRoom(Long exchangeId, Long postId, Long requesterId, Long ownerId,
                                List<String> newItemImageKeys) {
        try {
            chatRoomPort.create(exchangeId, postId, requesterId, ownerId);
        } catch (RuntimeException failure) {
            if (newItemImageKeys != null && !newItemImageKeys.isEmpty()) {
                discardNewItemImages(exchangeId, newItemImageKeys, failure);
            }
            throw failure;
        }
    }

    private void discardNewItemImages(Long exchangeId, List<String> imageKeys, RuntimeException originalFailure) {
        try {
            imagePort.discardFromExchange(exchangeId, imageKeys);
        } catch (RuntimeException discardFailure) {
            originalFailure.addSuppressed(discardFailure);
        }
    }

    private List<String> attachImages(Long requesterId, Long exchangeId, List<String> imageKeys) {
        try {
            return imagePort.attachToExchange(requesterId, exchangeId, imageKeys);
        } catch (ExchangeImageRejectedException rejected) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_IMAGE_KEY,
                    rejected.getOrder() + "번째 이미지를 사용할 수 없습니다.");
        }
    }

    /**
     * 요청할 글. 화면이 삭제 · 예약중 · 교환 완료에 각기 다른 안내를 띄우므로 에러를 나눈다 (C-07-1 예외, spec 결정 1 · 5)
     */
    private PostInfo requestablePostOf(Long postId) {
        PostInfo post = postPort.findById(postId)
                .filter(found -> found.status() != PostInfo.Status.DELETED)
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.POST_NOT_FOUND));
        switch (post.status()) {
            case AVAILABLE -> { }
            case RESERVED -> throw new ExchangeException(ExchangeErrorCode.POST_RESERVED_ELSEWHERE);
            case COMPLETED, DELETED -> throw new ExchangeException(ExchangeErrorCode.POST_NOT_REQUESTABLE);
        }
        return post;
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
