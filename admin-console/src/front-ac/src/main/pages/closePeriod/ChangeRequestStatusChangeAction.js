import React from "react";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import axios from "../../../utils/axios";
import useChangeRequestStatus from "./useUpdateChangeRequest";

export const fetchChangeRequest = async (filters) => {
    const {data} = await axios.post("change-request/change-request-status");
    return data;
}

const ChangeRequestStatusChangeAction = ({row, status, tooltip, icon}) => {
    const {mutateAsync: changeRequestStatus} = useChangeRequestStatus();

    const handleOpen = async () => {
        const {data} = await changeRequestStatus({
            changeRequestId: row.id,
            changeRequestStatus: status
        });
        return data;
    }

    return (
        <IconButtonWithTooltip
            icon={icon}
            onClick={handleOpen}
            tooltip={tooltip}
            disabled={row.status !== "PENDING"}
        />
    )
}

export default ChangeRequestStatusChangeAction;