import React from "react";
import {CartesianGrid, Line, LineChart, Tooltip, XAxis, YAxis} from "recharts";

/**
 * A student's marks across the journal's columns.
 *
 * Reads the rendered view rather than knowing anything about grades, so it
 * works for any journal an admin points it at — the columns are whatever that
 * journal defines. Non-numeric marks (ჩთ) are simply absent from the line
 * rather than plotted as zero, which would drag an average-looking chart down
 * for a lesson nobody sat.
 */
const GradeTrendChart = ({view, row}) => {

    if (!row) {
        return null;
    }

    const points = view.columns
        .map(column => ({
            name: column.label,
            value: Number(row.values[column.code])
        }))
        .filter(p => !Number.isNaN(p.value));

    if (points.length < 2) {
        // One point is not a trend; drawing it would suggest more than is there.
        return null;
    }

    return (
        <div className="ib__center" style={{marginTop: 24}}>
            <LineChart width={640} height={220} data={points}
                       margin={{top: 20, right: 20, bottom: 5, left: 0}}>
                <CartesianGrid strokeDasharray="3 3"/>
                <XAxis dataKey="name" tick={{fontSize: 11}}/>
                <YAxis allowDecimals={false}/>
                <Tooltip/>
                <Line type="monotone" dataKey="value" stroke="#f25d23" strokeWidth={2}/>
            </LineChart>
        </div>
    );
};

export default GradeTrendChart;
