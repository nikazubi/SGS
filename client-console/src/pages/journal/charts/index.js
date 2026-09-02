import GradeTrendChart from "./GradeTrendChart";
import AbsenceBarsChart from "./AbsenceBarsChart";

/**
 * Which chart a journal draws.
 *
 * A chart is code: it has to know what is an axis and what is a series, and
 * that is a judgement per journal rather than something derivable from columns.
 * What *is* data is which journal gets which — the journal carries a chartKey
 * and this maps it to a component.
 *
 * So adding a chart is a file plus one line here, once per chart rather than
 * once per journal. A journal naming no chart, or naming one that has not been
 * written yet, renders a complete page without one — omission is never a
 * broken page.
 *
 * Keyed by a stable name rather than by journal uuid on purpose: uuids are
 * generated per environment, so a uuid-keyed registry would need different code
 * in development and in production.
 */
const CHARTS = {
    GRADE_TREND: GradeTrendChart,
    ABSENCE_BARS: AbsenceBarsChart
};

export const chartFor = (chartKey) => (chartKey ? CHARTS[chartKey] : null) || null;

/** Offered in the journal editor, so an admin picks from what exists. */
export const AVAILABLE_CHARTS = Object.keys(CHARTS);
