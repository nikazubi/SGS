import {Link} from "react-router-dom";
import {useQuery} from "react-query";
import NewsTile from "../../components/NewsTile";
import {fetchHomeworkMonth} from "../journal/parentApi";
import "./standardLanding.css";

const thisMonth = () => new Date().toISOString().slice(0, 7);

/**
 * The landing page for basic and secondary.
 *
 * Seven buttons on a six-column track. Six rather than three so a row of two
 * can split evenly instead of leaving a third of the width empty:
 *
 *     news (4)                        homework (2)
 *     per-subject (2)   summary (2)   annual (2)
 *     ethics (3)                      absence (3)
 *
 * The rows are not an arrangement of seven equal things. Row 1 is the two
 * content modules; row 2 is one journal seen three ways; row 3 is the two
 * monthly class-wide registers. The grouping is a fact about the data rather
 * than a layout convenience — which is why the rows are allowed to mean
 * something and the order is not alphabetical.
 *
 * **Only the first two boxes are fixed.** Everything after them is a
 * `parent_view` row, so the five here are the school's five today and a sixth
 * is an INSERT. The grid therefore has to survive a count it was not designed
 * around: `--span` is set per tile below, and anything past the seventh falls
 * into a plain half-width row rather than overflowing.
 */
const StandardLandingGrid = ({modules, views}) => {
    const present = new Set(modules || []);
    const items = views || [];

    return (
        <div className="standardLanding">
            <div className="standardLanding__grid">
                {present.has("NEWS") ? (
                    <NewsTile className="standardLanding__tile standardLanding__tile--news"/>
                ) : null}

                {present.has("HOMEWORK") ? <HomeworkTile/> : null}

                {items.map((view, i) => (
                    <Link key={view.journalUuid + ":" + view.label}
                          to={linkFor(view)}
                          className="standardLanding__tile standardLanding__tile--view"
                          style={{"--span": spanFor(i, items.length)}}>
                        <span className="standardLanding__label">{view.label}</span>
                    </Link>
                ))}
            </div>
        </div>
    );
};

/**
 * The period tier and subject mode travel in the query string.
 *
 * Not in the path: they are a starting position rather than an identity. A
 * parent who changes the period picker is still on the same journal, and the
 * uuid is what addresses it — which is the reason the legacy console's
 * /grades/<subject NAME> had to be replaced in the first place.
 */
const linkFor = (view) => {
    const params = new URLSearchParams();
    if (view.periodKind) params.set("periodKind", view.periodKind);
    if (view.subjectMode) params.set("subjects", view.subjectMode);
    if (view.chartKey) params.set("chart", view.chartKey);
    if (view.chartColumn) params.set("chartColumn", view.chartColumn);
    const query = params.toString();
    return `/journal/${view.journalUuid}${query ? "?" + query : ""}`;
};

/**
 * How wide the nth tile is, on a six-column track.
 *
 * Three across then two across is the shape the school asked for, and it only
 * divides for exactly five. For any other count this falls back to halves,
 * which tiles evenly at any number and never leaves a hole — a seeded sixth
 * journal changes the rhythm rather than breaking the row.
 */
const spanFor = (index, total) => {
    if (total !== 5) return 3;
    return index < 3 ? 2 : 3;
};

/** Homework, with this month's unopened count. */
const HomeworkTile = () => {
    const {data} = useQuery(
        ["PARENT_HOMEWORK_MONTH", thisMonth()], () => fetchHomeworkMonth(thisMonth()),
        {refetchOnWindowFocus: false});

    const unseen = (data?.days || []).reduce((sum, day) => sum + day.unseen, 0);

    return (
        <Link to="/homework" className="standardLanding__tile standardLanding__tile--homework">
            {unseen > 0 ? <span className="standardLanding__badge">{unseen}</span> : null}
            <span className="standardLanding__label">საშინაო დავალებები</span>
        </Link>
    );
};

export default StandardLandingGrid;
