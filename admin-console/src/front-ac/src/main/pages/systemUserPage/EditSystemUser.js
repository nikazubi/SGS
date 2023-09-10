
import {useState} from "react";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {Edit} from "@material-ui/icons";
import {ModalOpenMode} from "../../../utils/constants";
import SystemUserModal from "./SystemuserFormModal";


const EditSystemUser = ({data, disabled = false}) => {
    const [open, setOpen] = useState(false);

    console.log("shexeeeee aqaaa", data)
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
                    onClose={() => setOpen(false)}
                    modalOpenMode={ModalOpenMode.edit}
                />
            )}
        </>
    )
}

export default EditSystemUser;