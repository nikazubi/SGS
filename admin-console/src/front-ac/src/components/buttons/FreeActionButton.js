import React from 'react';
import Button from '@material-ui/core/Button';

const FreeActionButton = React.forwardRef((
  {
    icon,
    title,
    onClick,
    ...rest
  }, ref) => (
  <Button
    ref={ref}
    variant="contained"
    color="primary"
    startIcon={icon ? icon : null}
    onClick={onClick}
    style={{padding: '3.5px 16px'}}
    size="medium"
    {...rest}
  >
    {title}
  </Button>
));

export default FreeActionButton;
