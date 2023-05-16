import React from 'react';
import * as S from './styles';
import Button from './Button';

const SubmitButton = ({
  children,
  pending,
  disabled,
  type = 'submit',
  color = 'primary',
  spinnerSize = 24,
  wrapperStyles = {},
  onClick,
  ...rest
}) => {
  return (
    <S.SubmitButtonWrapper style={wrapperStyles}>
      <Button
        type={type}
        color={color}
        disabled={disabled || pending}
        onClick={onClick}
        {...rest}
      >
        {children}
      </Button>
      {pending &&
        <S.SpinnerWrapper>
          <S.SubmitButtonProgress size={spinnerSize}/>
        </S.SpinnerWrapper>
      }
    </S.SubmitButtonWrapper>
  );
};

export default SubmitButton;
