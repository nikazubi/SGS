import React from 'react';
import { LineChart, Line, XAxis, YAxis,  CartesianGrid, Tooltip, activeDot, LabelList } from 'recharts';

const pData = [5, 2, 3, 4, 5, 6, 7, 2, 1, 5];
const xLabels = [
  'სექტემბერი',
  'ოქტომბერი',
  'ნოემბერი',
  'დეკემბერი',
  'იანვარი',
  'თებერვალი',
  'მარტი',
  'აპრილი',
  'მაისი',
  'ივნისი'
];


const CustomizedLabel = (props) => {
    const { x, y, stroke, value } = props;
  
    return (
      <text x={x} y={y - 8} dy={-4} fill={stroke} fontSize={12} textAnchor="middle">
        {value}
      </text>
    );
  };

const handleLineClick = (event, data) => {
    // Handle the line click event here
    console.log('Line clicked:', data);
  };

export default function Chart() {
  const CustomTick = (props) => {
    const { x, y, payload } = props;

    return (
      <g width={500} transform={`translate(${x},${y})`}>
        <text x={0} y={0} dy={16} textAnchor="middle" fill="#666">
          {payload.value}
        </text>
      </g>
    );
  };

  return (
    <LineChart width={1200} height={300} data={xLabels.map((label, index) => ({ label, value: pData[index] }))}>
      <CartesianGrid onClick={()=>console.log('object1')} strokeDasharray="0" vertical={false} />
      <XAxis padding={{ left: 80, right: 80 }} angle={0} dataKey="label" height={60} tick={<CustomTick />} />
      <YAxis />
      <Tooltip />
      <Line  type="linear" dataKey="value" stroke="#009688" dot={{r:4}} activeDot={{ onClick: handleLineClick, r: 9  }} >
      <LabelList content={<CustomizedLabel />} />
      </Line>
    </LineChart>
  );
}
