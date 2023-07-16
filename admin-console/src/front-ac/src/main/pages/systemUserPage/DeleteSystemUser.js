import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import {Delete} from "@material-ui/icons";
import ConfirmationModal from "../../../components/modals/ConfirmationModal";
import {useState} from "react";
import useDeleteSystemUser from "./useDeleteSystemuser";

const DeleteTotalSystemUser = ({data}) =>{
    const [open, setOpen] = useState(false);
    const {mutate: onDelete} = useDeleteSystemUser();

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
                    title={'სისტემური მომხმარებლის წაშლა'}
                    contentText={'ნამდვილად გსურთ სისტემური მომხმარებლის წაშლა?'}
                    onSubmit={options => onDelete(data.id, options)}
                    onClose={() => setOpen(false)}
                />
            )}
        </>
    )

}

export default DeleteTotalSystemUser