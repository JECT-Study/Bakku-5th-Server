package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.ReportCommand;
import com.ject.user.application.service.ReportService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.Report;
import com.ject.user.domain.domain.ReportReason;
import com.ject.user.domain.domain.ReportTargetType;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.AlreadyReportedException;
import com.ject.user.domain.exception.ReportTargetNotFoundException;
import com.ject.user.domain.exception.SelfReportException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T03:00:00Z");
    private static final Instant JOINED_AT = Instant.parse("2026-11-01T03:00:00Z");

    private FakeMemberRepository memberRepository;
    private FakeReportRepository reportRepository;
    private ReportService reportService;
    private Long me;
    private Long other;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        reportRepository = new FakeReportRepository();
        reportService = new ReportService(reportRepository, memberRepository,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
        me = memberRepository.save(member("쿠로미덕후")).getId();
        other = memberRepository.save(member("하치와레러버")).getId();
    }

    @Test
    @DisplayName("RP-1 회원을 신고하면 신고당한 회원으로 저장한다")
    void report_member() {
        // when
        Long reportId = reportService.report(command("MEMBER", other, "FRAUD", null));

        // then
        assertEquals(1L, reportId);
        Report saved = reportRepository.saved().get(0);
        assertEquals(ReportTargetType.MEMBER, saved.getTargetType());
        assertEquals(other, saved.getReportedMemberId());
        assertEquals(NOW, saved.getReportedAt());
    }

    @Test
    @DisplayName("RP-1 교환글 · 첨부 아이템 · 채팅방은 1차에 대상을 확인하지 않고 신고당한 회원도 비워 둔다")
    void report_otherTargets() {
        // when
        reportService.report(command("POST", 999L, "EXTERNAL_TRADE", null));
        reportService.report(command("OFFERED_ITEM", 999L, "INAPPROPRIATE_CONTENT", null));
        reportService.report(command("CHAT_ROOM", 999L, "ITEM_PROBLEM", null));

        // then
        assertEquals(3, reportRepository.saved().size());
        assertTrue(reportRepository.saved().stream().allMatch(report -> report.getReportedMemberId() == null));
    }

    @Test
    @DisplayName("RP-1 기타 사유는 앞뒤 공백을 지운 내용을 저장하고, 다른 사유의 내용은 저장하지 않는다")
    void report_detail() {
        // when
        reportService.report(command("POST", 10L, "OTHER", "  연락처를 올렸어요 "));
        reportService.report(command("POST", 11L, "FRAUD", "보내도 저장 안 함"));

        // then
        assertEquals("연락처를 올렸어요", reportRepository.saved().get(0).getDetail());
        assertNull(reportRepository.saved().get(1).getDetail());
        assertEquals(ReportReason.FRAUD, reportRepository.saved().get(1).getReason());
    }

    @Test
    @DisplayName("RP-1 같은 대상을 다시 신고하면 AlreadyReportedException")
    void report_alreadyReported() {
        // given
        reportService.report(command("CHAT_ROOM", 5L, "EXTERNAL_TRADE", null));

        // when & then
        assertThrows(AlreadyReportedException.class,
                () -> reportService.report(command("CHAT_ROOM", 5L, "FRAUD", null)));
    }

    @Test
    @DisplayName("RP-1 대상 종류가 다르면 같은 ID여도 따로 신고할 수 있다")
    void report_sameIdDifferentType() {
        // when
        reportService.report(command("POST", other, "FRAUD", null));
        reportService.report(command("MEMBER", other, "FRAUD", null));

        // then
        assertEquals(2, reportRepository.saved().size());
    }

    @Test
    @DisplayName("RP-1 나 자신을 신고하면 SelfReportException")
    void report_self() {
        assertThrows(SelfReportException.class, () -> reportService.report(command("MEMBER", me, "FRAUD", null)));
    }

    @Test
    @DisplayName("RP-1 없는 회원이면 ReportTargetNotFoundException")
    void report_unknownMember() {
        assertThrows(ReportTargetNotFoundException.class,
                () -> reportService.report(command("MEMBER", 999L, "FRAUD", null)));
    }

    @Test
    @DisplayName("RP-1 탈퇴한 회원이면 ReportTargetNotFoundException")
    void report_withdrawnMember() {
        // given
        Member withdrawn = memberRepository.findById(other).orElseThrow();
        withdrawn.withdraw(NOW);
        memberRepository.save(withdrawn);

        // when & then
        assertThrows(ReportTargetNotFoundException.class,
                () -> reportService.report(command("MEMBER", other, "FRAUD", null)));
        assertTrue(reportRepository.saved().isEmpty());
    }

    private ReportCommand command(String targetType, Long targetId, String reason, String detail) {
        return new ReportCommand(me, targetType, targetId, reason, detail);
    }

    private static Member member(String nickname) {
        return Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2001, 3, 15)), null, MemberStatus.ACTIVE, null, 0, false, JOINED_AT, null,
                null, JOINED_AT, null);
    }
}
