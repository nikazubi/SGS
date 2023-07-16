import React, { useEffect, useState } from 'react';
import { LineChart, Line, XAxis, YAxis,  CartesianGrid, Tooltip, activeDot, LabelList } from 'recharts';

const pData = [5, 3, 4, 5, 6, 7, 2, 5];
const xLabels = [
  'სექტემბერი-ოქტომბერი',
  'ნოემბერი',
  'დეკემბერი',
  'იანვარი-თებერვალი',
  'მარტი',
  'აპრილი',
  'მაისი',
  'ივნისი'
];


const CustomizedLabel = (props) => {
    const { x, y, stroke, value } = props;
  
    return (
      <text x={x} y={y - 8} dy={-4} fill={stroke} fontSize={16} textAnchor="middle">
        {value}
      </text>
    );
  };

const handleLineClick = (event, data) => {
    // Handle the line click event here
    //data.index 0 means => სექტემბერი
    //data.payload.label => 'სექტემბერი'
    //API semestruli

    //ak aris axios requesti
    console.log('Line clicked:', data);
    
};

export default function Chart() {

  const [isHovered, setIsHovered] = useState(false)

  useEffect(()=>{
    if (isHovered){
        document.querySelector('.recharts-wrapper').style.cursor = "pointer"
    }

    else{
        document.querySelector('.recharts-wrapper').style.cursor = "unset"
    }

  },[isHovered])

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
    <LineChart width={1200} height={300} data={xLabels.map((label, index) => ({ label, value: pData[index] }))} >
      <CartesianGrid strokeDasharray="0" vertical={false} />
      <XAxis style={{cursor:'pointer'}} onClick={handleLineClick} padding={{ left: 80, right: 80 }} angle={0} dataKey="label" height={60} tick={<CustomTick />} />
      <YAxis />
      <Tooltip active={true} cursor={false} onClick={handleLineClick} wrapperStyle={{display: 'none'}} />
      <Line type="linear" dataKey="value" stroke="#009688" dot={{r:4, cursor: 'pointer'}} cursor={'pointer'} activeDot={{ onClick: handleLineClick, r: 9, onMouseLeave: () => setIsHovered(false), onMouseEnter: () => setIsHovered(true) }} >
      <LabelList content={<CustomizedLabel />} />
      </Line>
    </LineChart>
  );
}
