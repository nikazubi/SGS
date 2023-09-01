import { Formik } from 'formik';
import React, { useCallback, useState } from 'react';
import FormModalView from './FormModalView';
import {handleModalFormSuccess} from "../../utils/helpers";
import ConfirmationModal from "../modals/ConfirmationModal";
import {useTableRef} from "../../contexts/table-ref-context";
import {useNotification} from "../../contexts/notification-context";


const FormModal = ({
  resolveSubmitData,
  onSubmit,
  onSuccess,
  width,
  initialValues,
  refetch,
  validationSchema,
  onClose,
  onChange = (values) => values,
  validate = () => {},
  isDataTableModal = true,
  shouldSubmitData = true,
  showEntityInformation = true,
  closeOnSuccess = true,
  ...rest
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { tableRef } = useTableRef();
  const { setNotification, setErrorMessage } = useNotification();
  const [ confirmDialogOpen, setConfirmDialogOpen] = useState(false)
  const [ changed, setChanged ] = useState(false)

  let onFormSuccess = null;
  if (isDataTableModal) {
    onFormSuccess = () => {
      if (!!tableRef && !!tableRef.current) {
        tableRef.current.onQueryChange();
      }
    };
  }

  const handleSubmit = async (values, { resetForm, setValues }) => {
    if (shouldSubmitData) {
      setIsSubmitting(true);
    }
    let resolvedData = values;
    if (typeof resolveSubmitData === 'function') {
      resolvedData = resolveSubmitData(values);
    }

    onSubmit(resolvedData, {
      onSuccess: (response) => {
        setIsSubmitting(false);
        if (typeof onSuccess === 'function') {
          onSuccess({ values, resetForm, response });
        }
        handleModalFormSuccess({
          setNotification,
          onClose,
          resetForm,
          onFormSuccess,
          closeOnSuccess,
        });

      },
      onError: error => {
        setIsSubmitting(false);
        setErrorMessage(error);
      }
    }, {
      resetForm,
      setValues,
    });
  };



  const switchConfirmationDialog = useCallback( () => {
    setConfirmDialogOpen(!confirmDialogOpen)
  }, [confirmDialogOpen, setConfirmDialogOpen])

  const handleConfirmationDialogConfirm = () => {
    setConfirmDialogOpen(false)
    onClose()
  }

  const handleConfirmationDialogCancel = () => {
    setConfirmDialogOpen(false)
  }

  const handleModalClose = () => () => {
    if (changed) {
      rest.disableEscapeKeyDown = true
      switchConfirmationDialog()
    } else {
      onClose();
    }
  };

  return (
    <>
      <Formik
        initialValues={initialValues}
        validationSchema={validationSchema}
        onSubmit={handleSubmit}
        enableReinitialize
        validate={async values => {
          onChange(values);
          return await validate(values);
        }}
      >
        {({ values, errors, error, touched, handleChange, setFieldValue, setValues, handleSubmit }) => (
          <FormModalView
            setChanged={setChanged}
            initialValues={initialValues}
            submitting={isSubmitting}
            values={values}
            errors={errors}
            touched={touched}
            width={width}
            error={error}
            onClose={handleModalClose()}
            handleChange={handleChange}
            setFieldValue={setFieldValue}
            setValues={setValues}
            onSubmit={handleSubmit}
            showEntityInformation={showEntityInformation}
            {...rest}
          />
        )}
      </Formik>
      {confirmDialogOpen && (
        <ConfirmationModal
          title={'დაადასტურეთ გაუქმება'}
          contentText={'ნამდვილად გსურთ რედაქტირების გაუქმება?'}
          open={confirmDialogOpen}
          onSubmit={handleConfirmationDialogConfirm}
          onClose={handleConfirmationDialogCancel}
          saveText={'დიახ'}
          cancelText={'დახურვა'}
        />
      )}
    </>
  );
};

export default FormModal;
