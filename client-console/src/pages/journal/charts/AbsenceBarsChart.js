import React from "react";
import {Bar, BarChart, Cell, CartesianGrid, ReferenceLine, Tooltip, XAxis, YAxis} from "recharts";

/**
 * One bar per row — the shape a class-wide journal has, where rows are periods.
 *
 * Plots the first numeric column, because that is what a journal of this shape
 * has to say: hours missed in a month, a monthly behaviour score. Which column
 * that is comes from the journal, not from here.
 *
 * Green until the month passes its allowance, then red - the brief's one visual
 * rule for absence. The allowance is per month, so the comparison is per bar:
 * September can be within its ceiling while October is past its own. A journal
 * with no ceiling gets no colouring and keeps the neutral bar, which is what
 * every other class-wide journal does.
 */
const AbsenceBarsChart = ({view}) => {

    const column = view.columns.find(c =>
        view.rows.some(r => !Number.isNaN(Number(r.values[c.code]))
            && r.values[c.code] !== ""));

    if (!column || view.rows.length < 2) {
        return null;
    }

    const data = view.rows.map(row => ({
        name: row.label,
        value: Number(row.values[column.code]) || 0,
        // Null for a month nobody has set an allowance for. Undefined and zero
        // are different things here: zero would mean every absence is over.
        threshold: row.threshold == null ? null : Number(row.threshold)
    }));

    const over = (point) => point.threshold != null && point.value > point.threshold;

    // One line, only when every month shares an allowance. Drawing a different
    // line per bar is not something a bar chart can say, and a line at one
    // month's ceiling would misread as the rule for all of them.
    const thresholds = data.map(d => d.threshold).filter(t => t != null);
    const commonThreshold = thresholds.length === data.length
    && thresholds.every(t => t === thresholds[0])
        ? thresholds[0]
        : null;

    return (
        <div className="ib__center" style={{marginTop: 24}}>
            <BarChart width={640} height={220} data={data}
                      margin={{top: 20, right: 20, bottom: 5, left: 0}}>
                <CartesianGrid strokeDasharray="3 3"/>
                <XAxis dataKey="name" tick={{fontSize: 11}}/>
                <YAxis allowDecimals={false}/>
                <Tooltip/>
                {commonThreshold != null ? (
                    <ReferenceLine y={commonThreshold} stroke="#c62828"
                                   strokeDasharray="4 4"/>
                ) : null}
                <Bar dataKey="value" name={column.label}>
                    {data.map((point, index) => (
                        <Cell key={index}
                              fill={point.threshold == null
                                  ? "#f79348"
                                  : over(point) ? "#c62828" : "#2e9e6b"}/>
                    ))}
                </Bar>
            </BarChart>
        </div>
    );
};

export default AbsenceBarsChart;
