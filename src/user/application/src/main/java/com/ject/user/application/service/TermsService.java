package com.ject.user.application.service;

import com.ject.user.application.dto.TermsInfo;
import com.ject.user.domain.domain.TermsCatalog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TermsService {

    public List<TermsInfo> getCurrentTerms() {
        return TermsCatalog.current().stream()
                .map(TermsInfo::from)
                .toList();
    }
}
