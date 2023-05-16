import CircularProgress from '@material-ui/core/CircularProgress';
import styled from '@material-ui/core/styles/styled';

export const SubmitButtonWrapper = styled('div')({
  position: 'relative',
  display: 'inline-flex',
  alignItems: 'center'
});

export const SubmitButtonProgress = styled(CircularProgress)(({ theme }) => ({
  color: theme.palette.secondary.main,
}));

export const SpinnerWrapper = styled('div')(({ theme }) => ({
  position: 'absolute',
  width: '100%',
  height: '100%',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center'
}));
