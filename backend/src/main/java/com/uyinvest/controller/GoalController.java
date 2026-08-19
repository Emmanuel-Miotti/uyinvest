package com.uyinvest.controller;

import com.uyinvest.dto.request.GoalRequest;
import com.uyinvest.dto.response.GoalResponse;
import com.uyinvest.exception.ErrorResponse;
import com.uyinvest.security.CustomUserDetails;
import com.uyinvest.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "Personal financial goals with computed progress")
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    @Operation(summary = "List the authenticated user's goals")
    public ResponseEntity<List<GoalResponse>> getAll(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(goalService.getAllForUser(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a goal by id")
    @ApiResponse(responseCode = "404", description = "Goal not found or not owned by the caller",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<GoalResponse> getById(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID id) {
        return ResponseEntity.ok(goalService.getById(principal.getId(), id));
    }

    @PostMapping
    @Operation(summary = "Create a financial goal")
    public ResponseEntity<GoalResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.create(principal.getUser(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a goal's editable fields")
    @ApiResponse(responseCode = "404", description = "Goal not found or not owned by the caller",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<GoalResponse> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a goal")
    @ApiResponse(responseCode = "404", description = "Goal not found or not owned by the caller",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID id) {
        goalService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
