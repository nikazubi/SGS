import React from 'react';
import {createHandleFormSubmit} from "../../utils/helpers";
import Modal from "./Modal";


const FormModalView = ({
  form: Form,
  formProps,
  initialValues,
  values,
  errors,
  touched,
  onSubmit,
  handleChange,
  setFieldValue,
  setValues,
  setChanged,
  showEntityInformation,
  ...rest
}) => {

  const handleSubmit = createHandleFormSubmit(onSubmit);

  let formikProps = {
    values: values || initialValues,
    errors,
    touched,
    handleChange,
    setFieldValue,
    setValues
  };
  if (!!formProps && typeof formProps === 'object') {
    formikProps = {...formikProps, ...formProps};
  }
  return (
    <Modal
      onSubmit={handleSubmit}
      initialValues={initialValues}
      showEntityInformation={showEntityInformation}
      {...rest}
    >
      <form onSubmit={handleSubmit} style={{height: '100%'}} onChange={() => setChanged(true)}>
        <Form {...formikProps} />
      </form>
    </Modal>
  );
};

export default FormModalView;
