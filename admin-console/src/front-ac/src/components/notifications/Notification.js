import React from 'react';
import Snackbar from '@material-ui/core/Snackbar';
import Alert from './Alert';
import { useNotification } from '../../contexts/notification-context';

const Notification = ({
  autoHideDuration = 6000,
  anchorOrigin = { vertical: 'bottom', horizontal: 'right' }
}) => {

  const { notification, open, removeNotification } = useNotification();

  const handleClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }
    removeNotification();
  };
  return (
    <Snackbar
      style={{ bottom: 40 }}
      open={open}
      autoHideDuration={autoHideDuration}
      onClose={handleClose}
      anchorOrigin={anchorOrigin}
    >
      <div>
        {notification &&
          <Alert onClose={handleClose} severity={notification.severity}>
            {notification.message}
          </Alert>
        }
      </div>
    </Snackbar>
  );
};

export default Notification;
