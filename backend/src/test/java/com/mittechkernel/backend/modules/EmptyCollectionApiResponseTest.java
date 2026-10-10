package com.mittechkernel.backend.modules;

import com.mittechkernel.backend.modules.project.controller.ProjectController;
import com.mittechkernel.backend.modules.project.service.ProjectService;
import com.mittechkernel.backend.modules.session.controller.SessionController;
import com.mittechkernel.backend.modules.session.service.AttendanceService;
import com.mittechkernel.backend.modules.session.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
class EmptyCollectionApiResponseTest {

    @Test
    void emptyProjectsReturnSuccessfulEmptyCollection() {
        ProjectService service = new ProjectService(null, null, null, null, null) {
            @Override
            public List<com.mittechkernel.backend.modules.project.dto.ProjectResponse> getProjects(
                    Long domainId, String status, String program, String query) {
                return List.of();
            }
        };
        ProjectController controller = new ProjectController(service);

        var response = controller.getProjects(null, null, null, null, request("/api/v1/projects"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isEmpty();
    }

    @Test
    void emptySessionsReturnSuccessfulEmptyCollection() {
        SessionService sessions = new SessionService(null, null, null, null) {
            @Override
            public List<com.mittechkernel.backend.modules.session.dto.SessionResponse> getSessions(
                    Long domainId, String program, String type, java.time.LocalDate date) {
                return List.of();
            }
        };
        SessionController controller = new SessionController(sessions,
                new AttendanceService(null, null, null, null, null, null));

        var response = controller.listSessions(null, null, null, null, request("/api/v1/sessions"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isEmpty();
    }

    private static HttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
