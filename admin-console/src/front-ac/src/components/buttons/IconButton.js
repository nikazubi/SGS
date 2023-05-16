import React from 'react';
import MTIconButton from '@material-ui/core/IconButton';

const IconButton = ({ icon, children, ...rest }) => (
  <MTIconButton color="primary" {...rest}>
    {icon}
  </MTIconButton>
);

export default IconButton;
