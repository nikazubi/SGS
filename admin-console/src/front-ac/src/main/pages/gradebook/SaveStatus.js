import React from "react";
import {Button, CircularProgress, Tooltip} from "@mui/material";
import {CheckCircleOutline, ErrorOutline, Save} from "@mui/icons-material";

/**
 * Whether the teacher's work is safe.
 *
 * Autosave without a visible state is a promise nobody can check, and marks
 * matter enough to say so plainly rather than saving quietly and hoping.
 */
const SaveStatus = ({status, pending, onFlush}) => {

    if (status === "saving") {
        return (
            <span style={{display: "flex", alignItems: "center", color: "#5b7c8d"}}>
                <CircularProgress size={16} thickness={5} style={{marginRight: 8}}/>
                ინახება…
            </span>
        );
    }

    if (status === "dirty") {
        return (
            <Tooltip title="შენახვა ავტომატურად ხდება — დაუყოვნებლივ შესანახად დააჭირეთ">
                <Button size="small" startIcon={<Save/>} onClick={onFlush}
                        style={{textTransform: "none", color: "#8a6d3b"}}>
                    {`შეუნახავი: ${pending}`}
                </Button>
            </Tooltip>
        );
    }

    if (status === "error") {
        return (
            <Tooltip title="ზოგიერთი უჯრა ვერ შეინახა — იხილეთ მონიშნული უჯრები">
                <Button size="small" startIcon={<ErrorOutline/>} onClick={onFlush}
                        style={{textTransform: "none", color: "#a94442"}}>
                    ხელახლა შენახვა
                </Button>
            </Tooltip>
        );
    }

    if (status === "saved") {
        return (
            <span style={{display: "flex", alignItems: "center", color: "#3c763d"}}>
                <CheckCircleOutline fontSize="small" style={{marginRight: 6}}/>
                შენახულია
            </span>
        );
    }

    return <span/>;
};

export default SaveStatus;
