import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {Delete} from "@material-ui/icons";
import ConfirmationModal from "../../../components/modals/ConfirmationModal";
import {useState} from "react";
import useDeleteAcademyClass from "./useDeleteAcademyClass";

const DeleteTotalAbsenceGrades = ({data}) =>{
    const [open, setOpen] = useState(false);
    const {mutate: onDelete} = useDeleteAcademyClass();

    return (
        <>
            <IconButtonWithTooltip
                tooltip={'წაშლა'}
                onClick={() => setOpen(true)}
                icon={<Delete/>}
                // disabled={disabled}
            />
            {open && (
                <ConfirmationModal
                    open={open}
                    title={'საგნის წაშლა'}
                    contentText={'ნამდვილად გსურთ საგნის წაშლა?'}
                    onSubmit={options => onDelete(data.id, options)}
                    onClose={() => setOpen(false)}
                />
            )}
        </>
    )

}

export default DeleteTotalAbsenceGrades