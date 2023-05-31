import React from 'react';
import MTTextField from '@material-ui/core/TextField';

const TextField = (props) => (
  <MTTextField
    variant='outlined'
    margin='dense'
    fullWidth
    {...props}
  />
);

export default TextField;
