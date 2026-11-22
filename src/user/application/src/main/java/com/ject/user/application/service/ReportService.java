package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.ReportCommand;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.application.port.ReportRepository;
import com.ject.user.domain.domain.Report;
import com.ject.user.domain.domain.ReportReason;
import com.ject.user.domain.domain.ReportTargetType;
import com.ject.user.domain.exception.AlreadyReportedException;
import com.ject.user.domain.exception.ReportTargetNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고 (RP-1). 신고는 DB에만 쌓고 1차는 개발자가 DB에서 확인해 PM에게 전달한다 (P-06-02).
 */
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ServiceTime serviceTime;

    public ReportService(ReportRepository reportRepository, MemberRepository memberRepository,
                         ServiceTime serviceTime) {
        this.reportRepository = reportRepository;
        this.memberRepository = memberRepository;
        this.serviceTime = serviceTime;
    }

    /**
     * 1차는 회원 신고만 대상을 확인한다. 교환글 · 첨부 아이템 · 채팅방은 확인 없이 저장하고,
     * 채팅방은 채팅방 ID 기준으로 한 번만 받는다 (2차에 같은 상대 기준). 교환 물품 문제 사유 확인(REPORT_003)은 2차.
     *
     * @return 신고 id
     * @throws com.ject.user.domain.exception.SelfReportException 나 자신 (REPORT_004)
     * @throws ReportTargetNotFoundException 없는 회원 · 탈퇴한 회원 (REPORT_001)
     * @throws AlreadyReportedException 이미 신고한 대상 (REPORT_002)
     */
    @Transactional
    public Long report(ReportCommand command) {
        ReportTargetType targetType = ReportTargetType.valueOf(command.targetType());
        Report report = Report.create(command.reporterId(), targetType, command.targetId(),
                ReportReason.valueOf(command.reason()), command.detail(), serviceTime.now());
        if (targetType == ReportTargetType.MEMBER) {
            memberRepository.findById(command.targetId())
                    .filter(member -> !member.isWithdrawn())
                    .orElseThrow(() -> new ReportTargetNotFoundException(targetType.name(), command.targetId()));
        }
        return reportRepository.saveIfAbsent(report)
                .orElseThrow(() -> new AlreadyReportedException(targetType.name(), command.targetId()));
    }
}
