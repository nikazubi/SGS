import React, { useCallback, useState } from 'react';
import * as S from './styles';
import IconButton from '@material-ui/core/IconButton';
import Refresh from '@material-ui/icons/Refresh';
import Alert from '@material-ui/lab/Alert';
import FlexBox from './FlexBox';
import Typography from '@material-ui/core/Typography';
import { useUserContext } from "../contexts/user-context";
import {useConvertError} from "../hooks/convertError";

const ErrorPage = ({
                     message,
                     error,
                     onReload,
                     hasReload = true,
                     style,
                     logout: receivedLogout
                   }) => {
  const {convert} = useConvertError();
  let {logout} = useUserContext();

  if (!logout) {
    logout = receivedLogout;
  }

  const handleReload = () => {
    if (onReload && typeof onReload === 'function') {
      onReload();
    } else {
      window.location.reload();
    }
  };

  const errorData = convert(error);
  if (errorData?.statusCode === 401) {
    logout();
  }
  message = !message && !!errorData ? errorData.message : message;

  return (
    <S.ErrorPage style={style}>
      <Alert severity="error" style={{alignItems: 'center'}}>
        <FlexBox style={{maxWidth: 800}} alignItems="center">
          <Typography variant="subtitle1">
            {message || 'unexpectedError'}
          </Typography>

          {hasReload &&
          <IconButton
            style={{marginLeft: 5}}
            color="primary"
            onClick={handleReload}
          >
            <Refresh/>
          </IconButton>
          }
        </FlexBox>
      </Alert>

    </S.ErrorPage>
  );
};

export default ErrorPage;
