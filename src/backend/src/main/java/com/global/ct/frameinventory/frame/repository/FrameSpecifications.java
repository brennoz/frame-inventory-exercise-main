package com.global.ct.frameinventory.frame.repository;

import com.global.ct.frameinventory.frame.model.Frame;
import com.global.ct.frameinventory.frame.model.FrameEnvironment;
import com.global.ct.frameinventory.frame.model.FrameStatus;
import com.global.ct.frameinventory.frame.model.MediaType;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class FrameSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private FrameSpecifications() {
    }

    public static Specification<Frame> matching(String query, FrameStatus status,
                                                FrameEnvironment environment, MediaType mediaType) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%";
                List<Predicate> textMatches = new ArrayList<>();
                for (String field : List.of("frameId", "siteNumber", "station", "address", "town", "postcode", "format")) {
                    Expression<String> value = builder.lower(root.get(field).as(String.class));
                    textMatches.add(builder.like(value, pattern, LIKE_ESCAPE));
                }
                predicates.add(builder.or(textMatches.toArray(Predicate[]::new)));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (environment != null) {
                predicates.add(builder.equal(root.get("environment"), environment));
            }
            if (mediaType != null) {
                predicates.add(builder.equal(root.get("mediaType"), mediaType));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
