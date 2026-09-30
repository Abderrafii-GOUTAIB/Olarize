package com.olarize.assistant.infrastructure.out.provider;

import com.olarize.assistant.domain.model.ExpiryToolCall;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reviewed deterministic evaluation examples; not a general language model. */
public final class SyntheticExpiryPlanner {
    private static final Set<String> NEXT_MONTH = Set.of(
            "which contracts expire next month?", "quels contrats expirent le mois prochain ?",
            "ما هي العقود التي تنتهي الشهر المقبل؟");
    public ExpiryToolCall plan(String question) {
        String text = question.strip().toLowerCase(Locale.ROOT);
        if (NEXT_MONTH.contains(text)) return new ExpiryToolCall("listExpiringContracts", Map.of("period", "NEXT_MONTH"));
        if ("which contracts expire in october 2026?".equals(text))
            return new ExpiryToolCall("listExpiringContracts", Map.of("fromDate", "2026-10-01", "toDate", "2026-10-31"));
        // Unknown wording, periods or instructions are never transformed into unrestricted tool calls.
        return new ExpiryToolCall("clarify", Map.of());
    }
}
