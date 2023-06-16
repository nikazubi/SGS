import React from 'react';
import Box from '@material-ui/core/Box';
import * as S from './styles';
import DraggableDialog from './DraggableDialog';
import ActionFooter from "./ActionFooter";

const Modal = ({
  title,
  height,
  width,
  errorModal,
  closable = true,
  minWidth = 400,
  minHeight = 150,
  maxWidth = 1200,
  maxHeight = 600,
  padding = 2,
  hasActions = true,
  saveText,
  cancelText,
  submitting = false,
  disableSubmit = false,
  onClose,
  onSubmit,
  submitButton,
  error,
  children,
  style,
  initialValues,
  showEntityInformation,
  ...rest
}) => (
  <DraggableDialog
    onClose={onClose}
    maxWidth={false}
    focus={!hasActions}
    title={title}
    initialValues={initialValues}
    showEntityInformation={showEntityInformation}
    {...rest}
  >
    <S.ModalContent>
      <Box
        p={padding}
        height={height}
        width={width}
        maxWidth={maxWidth}
        minWidth={minWidth}
        minHeight={minHeight}
        maxHeight={maxHeight}
        style={style}
      >
        {children}
      </Box>
    </S.ModalContent>
    {hasActions && <ActionFooter
      saveText={saveText}
      cancelText={cancelText}
      onSubmit={onSubmit}
      onClose={onClose}
      submitting={submitting}
      closable={closable}
      disableSubmit={disableSubmit}
      submitButton={submitButton}
      error={error}
      initialValues={initialValues}
      showEntityInformation={showEntityInformation}
    />}
  </DraggableDialog>
);

export default Modal;
