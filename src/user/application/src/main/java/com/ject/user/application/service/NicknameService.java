package com.ject.user.application.service;

import com.ject.user.application.dto.NicknameAvailability;
import com.ject.user.application.dto.NicknameAvailability.UnavailableReason;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.NicknameViolation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class NicknameService {

    private final MemberRepository memberRepository;

    public NicknameService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * 형식 · 중복을 검사한다 (A-05-2). 사용할 수 없어도 예외 대신 이유를 돌려준다.
     * 중복은 영문 대소문자를 구분하지 않고, 탈퇴 회원 닉네임은 바로 다시 쓸 수 있다 (결정 A5-0 · A5-2).
     */
    public NicknameAvailability checkAvailability(String value) {
        Optional<NicknameViolation> violation = Nickname.validate(value);
        if (violation.isPresent()) {
            return NicknameAvailability.unusable(reasonOf(violation.get()));
        }
        if (memberRepository.findByNickname(new Nickname(value)).isPresent()) {
            return NicknameAvailability.unusable(UnavailableReason.DUPLICATE);
        }
        return NicknameAvailability.usable();
    }

    private static UnavailableReason reasonOf(NicknameViolation violation) {
        return switch (violation) {
            case INVALID_LENGTH -> UnavailableReason.INVALID_LENGTH;
            case INVALID_FORMAT -> UnavailableReason.INVALID_FORMAT;
        };
    }
}
