import React, { useState } from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  LabelList
} from "recharts";



// Custom tick formatter function to display values as percentages
// const formatPercentage = (value) => {
//     console.log(value, 'TEST123 JIB')
//   if (value === 3000) return "100%"; // Maximum value
//   return `${((value / 3000) * 100).toFixed(1)}%`;
// };

export default function CustomBar({color, attend, attendMax, layout, data}) {

    // const data = [
    //     {
    //       name: label,
    //       [keyLabel]: attend, // Adjust the value to match your data
    //     },
    //   ];

    const formatPercentage = (value) => {
        console.log(value, 'TEST123 JIB')
      if (value === attendMax) return "100%"; // Maximum value
      return `${((value / attendMax) * 100)}%`;
    };

    const displayPrecent = (v) =>{
      return (((v.არა / attendMax) * 100).toFixed(2) + '%')
    }

  return (
    <ResponsiveContainer width="100%" height={500}>
    <BarChart
      width={1280}
      height={500}
      data={data}
      layout={layout}
      margin={{
        top: 15,
        right: 30,
        left: 20,
        bottom: 5
      }}

    >
      <CartesianGrid fill="#fff" strokeDasharray="3 3" />
      {layout === 'horizontal' ? <XAxis dataKey="name" /> :
      <XAxis type="number"
        domain={[0, attendMax]}
        tickFormatter={formatPercentage}
      />}

      {layout === 'horizontal' ? <YAxis
        domain={[0, attendMax]}
        tickFormatter={formatPercentage}
      /> : <YAxis dataKey="name" type="category" />}

      <Tooltip />
      {layout === 'horizontal' ? '' : <Legend formatter={()=>((attend/attendMax) * 100).toFixed(2) + '%'} />}
      <Bar
        dataKey={'არა'}
        fill={color}
        background={{ fill: "#eee" }}
        barSize={layout === 'horizontal' ? 50 : 100} // Adjust the bar size based on the maximum value
        radius={layout === 'horizontal' ? [20, 20, 0, 0] : [0, 20, 20, 0]}
      >
        <LabelList dataKey={displayPrecent} position="top" />
      </Bar>
    </BarChart>
    </ResponsiveContainer>
  );
}