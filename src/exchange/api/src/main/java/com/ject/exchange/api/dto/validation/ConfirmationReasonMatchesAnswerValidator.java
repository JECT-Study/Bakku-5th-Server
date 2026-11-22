package com.ject.exchange.api.dto.validation;

import com.ject.exchange.api.dto.enums.ConfirmationAnswer;
import com.ject.exchange.api.dto.enums.NotMetReason;
import com.ject.exchange.api.dto.request.ConfirmationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConfirmationReasonMatchesAnswerValidator
        implements ConstraintValidator<ConfirmationReasonMatchesAnswer, ConfirmationRequest> {

    @Override
    public boolean isValid(ConfirmationRequest request, ConstraintValidatorContext context) {
        // 답이 없으면 @NotNull이 알린다
        if (request == null || request.answer() == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        if (request.answer() == ConfirmationAnswer.NOT_MET && request.notMetReason() == null) {
            valid = reject(context, "notMetReason", "못 만난 이유를 선택해 주세요.");
        }
        if (request.answer() == ConfirmationAnswer.DONE && request.notMetReason() != null) {
            valid = reject(context, "notMetReason", "교환했어요에는 이유를 보내지 않습니다.");
        }
        if (request.notMetReason() != NotMetReason.OTHER && request.notMetDetail() != null) {
            valid = reject(context, "notMetDetail", "직접 입력은 기타 이유에만 보낼 수 있습니다.");
        }
        return valid;
    }

    /**
     * 위반을 해당 필드에 붙여 errors[].field에 필드 이름이 나가게 한다 (api 규약)
     */
    private static boolean reject(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
