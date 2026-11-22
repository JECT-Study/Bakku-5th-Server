package com.ject.user.domain.domain;

import com.ject.user.domain.exception.SelfReportException;

import java.time.Instant;
import java.util.Objects;

/**
 * 신고. 같은 신고자는 같은 대상을 한 번만 신고한다 (C-09-1, 결정 B11).
 * 1차는 회원 신고만 신고당한 사람을 알 수 있어 채운다. 교환글 · 첨부 아이템 · 채팅방은 2차에 대상 확인과 함께 채운다.
 */
public class Report {

    private final Long id;
    private final Long reporterId;
    private final ReportTargetType targetType;
    private final Long targetId;
    private final ReportReason reason;
    private final String detail;
    private final Long reportedMemberId;
    private final Instant reportedAt;

    private Report(Long id, Long reporterId, ReportTargetType targetType, Long targetId, ReportReason reason,
                   String detail, Long reportedMemberId, Instant reportedAt) {
        this.id = id;
        this.reporterId = Objects.requireNonNull(reporterId);
        this.targetType = Objects.requireNonNull(targetType);
        this.targetId = Objects.requireNonNull(targetId);
        this.reason = Objects.requireNonNull(reason);
        this.detail = detail;
        this.reportedMemberId = reportedMemberId;
        this.reportedAt = Objects.requireNonNull(reportedAt);
    }

    /**
     * 기타 사유가 아니면 입력 내용은 저장하지 않는다.
     *
     * @throws SelfReportException 나 자신을 신고할 때 (REPORT_004)
     */
    public static Report create(Long reporterId, ReportTargetType targetType, Long targetId, ReportReason reason,
                                String detail, Instant now) {
        boolean member = targetType == ReportTargetType.MEMBER;
        if (member && Objects.equals(reporterId, targetId)) {
            throw new SelfReportException(reporterId);
        }
        String savedDetail = reason == ReportReason.OTHER && detail != null ? detail.strip() : null;
        return new Report(null, reporterId, targetType, targetId, reason, savedDetail, member ? targetId : null, now);
    }

    public Long getId() {
        return id;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public ReportReason getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    public Long getReportedMemberId() {
        return reportedMemberId;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }
}
