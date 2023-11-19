import React from "react";
import {Bar, BarChart, CartesianGrid, LabelList, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis} from "recharts";


// Custom tick formatter function to display values as percentages
// const formatPercentage = (value) => {
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
      if (value === attendMax) return "100%"; // Maximum value
      return `${((value / attendMax) * 100).toFixed(2)}%`;
    };

    const displayPrecent = (v) =>{
        return (((v.value / attendMax) * 100).toFixed(2) + '%')
    }

  return (
      <ResponsiveContainer width="100%" height={220}>
          <BarChart
              width={1280}
              height={300}
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
          dataKey={'value'}
          fill={color}
          background={{ fill: "#eee" }}
          barSize={layout === 'horizontal' ? 50 : 70} // Adjust the bar size based on the maximum value
          radius={layout === 'horizontal' ? [20, 20, 0, 0] : [0, 20, 20, 0]}
      >
        <LabelList dataKey={displayPrecent} position="top" />
      </Bar>
    </BarChart>
    </ResponsiveContainer>
  );
}
