import React from "react";
import {Bar, BarChart, CartesianGrid, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis} from "recharts";

export default function CustomShefasebaBar({color, data, width = '100%', height = 440, left = 30}) {

    const formatPercentage = (value) => {
        return value;
    };

    const displayPrecent = (v) => {
        return v.value;
    }

    const isDense = data.length > 8;
    const xAxisHeight = isDense ? 110 : 40;


    return (
        <ResponsiveContainer width={width} height={height}>
            <BarChart
                data={data}
                layout={'horizontal'}
                margin={{
                    top: 20,
                    right: 30,
                    left: left,
                    bottom: 5
                }}
            >
                <CartesianGrid fill="#fff" strokeDasharray="3 3"/>
                <XAxis
                    dataKey="name"
                    interval={0}
                    angle={isDense ? -45 : 0}
                    textAnchor={isDense ? 'end' : 'middle'}
                    height={xAxisHeight}
                    tick={{fontSize: isDense ? 11 : 12}}
                />

                <YAxis
                    domain={[0, 7]}
                    tickCount={8}
                    tickFormatter={formatPercentage}
                />

                <Tooltip/>
                <Bar
                    dataKey={'value'}
                    fill={color}
                    background={{fill: "#eee"}}
                    maxBarSize={50}
                    radius={[20, 20, 0, 0]}
                >
                    <LabelList dataKey={displayPrecent} position="top"/>
                </Bar>
            </BarChart>
        </ResponsiveContainer>
    );
}
