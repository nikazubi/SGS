import React from 'react';
import MTButton from '@material-ui/core/Button';

const Button = ({
  color = 'primary',
  children,
  variant = 'contained',
  ...rest
}) => (
  <MTButton
    color={color}
    variant={variant}
    {...rest}
  >
    {children}
  </MTButton>
);

export default Button;
