package com.ject.exchange.api.controller;

import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.response.ApiResponse;
import com.ject.exchange.api.controller.api.AppointmentApi;
import com.ject.exchange.api.dto.enums.AppointmentStatus;
import com.ject.exchange.api.dto.enums.AppointmentTimeType;
import com.ject.exchange.api.dto.enums.ConfirmationAnswer;
import com.ject.exchange.api.dto.enums.ConfirmationResult;
import com.ject.exchange.api.dto.request.AcceptAppointmentRequest;
import com.ject.exchange.api.dto.request.ConfirmationRequest;
import com.ject.exchange.api.dto.request.ProposeAppointmentRequest;
import com.ject.exchange.api.dto.request.UpdateReminderRequest;
import com.ject.exchange.api.dto.response.AppointmentResponse;
import com.ject.exchange.api.dto.response.ConfirmationResponse;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import static com.ject.exchange.api.controller.ExchangeStubData.APPOINTMENT_CREATED_AT;
import static com.ject.exchange.api.controller.ExchangeStubData.APPOINTMENT_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.EXCHANGE_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.OWNER_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.REQUESTER_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.SCHEDULED_AT;
import static com.ject.exchange.api.controller.ExchangeStubData.appointmentItems;
import static com.ject.exchange.api.controller.ExchangeStubData.at;
import static com.ject.exchange.api.controller.ExchangeStubData.confirmedAppointment;
import static com.ject.exchange.api.controller.ExchangeStubData.place;

@RestController
@RequiredArgsConstructor
public class AppointmentController implements AppointmentApi {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Override
    public ApiResponse<AppointmentResponse> proposeAppointment(Long memberId, Long exchangeId, ProposeAppointmentRequest request) {
        Long appointmentId = commandBus.send(request.toCommand(exchangeId, memberId));
        AppointmentResult result = queryBus.send(new GetAppointmentQuery(appointmentId, memberId));
        return ApiResponse.created(AppointmentResponse.from(result));
    }

    @Override
    public ApiResponse<AppointmentResponse> getAppointment(Long memberId, Long appointmentId) {
        AppointmentResult result = queryBus.send(new GetAppointmentQuery(appointmentId, memberId));
        return ApiResponse.success(AppointmentResponse.from(result));
    }

    @Override
    public ApiResponse<AppointmentResponse> getCurrentAppointment(Long exchangeId) {
        return ApiResponse.success(confirmedAppointment(
                APPOINTMENT_ID, exchangeId, AppointmentStatus.CONFIRMED, SCHEDULED_AT, 30, null, null));
    }

    @Override
    public ApiResponse<AppointmentResponse> acceptAppointment(Long appointmentId, AcceptAppointmentRequest request) {
        return ApiResponse.success(confirmedAppointment(
                appointmentId, EXCHANGE_ID, AppointmentStatus.CONFIRMED, SCHEDULED_AT, 30, null, null));
    }

    @Override
    public ApiResponse<AppointmentResponse> rejectAppointment(Long appointmentId) {
        return ApiResponse.success(closedProposal(appointmentId, AppointmentStatus.REJECTED));
    }

    @Override
    public ApiResponse<AppointmentResponse> withdrawAppointment(Long appointmentId) {
        return ApiResponse.success(closedProposal(appointmentId, AppointmentStatus.WITHDRAWN));
    }

    @Override
    public ApiResponse<AppointmentResponse> updateReminder(Long appointmentId, UpdateReminderRequest request) {
        return ApiResponse.success(confirmedAppointment(
                appointmentId, EXCHANGE_ID, AppointmentStatus.CONFIRMED, SCHEDULED_AT,
                request.reminderOffsetMinutes(), null, null));
    }

    @Override
    public ApiResponse<AppointmentResponse> respondConfirmation(Long appointmentId, ConfirmationRequest request) {
        ConfirmationResponse confirmation = new ConfirmationResponse(
                at(27, 15, 30), ConfirmationResult.COMPLETED, false, ConfirmationAnswer.DONE, at(27, 15, 42));
        return ApiResponse.success(confirmedAppointment(
                appointmentId, EXCHANGE_ID, AppointmentStatus.COMPLETED, SCHEDULED_AT, 30, null, confirmation));
    }

    private static AppointmentResponse closedProposal(Long appointmentId, AppointmentStatus status) {
        return new AppointmentResponse(
                appointmentId, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                status, AppointmentTimeType.SCHEDULED, SCHEDULED_AT, place(), appointmentItems(),
                null, 30, false,
                null, null, null,
                null, APPOINTMENT_CREATED_AT);
    }
}
