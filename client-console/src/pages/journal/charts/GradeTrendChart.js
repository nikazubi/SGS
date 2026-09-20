import React from "react";
import {CartesianGrid, Line, LineChart, Tooltip, XAxis, YAxis} from "recharts";
import ChartFrame, {chartTheme} from "./ChartFrame";

/**
 * One subject's marks over the term, as a line.
 *
 * **Only the marks that form a series.** This used to plot every column, which
 * put the seven ongoing marks, their own average, three test results and the
 * final trimester assessment on one line — so the average sat between marks it
 * was computed from, and the trimester grade appeared as if it were the eighth
 * week. A line says "this came after that", and most of those columns do not.
 *
 * The series is the journal's own grouping: the school put the ongoing marks
 * under one heading and left every summary column ungrouped, which is exactly
 * the distinction wanted. A journal that groups differently gets whatever it
 * grouped, and one that groups nothing gets no chart rather than a wrong one.
 */
const GradeTrendChart = ({view, row}) => {

    if (!row) {
        return null;
    }

    const grouped = view.columns.filter(c => c.groupLabel);
    if (!grouped.length) {
        return null;
    }

    // The first group only. A second would be a different series on the same
    // axis, which is a chart this is not.
    const seriesName = grouped[0].groupLabel;

    const points = grouped
        .filter(c => c.groupLabel === seriesName)
        .map(column => ({name: column.label, value: Number(row.values[column.code])}))
        // A mark that is not a number — ჩთ, or a lesson not yet held — is absent
        // rather than zero, which would pull the line down for nothing.
        .filter(p => !Number.isNaN(p.value));

    if (points.length < 2) {
        // One point is not a trend, and drawing it suggests more than is there.
        return null;
    }

    return (
        <ChartFrame title={seriesName} ratio={2.6}>
            <LineChart data={points} margin={{top: 16, right: 16, bottom: 4, left: -18}}>
                <CartesianGrid {...chartTheme.grid}/>
                <XAxis dataKey="name" {...chartTheme.axis}/>
                <YAxis allowDecimals={false} {...chartTheme.axis}/>
                <Tooltip {...chartTheme.tooltip}/>
                <Line type="monotone" dataKey="value" name={seriesName}
                      stroke={chartTheme.line} strokeWidth={2}
                      dot={{r: 4, fill: chartTheme.line}} activeDot={{r: 6}}/>
            </LineChart>
        </ChartFrame>
    );
};

export default GradeTrendChart;
