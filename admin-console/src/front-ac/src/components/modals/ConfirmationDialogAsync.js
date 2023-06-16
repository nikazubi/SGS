import React, { useState } from 'react';
import Modal from './Modal';


const ConfirmationDialogAsync = ({
                             title,
                             open,
                             contentText,
                             saveText,
                             cancelText,
                             refresh = false,
                             refetch,
                             onSubmit,
                             onClose,
                             onSuccess = () => {},
                           }) => {
  const [submitting, setSubmitting] = useState(false);

  const defaultFormSuccess = () => {
    return () => {
      // if (refresh === true) {
      //   if (!!tableRef && !!tableRef.current) {
      //     tableRef.current.onQueryChange();
      //   }
      // }
      onSuccess();
    }
  };

  const handleSubmit = () => {
    setSubmitting(true);
    onSubmit()
      .then(() => {
          setSubmitting(false);
          onClose()
        // handleModalFormSuccess({
        //   setNotification,
        //   onClose,
        //   onFormSuccess: defaultFormSuccess(),
        //   t
        // });
        if (refetch && typeof refetch === 'function') {
          refetch();
        }
      })
      .finally(() => {
        setSubmitting(false);
        onClose();
      });
  };

  return (
    <Modal
      minHeight={50}
      title={title}
      open={open}
      onClose={onClose}
      submitting={submitting}
      saveText={saveText ?? "დიახ"}
      cancelText={cancelText ?? "არა"}
      disableBackdropClick={submitting}
      disableEscapeKeyDown={submitting}
      onSubmit={handleSubmit}
    >
      {!!contentText ? contentText : ''}
    </Modal>
  );
};

export default ConfirmationDialogAsync;
