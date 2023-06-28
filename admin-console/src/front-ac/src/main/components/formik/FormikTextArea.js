import React from 'react';
import TextField from './TextField';
import { useField } from 'formik';

const FormikTextArea = ({ label, ...props }) => {
  const [field, meta] = useField(props);
  const errorText = meta.error && meta.touched ? meta.error : '';

  return (
    <TextField
      multiline
      rows={props.rows}
      helperText={errorText !== ' ' && errorText}
      error={!!errorText}
      label={label || props.name}
      {...field}
      value={field.value || ''}
      {...props}
    />
  );
};

export default FormikTextArea;
