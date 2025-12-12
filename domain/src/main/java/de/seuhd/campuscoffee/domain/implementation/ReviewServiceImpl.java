//diese Version des Codes ist von GPT 5.1 generiert. (Ich hoffe, diese Markierung entspricht den Vorgaben)

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
 * Implementation of the Review service that handles business logic related to review entities.
 */
@Slf4j
@Service
public class ReviewServiceImpl extends CrudServiceImpl<Review, Long> implements ReviewService {

    private final ReviewDataService reviewDataService;
    private final UserDataService userDataService;
    private final PosDataService posDataService;
    // Reads the minimum approval count from application.yaml via ApprovalConfiguration.
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

    @Override
    @Transactional
    public @NonNull Review upsert(@NonNull Review review) {
        log.info("Upserting review with ID '{}'...", review.getId());

        // Sicherstellen, dass POS und User existieren (würde sonst eine passende Exception werfen)
        var pos = posDataService.getById(review.pos().getId());
        var author = userDataService.getById(review.author().getId());

        // Business-Regel: Ein User darf nur EIN Review pro POS abgeben.
        // Für neue Reviews (id == null) darf es noch keinen Datensatz mit gleicher POS-/Author-Kombination geben.
        boolean alreadyReviewed = reviewDataService.getAll().stream()
                .anyMatch(existing ->
                        existing.pos().getId().equals(pos.getId())
                                && existing.author().getId().equals(author.getId())
                                // Falls wir ein bestehendes Review updaten, nicht mit sich selbst vergleichen
                                && (review.getId() == null || !existing.getId().equals(review.getId()))
                );

        if (alreadyReviewed) {
            throw new ValidationException("A user cannot submit more than one review per POS.");
        }

        // Approval-Status konsistent zum approvalCount setzen
        Review normalized = updateApprovalStatus(review);

        // Delegation an generische CRUD-Logik
        return super.upsert(normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<Review> filter(@NonNull Long posId, @NonNull Boolean approved) {
        return reviewDataService.filter(posDataService.getById(posId), approved);
    }

    @Override
    @Transactional
    public @NonNull Review approve(@NonNull Review review, @NonNull Long userId) {
        log.info("Processing approval request for review with ID '{}' by user with ID '{}'...",
                review.getId(), userId);

        // 1) Prüfen, ob der approvende Nutzer existiert
        userDataService.getById(userId); // wir nutzen das Objekt nicht, aber die Methode stellt die Existenz sicher

        // 2) Review aus der Datenbank laden (stellt Existenz sicher und sorgt für frischen Zustand)
        Review persisted = reviewDataService.getById(review.getId());

        // 3) Nutzer darf sein eigenes Review nicht genehmigen
        if (persisted.author().getId().equals(userId)) {
            throw new ValidationException("Users are not allowed to approve their own reviews.");
        }

        // 4) Approval-Count erhöhen
        persisted = persisted.toBuilder()
                .approvalCount(persisted.approvalCount() + 1)
                .build();

        // 5) Approval-Status anhand der konfigurierten Mindestanzahl aktualisieren
        persisted = updateApprovalStatus(persisted);

        // 6) Speichern
        return reviewDataService.upsert(persisted);
    }

    /**
     * Calculates and updates the approval status of a review based on the approval count.
     * Business rule: A review is approved when it reaches the configured minimum approval count threshold.
     *
     * @param review The review to calculate approval status for
     * @return The review with updated approval status
     */
    Review updateApprovalStatus(Review review) {
        log.debug("Updating approval status of review with ID '{}'...", review.getId());
        return review.toBuilder()
                .approved(isApproved(review))
                .build();
    }

    /**
     * Determines if a review meets the minimum approval threshold.
     *
     * @param review The review to check
     * @return true if the review meets or exceeds the minimum approval count, false otherwise
     */
    private boolean isApproved(Review review) {
        return review.approvalCount() >= approvalConfiguration.minCount();
    }
}
