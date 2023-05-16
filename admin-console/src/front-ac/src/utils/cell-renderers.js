import React from 'react';
import CheckCircle from '@material-ui/icons/CheckCircle';
import { Info } from '@material-ui/icons';
import { Box, Tooltip } from '@material-ui/core';

export const renderBooleanCell = (value) => {
  if (typeof value !== 'boolean') {
    return null;
  }

  return (
    <CheckCircle style={{ color: `${value ? '#4caf50' : '#ccc'}` }}/>
  );
};

export const StatusCell = ({ color, tooltip, status }) => (
  <Box color={color} display="flex" alignItems="center">
    <Box pr={1}>
      {status}
    </Box>
    {!!tooltip && (
      <Tooltip title={tooltip}>
        <Info/>
      </Tooltip>
    )}
  </Box>  
)

export const cashCellProps = {
  cellStyle: {
    paddingRight: '10px',
    textAlign: 'right',
    whiteSpace: 'nowrap',
  },
  headerStyle: {
    textAlign: 'right'
  }
}
