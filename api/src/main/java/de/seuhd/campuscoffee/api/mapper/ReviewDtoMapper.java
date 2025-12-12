package de.seuhd.campuscoffee.api.mapper;
//extrem gekürzte Version, die andere hats immer zerschossen
import de.seuhd.campuscoffee.api.dtos.ReviewDto;
import de.seuhd.campuscoffee.domain.model.objects.Review;
import org.mapstruct.Mapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * MapStruct mapper for converting between the {@link Review} domain model objects and {@link ReviewDto}s.
 */
@Mapper(componentModel = "spring")
@ConditionalOnMissingBean // prevent IntelliJ warning about duplicate beans
public interface ReviewDtoMapper extends DtoMapper<Review, ReviewDto> {

    @Override
    ReviewDto fromDomain(Review source);

    @Override
    Review toDomain(ReviewDto source);
}
