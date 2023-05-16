import React from 'react';
import Button from './Button';
import styled from '@material-ui/core/styles/styled';

const StyledButton = styled(Button)({
  alignSelf: 'center'
});

const SmallButton = ({ ...props }) => (
  <StyledButton
    variant='contained'
    size='small'
    margin='dense'
    {...props}
  />
);

export default SmallButton;
