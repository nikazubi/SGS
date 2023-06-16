import React from 'react';
import * as S from './styles';
import Tooltip from '@material-ui/core/Tooltip';
import Dialog from '@material-ui/core/Dialog';
import Divider from '@material-ui/core/Divider';
import Close from '@material-ui/icons/Close';
import Draggable from 'react-draggable';


const DraggableDialog = ({
  title,
  children,
  focus,
  closable = true,
  onClose,
  initialValues,
  showEntityInformation,
  ...rest
}) => {

  return (
    <Dialog
      TransitionComponent={Draggable}
      TransitionProps={{ handle: '.modalTitle' }}
      disableScrollLock={true}
      onClose={onClose}
      {...rest}
    >
      <S.ModalTitle classes={{ root: 'modalTitle' }}>
        {title}
        {showEntityInformation && (
          <div
            style={{marginLeft: "10px", color: "#b3aabf", flexGrow: 1, fontSize: "14px"}}
          >
            {!!initialValues?.id ? `ID: ${initialValues.id}` : ""}
          </div>
        )}
        {closable && (
          <Tooltip title={"დახურვა"}>
            <S.ModalCloseIcon onClick={onClose} autoFocus={focus}>
              <Close/>
            </S.ModalCloseIcon>
          </Tooltip>
        )}
      </S.ModalTitle>
      <Divider/>
      {children}
    </Dialog>
  );
};

export default DraggableDialog;
