import React from "react";
import {Bar, BarChart, Cell, LabelList, ResponsiveContainer, Tooltip, XAxis} from "recharts";
import {state} from "../../../theme/primaryTheme";

/**
 * Hours missed, one bar per reporting period.
 *
 * The brief's one hard visual rule for this page: *"let it be green, and if
 * exceeded let it become red"*. The allowance is entered per class per month, so
 * **the comparison is per bar** — a child can be inside September's ceiling and
 * past October's, and the colours have to be able to say so. That is also why
 * there is no single threshold line across the chart: one line at one month's
 * ceiling would read as the rule for all of them.
 *
 * A month with no allowance recorded gets neither colour. Nothing is guessed.
 *
 * **The year is not a bar.** It is the total of these, several times the tallest
 * of them, and drawing it here would flatten every month into the floor. It sits
 * at the end of the row above instead.
 *
 * No axis and no gridlines: the figure is printed on the bar, so there is
 * nothing to trace across to. Legacy labelled each bar with its share of the
 * year instead, which answered a question nobody asked — the brief's readout is
 * hours.
 */
const AbsenceBarsChart = ({view}) => {

    const code = view.columns[0] && view.columns[0].code;
    if (!code) {
        return null;
    }

    const data = view.rows
        .filter(r => r.periodKind !== "YEAR")
        .map(r => ({
            name: shorten(r.label),
            full: r.label,
            value: r.values[code] === "" || r.values[code] == null
                ? null
                : Number(r.values[code]),
            threshold: r.threshold == null ? null : Number(r.threshold),
        }))
        // A month nobody has filled in is absent rather than nought: no absence
        // recorded and none missed are different claims.
        .filter(p => p.value != null);

    if (data.length < 2) {
        return null;
    }

    const over = (p) => p.threshold != null && p.value > p.threshold;
    const anyOver = data.some(over);

    return (
        <figure className="absenceChart">
            <figcaption className="absenceChart__legend">
                <span className="absenceChart__key">
                    <i style={{background: state.under}}/>დასაშვებ ფარგლებში
                </span>
                {/* Only when something is over it. A legend explaining red on a
                    chart with no red in it invites a parent to go looking. */}
                {anyOver ? (
                    <span className="absenceChart__key">
                        <i style={{background: state.over}}/>დასაშვებზე მეტი
                    </span>
                ) : null}
            </figcaption>

            <ResponsiveContainer width="100%" aspect={2.6} minHeight={200}>
                <BarChart data={data} margin={{top: 24, right: 8, bottom: 4, left: 8}}>
                    <XAxis dataKey="name" interval={0} tickLine={false}
                           axisLine={{stroke: "#cddde5"}}
                           tick={{fontSize: 11, fill: "#5b7a8a", width: 70}}
                           height={44}/>
                    <Tooltip
                        cursor={{fill: "rgba(34, 133, 178, 0.06)"}}
                        contentStyle={{
                            borderRadius: "0 10px 10px 10px",
                            border: "1px solid #cddde5", fontSize: 13
                        }}
                        labelFormatter={(_, p) => (p[0] ? p[0].payload.full : "")}
                        formatter={(v, n, p) => [
                            p.payload.threshold == null
                                ? `${v} სთ`
                                : `${v} სთ (დასაშვები ${p.payload.threshold})`,
                            "გაცდენილი"]}/>
                    <Bar dataKey="value" radius={[6, 6, 0, 0]} maxBarSize={56}>
                        {data.map((p, i) => (
                            <Cell key={i}
                                  fill={p.threshold == null ? state.unset
                                      : over(p) ? state.over : state.under}/>
                        ))}
                        <LabelList dataKey="value" position="top"
                                   style={{fontSize: 12, fontWeight: 600, fill: "#3b5a6b"}}/>
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
        </figure>
    );
};

/**
 * Month names, short enough for seven of them across a phone.
 *
 * Three letters of each part, so სექტემბერი-ოქტომბერი reads სექ-ოქტ - which is
 * how the brief's own table header writes it. Full names are one line above in
 * the row, and the tooltip carries them, so nothing is lost; without this, seven
 * long Georgian names across 366px overlap into an unreadable smear.
 */
const shorten = (label) => (label || "")
    .split("-")
    .map(part => (part.trim().length > 4 ? part.trim().slice(0, 3) : part.trim()))
    .join("-");

export default AbsenceBarsChart;
