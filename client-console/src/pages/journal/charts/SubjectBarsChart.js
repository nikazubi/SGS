import React from "react";
import {Bar, BarChart, CartesianGrid, LabelList, Tooltip, XAxis, YAxis} from "recharts";
import ChartFrame, {chartTheme} from "./ChartFrame";

/**
 * One bar per subject — the brief's "summary trimester assessment", where a
 * table of subjects sits above a chart comparing them.
 *
 * A different question from GRADE_TREND, which is why it is a second chart and
 * not a setting on the first: a trend follows one subject through the term,
 * this stands every subject beside each other in one of them.
 *
 * Which mark it plots is named by the menu item that opened the page, because
 * no ordering gets both summaries right: the trimester one wants the trimester
 * assessment, which is its last column, and the annual one wants the final
 * academic grade, which is not. A view that names nothing falls back to the last
 * numeric column, so a journal nobody has configured still gets a chart.
 */
const SubjectBarsChart = ({view, row, column: wanted}) => {

    // Drilled into one subject: one bar is not a comparison, and the trend is
    // the right picture for a single row.
    if (row || view.rows.length < 2) {
        return null;
    }

    const numeric = view.columns.filter(c =>
        view.rows.some(r => r.values[c.code] !== "" && r.values[c.code] != null
            && !Number.isNaN(Number(r.values[c.code]))));

    if (!numeric.length) {
        return null;
    }

    // What the button asked for, if it is actually a column here and actually
    // has numbers in it. Falling through to the last numeric one rather than
    // drawing nothing keeps a mistyped code from emptying the page.
    const column = numeric.find(c => c.code === wanted) || numeric[numeric.length - 1];

    const data = view.rows
        .map(r => ({
            name: r.label,
            value: r.values[column.code] === "" || r.values[column.code] == null
                ? null
                : Number(r.values[column.code])
        }))
        // A subject with nothing published is left out rather than drawn as
        // zero. Zero is a mark; "not yet" is not.
        .filter(point => point.value != null);

    if (data.length < 2) {
        return null;
    }

    return (
        <ChartFrame title={column.label} ratio={2.2} minHeight={240}>
            <BarChart data={data} margin={{top: 18, right: 16, bottom: 4, left: -18}}>
                <CartesianGrid {...chartTheme.grid}/>
                {/* Subject names are long and Georgian has no short form, so the
                    axis gives each one a line of its own rather than leaning
                    them, which stays readable at any width. */}
                <XAxis dataKey="name" interval={0} height={64} {...chartTheme.axis}
                       tick={{...chartTheme.axis.tick, width: 78}}/>
                <YAxis allowDecimals={false} {...chartTheme.axis}/>
                <Tooltip {...chartTheme.tooltip}/>
                <Bar dataKey="value" name={column.label} fill={chartTheme.bar}
                     radius={[6, 6, 0, 0]} maxBarSize={64}>
                    <LabelList dataKey="value" position="top"
                               style={{fontSize: 12, fill: "#3b5a6b"}}/>
                </Bar>
            </BarChart>
        </ChartFrame>
    );
};

export default SubjectBarsChart;
