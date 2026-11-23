package com.ject.exchange.application.dto.dto;

import java.util.List;

/**
 * @param imageKeys 업로드한 사진 key 1~5장 (tmp 또는 이 교환의 정식 key), 첫 장이 대표
 */
public record NewItem(
        String name, Boolean opened, String description, List<String> imageKeys
) {}
