package com.mittechkernel.backend.modules.task.controller;

import com.mittechkernel.backend.common.response.ApiResponse;
import com.mittechkernel.backend.modules.task.dto.TaskCompletionRequest;
import com.mittechkernel.backend.modules.task.dto.TaskRequest;
import com.mittechkernel.backend.modules.task.dto.TaskResponse;
import com.mittechkernel.backend.modules.task.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> listTasks(
            @RequestParam(name = "sessionId", required = false) Long sessionId,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTasks(sessionId), request));
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long id,
                                                            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTask(id), request));
    }

    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskRequest request,
                                                                HttpServletRequest httpServletRequest) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, httpServletRequest, HttpStatus.CREATED.value()));
    }

    @PatchMapping("/tasks/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(@PathVariable Long id,
                                                              @RequestBody TaskRequest request,
                                                              HttpServletRequest httpServletRequest) {
        TaskResponse response = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, httpServletRequest));
    }

    @PostMapping("/tasks/{id}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(@PathVariable Long id,
                                                                  @Valid @RequestBody TaskCompletionRequest request,
                                                                  HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiResponse.success(taskService.completeTask(id, request), httpServletRequest));
    }

    @PostMapping("/tasks/{id}/verify")
    public ResponseEntity<ApiResponse<TaskResponse>> verifyTask(@PathVariable Long id,
                                                              HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ApiResponse.success(taskService.verifyTask(id), httpServletRequest));
    }
}
