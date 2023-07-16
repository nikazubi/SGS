import useUpdateStudent from "./useUpdateStudent";
import {useState} from "react";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {Edit} from "@material-ui/icons";
import StudentModal from "./StudentModal";
import {ModalOpenMode} from "../../../utils/constants";



const EditStudent = ({ data, disabled = false }) => {
    const [open, setOpen] = useState(false);
    const {mutate: onUpdate} = useUpdateStudent();
    return (
        <>
            <IconButtonWithTooltip
                tooltip={"რედაქტირება"}
                onClick={() => setOpen(true)}
                icon={<Edit/>}
                disabled={disabled}
            />
            {open && (
                <StudentModal
                    open={open}
                    subject={data}
                    onSubmit={onUpdate}
                    onClose={() => setOpen(false)}
                    modalOpenMode={ModalOpenMode.edit}
                />
            )}
        </>
    )
}

export default EditStudent;