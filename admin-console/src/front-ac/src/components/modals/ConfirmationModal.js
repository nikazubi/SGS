import React, { useState } from 'react';
import Modal from './Modal';
import {useNotification} from "../../contexts/notification-context";


const ConfirmationModal = ({
                             title,
                             open,
                             contentText,
                             saveText,
                             cancelText,
                             refetch,
                             onSubmit,
                             onClose,
                             onSuccess = () => {
                             },
                           }) => {
  const [submitting, setSubmitting] = useState(false);
    const { setNotification, setErrorMessage } = useNotification();

  const defaultFormSuccess = () => {
    return () => {
      // if (!!tableRef && !!tableRef.current) {
      //   tableRef.current.onQueryChange();
      // }
      onSuccess();
    };
  };

  const handleSubmit = () => {
    setSubmitting(true);
    onSubmit({
      onSuccess: () => {
        // handleModalFormSuccess({
        //   setNotification,
        //   onClose,
        //   onFormSuccess: defaultFormSuccess(),
        //   t
        // });
        if (refetch && typeof refetch === 'function') {
          refetch();
        }
      },
      onError: (error) => {
        setErrorMessage(error);
      },
      onSettled: () => {
        setSubmitting(false);
        onClose();
      },
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
      {!!contentText ? contentText : "ნამდვილად გსურთ ნიშნის შეცვლის მოთხოვნის გაგზავნა?"}
    </Modal>
  );
};

export default ConfirmationModal;
