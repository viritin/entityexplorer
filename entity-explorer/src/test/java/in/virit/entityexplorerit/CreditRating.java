package in.virit.entityexplorerit;

/**
 * Creditworthiness rating, derived from a normalized credit score (0–100).
 * <p>
 * This is a purely domain-level concept: it carries the business label and the
 * threshold logic, but knows nothing about how it is displayed (no colors, no
 * CSS). The visual mapping lives in the UI layer
 * ({@code CreditScoreIndicator}), so the credit rule and its presentation can
 * evolve independently.
 */
public enum CreditRating {

    GOOD("Creditworthy"),
    MEDIUM("Limited"),
    POOR("At risk");

    /**
     * Minimum score (inclusive) to be considered {@link #GOOD}.
     * Business thresholds are kept as named constants so the credit rule is
     * explicit and auditable rather than hidden in magic numbers.
     */
    static final int GOOD_THRESHOLD = 70;

    /** Minimum score (inclusive) to be considered {@link #MEDIUM}; below this is {@link #POOR}. */
    static final int MEDIUM_THRESHOLD = 40;

    private final String label;

    CreditRating(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Maps a normalized credit score (0–100) to a rating. In practice such a
     * score is derived from an external bureau value (e.g. SCHUFA / FICO) and
     * normalized to a 0–100 scale before reaching this application.
     */
    public static CreditRating fromScore(int score) {
        if (score >= GOOD_THRESHOLD) {
            return GOOD;
        }
        if (score >= MEDIUM_THRESHOLD) {
            return MEDIUM;
        }
        return POOR;
    }

    /** Lowest score (inclusive) that falls into this rating — used to filter by rating. */
    public int minScoreInclusive() {
        return switch (this) {
            case GOOD -> GOOD_THRESHOLD;
            case MEDIUM -> MEDIUM_THRESHOLD;
            case POOR -> Integer.MIN_VALUE;
        };
    }

    /** Highest score (inclusive) that falls into this rating — used to filter by rating. */
    public int maxScoreInclusive() {
        return switch (this) {
            case GOOD -> Integer.MAX_VALUE;
            case MEDIUM -> GOOD_THRESHOLD - 1;
            case POOR -> MEDIUM_THRESHOLD - 1;
        };
    }
}
