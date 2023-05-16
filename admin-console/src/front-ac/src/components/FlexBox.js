import React from 'react';
import Box from "@mui/material/Box";

const FlexBox = ({ children, ...rest }) => (
  <Box display='flex' {...rest}>
    {children}
  </Box>
);

export default FlexBox;
