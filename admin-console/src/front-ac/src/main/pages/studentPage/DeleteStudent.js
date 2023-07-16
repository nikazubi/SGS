import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {Delete} from "@material-ui/icons";
import ConfirmationModal from "../../../components/modals/ConfirmationModal";
import {useState} from "react";
import useDeleteStudents from "./useDeleteStudents";

const DeleteStudent = ({data}) =>{
    const [open, setOpen] = useState(false);
    const {mutate: onDelete} = useDeleteStudents();

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

export default DeleteStudent