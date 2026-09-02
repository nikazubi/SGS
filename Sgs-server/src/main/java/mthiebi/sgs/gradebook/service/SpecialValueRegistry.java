package mthiebi.sgs.gradebook.service;

import mthiebi.sgs.gradebook.engine.SpecialValueBehaviour;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * How each special code behaves inside an aggregation.
 * <p>
 * Phase 1 ships the one code the school actually uses. It lives behind a lookup
 * rather than being inlined so the template editor can make it configurable
 * without the engine changing: the old system's equivalent was a hardcoded -50
 * that some screens averaged as a number and others rendered as text.
 */
@Service
public class SpecialValueRegistry {

    /**
     * ჩთ - not attested. Excluded from averages rather than counted as zero.
     */
    public static final String NOT_ATTESTED = "CHT";

    public Map<String, SpecialValueBehaviour> behavioursFor(Long templateVersionId) {
        Map<String, SpecialValueBehaviour> map = new HashMap<>();
        map.put(NOT_ATTESTED, SpecialValueBehaviour.EXCLUDE);
        return Collections.unmodifiableMap(map);
    }

    public boolean isKnown(String code) {
        return NOT_ATTESTED.equals(code);
    }

    /**
     * What a teacher sees in the cell. Falls back to the code itself.
     */
    public String labelOf(String code) {
        if (NOT_ATTESTED.equals(code)) {
            return "ჩთ";
        }
        return code;
    }
}
