import Divider from "@material-ui/core/Divider";
import * as S from "./styles";
import SubmitButton from "../buttons/SubmitButton";
import Button from "@material-ui/core/Button";
import React from "react";

const ActionFooter = ({
                        saveText,
                        cancelText,
                        onSubmit,
                        onClose,
                        submitting = false,
                        closable = true,
                        disableSubmit = false,
                        submitButton = true,
                        error,
                        initialValues,
                        showEntityInformation
                      }) => {

  if (!saveText) {
    saveText = 'შენახვა';
  }
  if (!cancelText) {
    cancelText = 'დახურვა';
  }

  return (
    <>
      <Divider/>
      <S.Actions>
        {!submitButton ? (
          submitButton
        ) : (
          <SubmitButton
            onClick={onSubmit}
            pending={submitting}
            disabled={!!error || disableSubmit}
          >
            {saveText}
          </SubmitButton>
        )}
        {closable && <Button
          onClick={onClose}
          variant="outlined"
          color="secondary"
          disabled={submitting}
          autoFocus
        >
          {cancelText}
        </Button>}
      </S.Actions>
    </>
  );
};

export default ActionFooter;