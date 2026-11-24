package com.ject.exchange.api.controller;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.controller.api.AppointmentApi;
import com.ject.exchange.api.exception.ExchangeExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 출시 범위에서 빠진 A-7 · M-1~3 경로가 없는지 확인한다 (#117).
 */
class AppointmentRemovedPathControllerTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long CHANGE_ID = 7L;
    private static final Long MEMBER_ID = 100L;

    private FakeCommandBus commandBus;
    private FakeQueryBus queryBus;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandBus = new FakeCommandBus();
        queryBus = new FakeQueryBus(new FakeExchangeRepository());
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AppointmentController(commandBus, queryBus, IMAGE_URL_RESOLVER))
                .setControllerAdvice(new ExchangeExceptionHandler())
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    // ---- A-7 ----

    @Test
    @DisplayName("A-7 PUT /appointments/{id}/reminder는 404이고 커맨드 · 쿼리를 보내지 않는다")
    void updateReminder_removed_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/appointments/{appointmentId}/reminder", APPOINTMENT_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reminderOffsetMinutes\": 60}"))
                .andExpect(status().isNotFound());

        assertTrue(commandBus.sentCommands().isEmpty());
        assertTrue(queryBus.sentQueries().isEmpty());
    }

    @Test
    @DisplayName("A-7 AppointmentApi에 /reminder 경로 매핑이 없다")
    void appointmentApi_hasNoReminderMapping() {
        assertFalse(mappedPaths().stream().anyMatch(path -> path.endsWith("/reminder")));
    }

    // ---- M-1 ~ M-3 ----

    @Test
    @DisplayName("M-1 POST /appointments/{id}/changes는 404이고 커맨드를 보내지 않는다")
    void proposeChange_removed_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/changes", APPOINTMENT_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    @Test
    @DisplayName("M-2 POST /appointment-changes/{id}/accept는 404다")
    void acceptChange_removed_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/appointment-changes/{changeId}/accept", CHANGE_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID))
                .andExpect(status().isNotFound());

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    @Test
    @DisplayName("M-3 POST /appointment-changes/{id}/reject는 404다")
    void rejectChange_removed_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/appointment-changes/{changeId}/reject", CHANGE_ID)
                        .requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID))
                .andExpect(status().isNotFound());

        assertTrue(commandBus.sentCommands().isEmpty());
    }

    @Test
    @DisplayName("M-1~3 AppointmentApi에 /changes · /appointment-changes 경로 매핑이 없다")
    void appointmentApi_hasNoChangeMapping() {
        List<String> paths = mappedPaths();

        assertFalse(paths.stream().anyMatch(path -> path.endsWith("/changes")));
        assertFalse(paths.stream().anyMatch(path -> path.contains("appointment-changes")));
    }

    /** AppointmentApi 메서드에 붙은 경로 매핑을 모두 모은다. */
    private static List<String> mappedPaths() {
        List<String> paths = new ArrayList<>();
        for (Method method : AppointmentApi.class.getMethods()) {
            addAll(paths, method.getAnnotation(GetMapping.class) == null ? null
                    : method.getAnnotation(GetMapping.class).value());
            addAll(paths, method.getAnnotation(PostMapping.class) == null ? null
                    : method.getAnnotation(PostMapping.class).value());
            addAll(paths, method.getAnnotation(PutMapping.class) == null ? null
                    : method.getAnnotation(PutMapping.class).value());
            addAll(paths, method.getAnnotation(PatchMapping.class) == null ? null
                    : method.getAnnotation(PatchMapping.class).value());
            addAll(paths, method.getAnnotation(DeleteMapping.class) == null ? null
                    : method.getAnnotation(DeleteMapping.class).value());
        }
        return paths;
    }

    private static void addAll(List<String> paths, String[] values) {
        if (values != null) {
            paths.addAll(Arrays.asList(values));
        }
    }
}
