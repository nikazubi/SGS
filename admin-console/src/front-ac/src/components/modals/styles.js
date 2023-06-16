import DialogContent from '@material-ui/core/DialogContent/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogActions from '@material-ui/core/DialogActions';
import styled from '@material-ui/core/styles/styled';
import IconButton from '@material-ui/core/IconButton';
import ErrorOutline from '@material-ui/icons/ErrorOutline';

export const Actions = styled(DialogActions)({
  paddingRight: 16,
  paddingBottom: 10,
  paddingLeft: 16
});

export const AlertModalActions = styled(DialogActions)({
  padding: '7px 16px'
});


export const ModalTitle = styled(DialogTitle)({
  padding: '7px 15px',
  '&:hover': {
    cursor: 'move'
  },
  '& h2': {
    fontSize: 15,
    width: '100%',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  }
});

export const ModalCloseIcon = styled(IconButton)({
  padding: 4,
  '& icon': {
    fontSize: 15,
  }
});

export const ModalContent = styled(DialogContent)({
  padding: 0
});

export const AlertModalContent = styled(DialogContent)({
  display: 'flex',
  paddingLeft: 8,
  paddingRight: 16,
  alignItems: 'center',
  minHeight: 70
});

export const ErrorIcon = styled(ErrorOutline)(({ theme }) => ({
  color: theme.palette.primary.main,
  marginRight: theme.spacing(1),
  fontSize: 35
}));
