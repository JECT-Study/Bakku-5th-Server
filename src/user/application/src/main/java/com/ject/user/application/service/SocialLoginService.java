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
    private final AppleTokenService appleTokenService;
    private final ServiceTime serviceTime;

    public SocialLoginService(SocialUserClient socialUserClient, SocialIdHasher socialIdHasher,
                              MemberRepository memberRepository, SignupTokenProvider signupTokenProvider,
                              LoginTokenIssuer loginTokenIssuer, AppleTokenService appleTokenService,
                              ServiceTime serviceTime) {
        this.socialUserClient = socialUserClient;
        this.socialIdHasher = socialIdHasher;
        this.memberRepository = memberRepository;
        this.signupTokenProvider = signupTokenProvider;
        this.loginTokenIssuer = loginTokenIssuer;
        this.appleTokenService = appleTokenService;
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
        return loginOrSignup(user, socialIdHasher.hash(user.socialId()), deviceId);
    }

    /**
     * Apple 로그인. 로그인 · 가입 확인이 끝나면 authorization code를 Apple 리프레시 토큰으로 바꿔 둔다.
     * 가입 전이어도 받아 둔다 (authorization code는 한 번만 쓸 수 있고 금방 만료돼서 가입 완료 때는 쓸 수 없음).
     * 토큰을 받지 못해도 로그인은 그대로 진행한다.
     */
    public SocialLoginResult loginWithApple(String identityToken, String authorizationCode, String deviceId) {
        SocialUser user = socialUserClient.fetch(SocialProvider.APPLE, identityToken);
        String socialIdHash = socialIdHasher.hash(user.socialId());
        SocialLoginResult result = loginOrSignup(user, socialIdHash, deviceId);
        appleTokenService.keep(socialIdHash, authorizationCode);
        return result;
    }

    private SocialLoginResult loginOrSignup(SocialUser user, String socialIdHash, String deviceId) {
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
