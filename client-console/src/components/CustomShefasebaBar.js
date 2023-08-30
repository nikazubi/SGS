import React from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LabelList
} from "recharts";

export default function CustomShefasebaBar({color, data}) {

    const formatPercentage = (value) => {
      return value;
    };

    const displayPrecent = (v) =>{
      return v.ქულა;
    }

  return (
    <BarChart
      width={1280}
      height={500}
      data={data}
      layout={'horizontal'}
      margin={{
        top: 20,
        right: 30,
        left: 20,
        bottom: 5
      }}
    >
      <CartesianGrid fill="#fff" strokeDasharray="3 3" />
      <XAxis dataKey="name" />

      <YAxis
        tickCount={8}
        tickFormatter={formatPercentage}
      /> 

      <Tooltip />
      <Bar
        dataKey={'ქულა'}
        fill={color}
        background={{ fill: "#eee" }}
        barSize={50} // Adjust the bar size based on the maximum value
        radius={[20, 20, 0, 0]}
      >
        <LabelList dataKey={displayPrecent} position="top" />
      </Bar>
    </BarChart>
  );
}
