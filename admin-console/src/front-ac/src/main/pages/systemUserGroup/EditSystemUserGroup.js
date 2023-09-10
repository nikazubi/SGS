import useUpdateSystemUserGroup from "./useUpdateSystemUserGroup";
import {useState} from "react";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {Edit} from "@material-ui/icons";
import AcademyClassModal from "./SystemUserGroupModal";
import {ModalOpenMode} from "../../../utils/constants";
import {PERMISSIONS} from "./permissions";


const EditSystemUserGroup = ({data, disabled = false}) => {
    const [open, setOpen] = useState(false);
    const {mutate: onUpdate} = useUpdateSystemUserGroup();
    const parsedData = data.permissions.toString().split(",")
        .map(val => {
            return {value: val, label: PERMISSIONS[val]}
        })
    console.log(parsedData)
    return (
        <>
            <IconButtonWithTooltip
                tooltip={"რედაქტირება"}
                onClick={() => setOpen(true)}
                icon={<Edit/>}
                disabled={disabled}
            />
            {open && (
                <AcademyClassModal
                    open={open}
                    subject={parsedData}
                    onSubmit={onUpdate}
                    onClose={() => setOpen(false)}
                    modalOpenMode={ModalOpenMode.edit}
                />
            )}
        </>
    )
}

export default EditSystemUserGroup;