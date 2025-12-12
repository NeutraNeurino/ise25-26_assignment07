//diese überarbeitete Version meines Codes ist von GPT 5.1 generiert. (Ich hoffe, diese Markierung entspricht den Vorgaben)

package de.seuhd.campuscoffee.domain.implementation;

import de.seuhd.campuscoffee.domain.configuration.ApprovalConfiguration;
import de.seuhd.campuscoffee.domain.exceptions.ValidationException;
import de.seuhd.campuscoffee.domain.model.objects.Review;
import de.seuhd.campuscoffee.domain.ports.api.ReviewService;
import de.seuhd.campuscoffee.domain.ports.data.CrudDataService;
import de.seuhd.campuscoffee.domain.ports.data.PosDataService;
import de.seuhd.campuscoffee.domain.ports.data.ReviewDataService;
import de.seuhd.campuscoffee.domain.ports.data.UserDataService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Domain service for handling business logic related to {@link Review} objects.
 */
@Slf4j
@Service
public class ReviewServiceImpl extends CrudServiceImpl<Review, Long> implements ReviewService {

    private final ReviewDataService reviewDataService;
    private final UserDataService userDataService;
    private final PosDataService posDataService;
    /**
     * Configuration bean that exposes the minimum number of approvals
     * required for a review to be considered approved.
     */
    private final ApprovalConfiguration approvalConfiguration;

    public ReviewServiceImpl(
            @NonNull ReviewDataService reviewDataService,
            @NonNull UserDataService userDataService,
            @NonNull PosDataService posDataService,
            @NonNull ApprovalConfiguration approvalConfiguration
    ) {
        super(Review.class);
        this.reviewDataService = reviewDataService;
        this.userDataService = userDataService;
        this.posDataService = posDataService;
        this.approvalConfiguration = approvalConfiguration;
    }

    @Override
    protected CrudDataService<Review, Long> dataService() {
        return reviewDataService;
    }

    /**
     * Upserts a review.
     *
     * Business rules:
     * <ul>
     *     <li>POS and author must exist.</li>
     *     <li>A user may submit at most one review per POS.</li>
     *     <li>The {@code approved} flag is kept consistent with {@code approvalCount}.</li>
     * </ul>
     */
    @Override
    @Transactional
    public @NonNull Review upsert(@NonNull Review review) {
        log.info("Upserting review with ID '{}'...", review.getId());

        // Ensure that referenced POS and author exist (throws if not found)
        var pos = posDataService.getById(review.getPos().getId());
        var author = userDataService.getById(review.getAuthor().getId());

        // Business rule: A user cannot submit more than one review per POS.
        boolean alreadyReviewed = reviewDataService.getAll().stream()
                .anyMatch(existing ->
                        existing.getPos().getId().equals(pos.getId())
                                && existing.getAuthor().getId().equals(author.getId())
                                // when updating an existing review, ignore the same record
                                && (review.getId() == null || !existing.getId().equals(review.getId()))
                );

        if (alreadyReviewed) {
            throw new ValidationException("A user cannot submit more than one review per POS.");
        }

        // Keep approval flag consistent with approvalCount
        Review normalized = updateApprovalStatus(review);

        // Delegate to generic CRUD implementation
        return super.upsert(normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<Review> filter(@NonNull Long posId, @NonNull Boolean approved) {
        return reviewDataService.filter(posDataService.getById(posId), approved);
    }

    /**
     * Processes an approval request for the given review.
     *
     * Business rules:
     * <ul>
     *     <li>The approving user must exist.</li>
     *     <li>The review must exist.</li>
     *     <li>Users are not allowed to approve their own reviews.</li>
     *     <li>Once the approval count reaches the configured threshold,
     *         the review is marked as approved.</li>
     * </ul>
     */
    @Override
    @Transactional
    public @NonNull Review approve(@NonNull Review review, @NonNull Long userId) {
        log.info("Processing approval request for review with ID '{}' by user with ID '{}'...",
                review.getId(), userId);

        // 1) Ensure the approving user exists
        userDataService.getById(userId); // throws if not found

        // 2) Reload review from DB to ensure it exists and is up-to-date
        Review persisted = reviewDataService.getById(review.getId());

        // 3) Users are not allowed to approve their own reviews
        if (persisted.getAuthor().getId().equals(userId)) {
            throw new ValidationException("Users are not allowed to approve their own reviews.");
        }

        // 4) Increment approval count
        persisted = persisted.toBuilder()
                .approvalCount(persisted.approvalCount() + 1)
                .build();

        // 5) Update approval status based on configured threshold
        persisted = updateApprovalStatus(persisted);

        // 6) Persist and return updated review
        return reviewDataService.upsert(persisted);
    }

    /**
     * Recalculates the {@code approved} flag based on the current {@code approvalCount}.
     * A review is considered approved if it reaches the configured minimum approval count.
     */
    Review updateApprovalStatus(Review review) {
        log.debug("Updating approval status of review with ID '{}'...", review.getId());
        return review.toBuilder()
                .approved(isApproved(review))
                .build();
    }

    /**
     * Determines whether the given review meets the minimum approval threshold.
     */
    private boolean isApproved(Review review) {
        return review.approvalCount() >= approvalConfiguration.minCount();
    }
}
