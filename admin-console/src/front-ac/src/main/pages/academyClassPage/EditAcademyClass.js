import useUpdateAcademyClass from "./useUpdateAcademyClass";
import {useState} from "react";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {Edit} from "@material-ui/icons";
import AcademyClassModal from "./AcademyClassModal";
import {ModalOpenMode} from "../../../utils/constants";



const EditAcademyClass = ({ data, disabled = false }) => {
    const [open, setOpen] = useState(false);
    const {mutate: onUpdate} = useUpdateAcademyClass();
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
                    subject={data}
                    onSubmit={onUpdate}
                    onClose={() => setOpen(false)}
                    modalOpenMode={ModalOpenMode.edit}
                />
            )}
        </>
    )
}

export default EditAcademyClass;