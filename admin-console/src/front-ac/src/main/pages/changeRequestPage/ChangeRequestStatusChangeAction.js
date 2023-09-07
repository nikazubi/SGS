import React, {useState} from "react";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import axios from "../../../utils/axios";
import useChangeRequestStatus from "./useUpdateChangeRequest";
import ApproveChangeRequestFormModal from "./ApproveChangeRequestFormModal";

export const fetchChangeRequest = async (filters) => {
    const {data} = await axios.post("change-request/change-request-status");
    return data;
}

const ChangeRequestStatusChangeAction = ({row, status, tooltip, icon}) => {
    const {mutateAsync: changeRequestStatus} = useChangeRequestStatus();
    const [modalOpen, setModalOpen] = useState(false);

    const onSubmit = async ({description}) => {
        const {data} = await changeRequestStatus({
            changeRequestId: row.id,
            changeRequestStatus: status,
            description: description
        });
        return data;
    }

    return (
        <div>
            <IconButtonWithTooltip
                icon={icon}
                onClick={async () => {
                    if (status === "APPROVED") {
                        setModalOpen(true)
                    } else {
                        await onSubmit({description: ""})
                    }

                }}
                tooltip={tooltip}
                disabled={row.status !== "PENDING"}
            />
            {modalOpen && status === "APPROVED" &&
                <ApproveChangeRequestFormModal row={row} open={modalOpen} onClose={() => setModalOpen(false)}
                                               submit={onSubmit}/>

            }
        </div>
    )
}

export default ChangeRequestStatusChangeAction;