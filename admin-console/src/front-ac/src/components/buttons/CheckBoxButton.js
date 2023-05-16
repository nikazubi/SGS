import CheckBoxIcon from '@material-ui/icons/CheckBox';
import CheckBoxOutlineBlankIcon from '@material-ui/icons/CheckBoxOutlineBlank';
import IconButton from "./IconButton";
import { useToggle } from "../../../hooks/useToggle";
import ConfirmationModal from "../modals/ConfirmationModal";

const CheckBoxButton = ({
  value,
  enabled,
  onChange,
  content,
}) => {
  const {open, handleOpen, handleClose} = useToggle();
  return <>
    <IconButton
      onClick={handleOpen}
      disabled={!enabled}
      icon={!!value ? <CheckBoxIcon/> : <CheckBoxOutlineBlankIcon/>}
    />
    <ConfirmationModal
      open={open}
      onClose={handleClose}
      onSubmit={onChange}
      contentText={content}
    />
  </>;

};

export default CheckBoxButton;