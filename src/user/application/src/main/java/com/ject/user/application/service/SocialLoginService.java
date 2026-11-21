package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.SignupIdentity;
import com.ject.user.application.dto.SocialLoginResult;
import com.ject.user.application.dto.SocialUser;
import com.ject.user.application.exception.RejoinNotAvailableException;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.application.port.SignupTokenProvider;
import com.ject.user.application.port.SocialIdHasher;
import com.ject.user.application.port.SocialUserClient;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.MemberBannedException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class SocialLoginService {

    private final SocialUserClient socialUserClient;
    private final SocialIdHasher socialIdHasher;
    private final MemberRepository memberRepository;
    private final SignupTokenProvider signupTokenProvider;
    private final LoginTokenIssuer loginTokenIssuer;
    private final ServiceTime serviceTime;

    public SocialLoginService(SocialUserClient socialUserClient, SocialIdHasher socialIdHasher,
                              MemberRepository memberRepository, SignupTokenProvider signupTokenProvider,
                              LoginTokenIssuer loginTokenIssuer, ServiceTime serviceTime) {
        this.socialUserClient = socialUserClient;
        this.socialIdHasher = socialIdHasher;
        this.memberRepository = memberRepository;
        this.signupTokenProvider = signupTokenProvider;
        this.loginTokenIssuer = loginTokenIssuer;
        this.serviceTime = serviceTime;
    }

    /**
     * 기존 회원이면 토큰을 발급하고, 처음이면 가입 토큰을 준다 (AU-1, A-02-1 · A-03-1).
     * 서로 다른 소셜 계정은 별개 회원이고, 탈퇴 후 7일 안에는 같은 소셜 계정으로 다시 가입할 수 없다.
     * 소셜 서버를 부르는 동안 DB 연결을 잡지 않도록 이 메서드에는 트랜잭션을 두지 않는다. 쓰기는 토큰 발급뿐이다.
     *
     * @param provider SocialProvider 이름 (대소문자 무시)
     */
    public SocialLoginResult login(String provider, String socialAccessToken, String deviceId) {
        SocialUser user = socialUserClient.fetch(SocialProvider.valueOf(provider.toUpperCase(Locale.ROOT)),
                socialAccessToken);
        String socialIdHash = socialIdHasher.hash(user.socialId());
        Optional<Member> member = memberRepository.findActiveBySocialIdHash(user.provider(), socialIdHash);
        if (member.isPresent()) {
            if (member.get().isBanned()) {
                throw new MemberBannedException();
            }
            return SocialLoginResult.login(loginTokenIssuer.issue(member.get(), deviceId));
        }
        ensureRejoinAvailable(user.provider(), socialIdHash);
        return SocialLoginResult.signupRequired(
                signupTokenProvider.issue(new SignupIdentity(user.provider(), socialIdHash)), user);
    }

    private void ensureRejoinAvailable(SocialProvider provider, String socialIdHash) {
        memberRepository.findLatestWithdrawnBySocialIdHash(provider, socialIdHash)
                .filter(withdrawn -> !withdrawn.canRejoin(serviceTime.now()))
                .ifPresent(withdrawn -> {
                    throw new RejoinNotAvailableException(serviceTime.toOffset(withdrawn.rejoinAvailableAt()));
                });
    }
}
