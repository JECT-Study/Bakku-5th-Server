package com.ject.exchange.api.controller;

import com.ject.common.response.ApiResponse;
import com.ject.exchange.api.controller.api.AppointmentChangeApi;
import com.ject.exchange.api.dto.enums.AppointmentChangeStatus;
import com.ject.exchange.api.dto.enums.AppointmentStatus;
import com.ject.exchange.api.dto.enums.AppointmentTimeType;
import com.ject.exchange.api.dto.request.ProposeAppointmentChangeRequest;
import com.ject.exchange.api.dto.response.AppointmentChangeResponse;
import com.ject.exchange.api.dto.response.AppointmentResponse;
import org.springframework.web.bind.annotation.RestController;

import static com.ject.exchange.api.controller.ExchangeStubData.APPOINTMENT_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.CHANGED_SCHEDULED_AT;
import static com.ject.exchange.api.controller.ExchangeStubData.CHANGE_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.CHANGE_OFFERED_ITEM_IDS;
import static com.ject.exchange.api.controller.ExchangeStubData.CHANGE_REQUESTED_AT;
import static com.ject.exchange.api.controller.ExchangeStubData.EXCHANGE_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.OWNER_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.REQUESTER_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.at;
import static com.ject.exchange.api.controller.ExchangeStubData.confirmedAppointment;
import static com.ject.exchange.api.controller.ExchangeStubData.place;

@RestController
public class AppointmentChangeController implements AppointmentChangeApi {

    @Override
    public ApiResponse<AppointmentChangeResponse> proposeAppointmentChange(Long appointmentId,
                                                                           ProposeAppointmentChangeRequest request) {
        return ApiResponse.created(new AppointmentChangeResponse(
                CHANGE_ID, appointmentId, REQUESTER_ID, AppointmentChangeStatus.PENDING,
                AppointmentTimeType.SCHEDULED, CHANGED_SCHEDULED_AT, place(), CHANGE_OFFERED_ITEM_IDS,
                CHANGE_REQUESTED_AT, null));
    }

    @Override
    public ApiResponse<AppointmentResponse> acceptAppointmentChange(Long changeId) {
        return ApiResponse.success(confirmedAppointment(
                APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.CONFIRMED, CHANGED_SCHEDULED_AT, 30, null, null));
    }

    @Override
    public ApiResponse<AppointmentChangeResponse> rejectAppointmentChange(Long changeId) {
        return ApiResponse.success(new AppointmentChangeResponse(
                changeId, APPOINTMENT_ID, OWNER_ID, AppointmentChangeStatus.REJECTED,
                AppointmentTimeType.SCHEDULED, CHANGED_SCHEDULED_AT, place(), CHANGE_OFFERED_ITEM_IDS,
                CHANGE_REQUESTED_AT, at(27, 10, 20)));
    }
}
