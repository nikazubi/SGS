import React from "react";
import {Check, Visibility} from "@material-ui/icons";
import IconButtonWithTooltip from "../../../components/buttons/IconButtonWithTooltip";
import axios from "../../../utils/axios";

export const fetchChangeRequest = async (filters) => {
    const {data} = await axios.post("change-request/change-request-status");
    return data;
}

const ChangeRequestStatusChangeAction = ({ row, status, tooltip, icon }) => {

    const handleOpen = async () => {
        const {data} = await axios.put("change-request/change-request-status", {
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