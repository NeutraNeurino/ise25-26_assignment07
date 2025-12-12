package de.seuhd.campuscoffee.api.controller;

import de.seuhd.campuscoffee.api.dtos.ReviewDto;
import de.seuhd.campuscoffee.api.mapper.DtoMapper;
import de.seuhd.campuscoffee.api.mapper.ReviewDtoMapper;
import de.seuhd.campuscoffee.api.openapi.CrudOperation;
import de.seuhd.campuscoffee.domain.model.objects.Review;
import de.seuhd.campuscoffee.domain.ports.api.CrudService;
import de.seuhd.campuscoffee.domain.ports.api.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static de.seuhd.campuscoffee.api.openapi.Operation.*;
import static de.seuhd.campuscoffee.api.openapi.Resource.REVIEW;

/**
 * Controller for handling reviews for POS, authored by users.
 */
@Tag(name="Reviews", description="Operations for managing reviews for points of sale.")
@Controller
@RequestMapping("/api/reviews")
@Slf4j
@RequiredArgsConstructor
public class ReviewController extends CrudController<Review, ReviewDto, Long> {

    private final ReviewService reviewService;
    private final ReviewDtoMapper reviewDtoMapper;
@Override
    protected @NonNull CrudService<Review, Long> service() {
        return reviewService;
    }

@Override
    protected @NonNull DtoMapper<Review, ReviewDto> mapper() {
        return reviewDtoMapper;
    }

    @Operation
    @CrudOperation(operation = GET_ALL, resource = REVIEW)
    @GetMapping("")
    public @NonNull ResponseEntity<List<ReviewDto>> getAll() {
        return super.getAll();
    }

    @Operation
    @CrudOperation(operation=GET_BY_ID, resource=REVIEW)
    @GetMapping("/{id}") // Adrian, ich hab den Enum-Wert hier angepasst, der war falsch --- IGNORE ---
    public @NonNull ResponseEntity<ReviewDto> getById(@PathVariable Long id) {
        return super.getById(id);
    }
    @Operation
    @CrudOperation(operation = CREATE, resource = REVIEW)
    @PostMapping("")
    public @NonNull ResponseEntity<ReviewDto> create(@Valid @RequestBody ReviewDto dto) {
        return super.create(dto);
    }
    
    @Operation
    @CrudOperation(operation = UPDATE, resource = REVIEW)
    @PutMapping("/{id}")
    public @NonNull ResponseEntity<ReviewDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ReviewDto dto
    ) {
        return super.update(id, dto);
    }

    @Operation
    @CrudOperation(operation = DELETE, resource = REVIEW)
    @DeleteMapping("/{id}")
    public @NonNull ResponseEntity<Void> delete(@PathVariable Long id) {
        return super.delete(id);
    }

    @Operation
    @CrudOperation(operation = FILTER, resource = REVIEW)
    @GetMapping("/filter")
    public ResponseEntity<List<ReviewDto>> filter(
            @RequestParam("pos_id") Long posId,
            @RequestParam("approved") Boolean approved
    ) {
        log.debug("Filtering reviews for posId={} and approved={}", posId, approved);
        return ResponseEntity.ok(reviewService.filter(posId, approved));
    }

    @Operation(summary = "Approve a review for a user.")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ReviewDto> approve(
            @PathVariable Long id,
            @RequestParam("user_id") Long userId
    ) {
        log.debug("Approving review {} by user {}", id, userId);
        return ResponseEntity.ok(reviewService.approve(id, userId));
    }
}