import useUpdateSubject from "./useUpdateSubject";
import {useState} from "react";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {Edit} from "@material-ui/icons";
import SubjectModal from "./SubjectModal";
import {ModalOpenMode} from "../../../utils/constants";



const EditSubject = ({ data, disabled = false }) => {
    const [open, setOpen] = useState(false);
    const {mutate: onUpdate} = useUpdateSubject();
    return (
        <>
            <IconButtonWithTooltip
                tooltip={"რედაქტირება"}
                onClick={() => setOpen(true)}
                icon={<Edit/>}
                disabled={disabled}
            />
            {open && (
                <SubjectModal
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

export default EditSubject;