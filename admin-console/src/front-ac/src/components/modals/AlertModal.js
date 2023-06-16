import React from 'react';
import * as S from './styles';
import Divider from '@material-ui/core/Divider';
import Button from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';
import DraggableDialog from './DraggableDialog';

const AlertModal = ({
  open,
  title,
  children,
  acceptText,
  closable,
  onClose,
  onAccept,
}) => (
  <DraggableDialog
    open={open}
    title={title}
    closable={closable}
    onClose={onClose}
  >
    <S.AlertModalContent>
      <S.ErrorIcon/>
      <Typography variant='body2'>
        {children}
      </Typography>
    </S.AlertModalContent>
    <Divider/>
    <S.AlertModalActions>
      <Button
        variant="contained"
        size='small'
        color="primary"
        onClick={onAccept}
        autoFocus
      >
        {acceptText}
      </Button>
    </S.AlertModalActions>
  </DraggableDialog>
);

export default AlertModal;
