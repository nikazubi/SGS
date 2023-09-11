import {useState} from "react";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {Edit} from "@material-ui/icons";
import {ModalOpenMode} from "../../../utils/constants";
import SystemUserModal from "./SystemuserFormModal";
import useUpdateSystemUser from "./useUpdateSystemUser";


const EditSystemUser = ({data, disabled = false}) => {
    const [open, setOpen] = useState(false);
    const {mutateAsync: onUpdate} = useUpdateSystemUser();

    return (
        <>
            <IconButtonWithTooltip
                tooltip={"რედაქტირება"}
                onClick={() => setOpen(true)}
                icon={<Edit/>}
                disabled={disabled}
            />
            {open && (
                <SystemUserModal
                    data={data}
                    open={open}
                    onSubmit={onUpdate}
                    onClose={() => setOpen(false)}
                    modalOpenMode={ModalOpenMode.edit}
                />
            )}
        </>
    )
}

export default EditSystemUser;