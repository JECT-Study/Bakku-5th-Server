package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.LoginTokens;
import com.ject.user.application.dto.SignupCommand;
import com.ject.user.application.dto.SignupIdentity;
import com.ject.user.application.exception.RejoinNotAvailableException;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.application.port.SignupTokenProvider;
import com.ject.user.application.port.TermsAgreementRepository;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.TermsAgreement;
import com.ject.user.domain.domain.TermsCatalog;
import com.ject.user.domain.domain.TermsCatalog.SignupConsent;
import com.ject.user.domain.domain.TermsType;
import com.ject.user.domain.exception.AlreadySignedUpException;
import com.ject.user.domain.exception.DuplicateNicknameException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SignupService {

    private final SignupTokenProvider signupTokenProvider;
    private final MemberRepository memberRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final LoginTokenIssuer loginTokenIssuer;
    private final ServiceTime serviceTime;

    public SignupService(SignupTokenProvider signupTokenProvider, MemberRepository memberRepository,
                         TermsAgreementRepository termsAgreementRepository, LoginTokenIssuer loginTokenIssuer,
                         ServiceTime serviceTime) {
        this.signupTokenProvider = signupTokenProvider;
        this.memberRepository = memberRepository;
        this.termsAgreementRepository = termsAgreementRepository;
        this.loginTokenIssuer = loginTokenIssuer;
        this.serviceTime = serviceTime;
    }

    /**
     * 약관 동의 · 프로필로 회원을 만들고 토큰을 발급한다 (AU-2, A-05-3).
     * 만 14세 확인은 소셜 값이 아니라 받은 생년월일로 한국 시간 기준 다시 한다.
     * 7일이 지난 재가입이면 이전 회원 id를 연결해 기록을 이어서 적용한다 (결정 A4-3).
     */
    @Transactional
    public LoginTokens signup(SignupCommand command) {
        SignupIdentity identity = signupTokenProvider.verify(command.signupToken());
        if (memberRepository.findActiveBySocialIdHash(identity.provider(), identity.socialIdHash()).isPresent()) {
            throw new AlreadySignedUpException();
        }
        Instant now = serviceTime.now();
        Long previousMemberId = previousMemberIdOf(identity, now);
        List<TermsAgreement> agreements = TermsCatalog.signupAgreements(consentsOf(command), now);

        Nickname nickname = new Nickname(command.nickname());
        if (memberRepository.findByNickname(nickname).isPresent()) {
            throw new DuplicateNicknameException();
        }
        Member member = memberRepository.save(Member.create(identity.provider(), identity.socialIdHash(), nickname,
                new BirthDate(command.birthDate()), genderOf(command.gender()), marketingAgreed(agreements),
                previousMemberId, now, serviceTime.today()));
        termsAgreementRepository.saveAll(member.getId(), agreements);
        return loginTokenIssuer.issue(member, command.deviceId());
    }

    /**
     * 가입 토큰은 7일 동안 유효하므로, 토큰을 받은 뒤 탈퇴한 경우까지 여기서 다시 확인한다.
     */
    private Long previousMemberIdOf(SignupIdentity identity, Instant now) {
        Optional<Member> withdrawn = memberRepository.findLatestWithdrawnBySocialIdHash(identity.provider(),
                identity.socialIdHash());
        if (withdrawn.isEmpty()) {
            return null;
        }
        if (!withdrawn.get().canRejoin(now)) {
            throw new RejoinNotAvailableException(serviceTime.toOffset(withdrawn.get().rejoinAvailableAt()));
        }
        return withdrawn.get().getId();
    }

    private static Map<TermsType, SignupConsent> consentsOf(SignupCommand command) {
        return command.agreements().stream()
                .collect(Collectors.toMap(
                        agreement -> TermsType.valueOf(agreement.termsType()),
                        agreement -> new SignupConsent(agreement.version(), agreement.agreed()),
                        (first, duplicate) -> first));
    }

    private static boolean marketingAgreed(List<TermsAgreement> agreements) {
        return agreements.stream()
                .anyMatch(agreement -> agreement.type() == TermsType.MARKETING && agreement.agreed());
    }

    private static Gender genderOf(String gender) {
        return gender == null ? null : Gender.valueOf(gender);
    }
}
