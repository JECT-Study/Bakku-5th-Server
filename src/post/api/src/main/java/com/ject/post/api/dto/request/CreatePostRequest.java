package com.ject.post.api.dto.request;

import com.ject.post.api.dto.enums.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Schema(description = "교환글 작성")
public record CreatePostRequest(
        @Schema(description = "업로드된 key 1~5개. 첫 장이 대표, 중복 불가", example = "[\"post/tmp/3/8f14e45f.jpg\", \"post/tmp/3/c9f0f895.jpg\"]")
        @NotNull @Size(min = 1, max = 5)
        List<@NotBlank String> imageKeys,

        @Schema(description = "제품명 2~40자", example = "치이카와 하치와레 가챠 키링")
        @NotBlank @Size(min = 2, max = 40)
        String title,

        @Schema(description = "카테고리", example = "CHARACTER")
        @NotNull
        PostCategory category,

        @Schema(description = "개봉 여부. true 개봉 / false 미개봉", example = "false")
        @NotNull
        Boolean opened,

        @Schema(description = "태그 최대 5개, 1개당 1~20자, 중복 불가, # 제외. 태그는 2차라 1차에는 보내지 않거나 빈 배열", example = "[\"치이카와\", \"하치와레\"]")
        @Size(max = 5)
        List<@NotBlank @Size(max = 20) @Pattern(regexp = "^[^#]+$", message = "#은 넣을 수 없습니다.") String> tags,

        @Schema(description = "자세한 설명 0~1,000자", example = "치이카와 가챠 3탄에서 뽑았어요.")
        @Size(max = 1000)
        String description,

        @Schema(description = "원하는 아이템 1~50자. null이거나 생략하면 '아무거나 상관없어요'", example = "치이카와 가챠 키링 (하치와레, 모몽가 제외)")
        @Size(min = 1, max = 50) @Pattern(regexp = ".*\\S.*", message = "공백만 입력할 수 없습니다.")
        String wantedItem,

        @Schema(description = "희망 장소")
        @NotNull @Valid
        PlaceRequest place,

        @Schema(description = "지금 교환 유지 시간(분). 15, 30, 60, 120, 180 중 하나. null이거나 생략하면 일반 교환", example = "60")
        Integer nowExchangeMinutes
) implements PostContentRequest {

    private static final Set<Integer> NOW_EXCHANGE_MINUTES = Set.of(15, 30, 60, 120, 180);

    @AssertTrue(message = "중복된 이미지가 있습니다.")
    @Schema(hidden = true)
    public boolean isImageKeysUnique() {
        return imageKeys == null || new HashSet<>(imageKeys).size() == imageKeys.size();
    }

    @AssertTrue(message = "중복된 태그가 있습니다.")
    @Schema(hidden = true)
    public boolean isTagsUnique() {
        return tags == null || new HashSet<>(tags).size() == tags.size();
    }

    @AssertTrue(message = "15, 30, 60, 120, 180 중 하나여야 합니다.")
    @Schema(hidden = true)
    public boolean isNowExchangeMinutesAllowed() {
        return nowExchangeMinutes == null || NOW_EXCHANGE_MINUTES.contains(nowExchangeMinutes);
    }
}
