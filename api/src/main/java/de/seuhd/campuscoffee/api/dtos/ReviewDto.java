package de.seuhd.campuscoffee.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * DTO record for POS metadata.
 */
@Builder(toBuilder = true)
public record ReviewDto(
        @Nullable Long id,                    // null when creating a new review
        @Nullable LocalDateTime createdAt,    // null when creating a new review
        @Nullable LocalDateTime updatedAt,    // null when creating a new review

        @NotNull Long posId,                  // never null
        @NotNull Long authorId,               // never null

        @NotBlank String review,              // must not be null or empty

        @Nullable Boolean approved            // not present (null) when creating a new review
) implements Dto<Long> {
}