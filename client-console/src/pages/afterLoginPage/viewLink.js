/**
 * Where a landing tile for a journal view leads. Shared by both landing grids,
 * so a tile opens the same screen whichever school drew it.
 *
 * The period tier and subject mode travel in the query string.
 *
 * Not in the path: they are a starting position rather than an identity. A
 * parent who changes the period picker is still on the same journal, and the
 * uuid is what addresses it — which is the reason the legacy console's
 * /grades/<subject NAME> had to be replaced in the first place.
 */
export const linkFor = (view) => {
    const params = new URLSearchParams();
    if (view.periodKind) params.set("periodKind", view.periodKind);
    if (view.subjectMode) params.set("subjects", view.subjectMode);
    if (view.chartKey) params.set("chart", view.chartKey);
    if (view.chartColumn) params.set("chartColumn", view.chartColumn);
    const query = params.toString();
    return `/journal/${view.journalUuid}${query ? "?" + query : ""}`;
};
