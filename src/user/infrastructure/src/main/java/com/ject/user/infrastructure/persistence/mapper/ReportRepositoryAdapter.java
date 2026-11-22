package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.application.port.ReportRepository;
import com.ject.user.domain.domain.Report;
import com.ject.user.infrastructure.persistence.repository.ReportJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ReportRepositoryAdapter implements ReportRepository {

    private final ReportJpaRepository reportJpaRepository;

    public ReportRepositoryAdapter(ReportJpaRepository reportJpaRepository) {
        this.reportJpaRepository = reportJpaRepository;
    }

    @Override
    public Optional<Long> saveIfAbsent(Report report) {
        return reportJpaRepository.insertIfAbsent(report.getReporterId(), report.getTargetType().name(),
                report.getTargetId(), report.getReason().name(), report.getDetail(), report.getReportedMemberId(),
                report.getReportedAt());
    }
}
