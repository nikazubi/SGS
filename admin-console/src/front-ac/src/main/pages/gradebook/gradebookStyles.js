/**
 * Cell states.
 *
 * A teacher needs to tell at a glance which numbers they typed, which the
 * system worked out, which they have overruled, and which they can no longer
 * change without the director. The old grid rendered all of them identically.
 */
export const gradebookGridSx = {
    "& .MuiDataGrid-columnHeader, & .MuiDataGrid-cell": {
        border: "1px solid #cce1ea"
    },

    // The number spinner steals width from a two-character mark.
    "& .MuiDataGrid-editInputCell input[type=number]::-webkit-outer-spin-button": {
        WebkitAppearance: "none",
        margin: 0
    },
    "& .MuiDataGrid-editInputCell input[type=number]::-webkit-inner-spin-button": {
        WebkitAppearance: "none",
        margin: 0
    },
    "& .MuiDataGrid-editInputCell input": {
        textAlign: "center",
        padding: 0
    },

    // On the printed scale rather than the stored one. Marked because the two
    // are both plausible numbers - a 10 and a 7 look equally like a real mark,
    // so nothing but the styling says which scale is on screen.
    "& .sgs-cell--converted": {
        color: "#8a6d3b",
        fontWeight: 600
    },

    // Calculated: present but visibly not typed by a person.
    "& .sgs-cell--derived": {
        backgroundColor: "#f4f8fa",
        fontWeight: 500
    },

    // Overridden: a formula is a convenience, not a cage - but the fact that
    // someone overruled it should be obvious.
    "& .sgs-cell--override": {
        backgroundColor: "#fff8e6"
    },
    "& .sgs-override-mark": {
        color: "#c08a2e",
        marginLeft: 3,
        fontSize: 14
    },

    // Recomputing server-side. The number showing is the previous one, so it
    // is dimmed rather than left looking current.
    "& .sgs-cell--stale": {
        opacity: 0.45
    },
    "& .sgs-recalculating": {
        color: "#9bb0bb",
        letterSpacing: 2
    },

    // Published to parents: read-only until a change request is approved.
    "& .sgs-cell--published": {
        backgroundColor: "#eef0f2",
        color: "#5c6970"
    },

    // Refused - a competing edit, a publication lock, or a locked column.
    "& .sgs-cell--conflict": {
        outline: "2px solid #d9534f",
        outlineOffset: -2,
        backgroundColor: "#fdf2f2"
    }
};
