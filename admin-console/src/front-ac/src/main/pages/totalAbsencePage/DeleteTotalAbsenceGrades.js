import useDeleteTotalAcademicHour from "./useDeleteTotalAcademicHour";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {Delete} from "@material-ui/icons";
import ConfirmationModal from "../../../components/modals/ConfirmationModal";
import {useState} from "react";

const DeleteTotalAbsenceGrades = ({data}) =>{
    const [open, setOpen] = useState(false);
    const {mutate: onDelete} = useDeleteTotalAcademicHour();

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
                    title={'თვიური აკადემიური საათის წაშლა'}
                    contentText={'ნამდვილად გსურთ თვიური აკადემიური საათის წაშლა?'}
                    onSubmit={options => onDelete(data.id, options)}
                    onClose={() => setOpen(false)}
                />
            )}
        </>
    )

}

export default DeleteTotalAbsenceGrades