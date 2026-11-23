package com.ject.exchange.api.dto.request;

import com.ject.exchange.application.dto.dto.NewItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

@Schema(description = "새로 작성한 아이템 (E-1의 newItem, E-3 요청 본문)")
public record NewItemRequest(
        @Schema(description = "업로드한 이미지 key 1~5장 (중복 불가), 첫 장이 대표. E-3은 이 교환의 정식 key와 섞을 수 있다", example = "[\"exchange/tmp/1/0b1c2d3e-4f50-6172-8394-a5b6c7d8e9f0.jpg\"]")
        @NotNull @Size(min = 1, max = 5) @UniqueElements
        List<@NotBlank String> imageKeys,

        @Schema(description = "제품명 (2~40자)", example = "치이카와 아크릴 스탠드")
        @NotBlank @Size(min = 2, max = 40)
        String name,

        @Schema(description = "개봉 여부. true 개봉 / false 미개봉", example = "false")
        @NotNull
        Boolean opened,

        @Schema(description = "자세한 설명 (0~1,000자)", example = "가챠에서 두 개 나와서 하나 교환해요")
        @Size(max = 1000)
        String description
) {
        public NewItem toNewItem() {
                return new NewItem(name, opened, description, imageKeys);
        }
}
