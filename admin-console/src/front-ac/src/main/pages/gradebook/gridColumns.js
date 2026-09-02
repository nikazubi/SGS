import React from "react";
import {cellKey} from "./useGradeSheet";

/**
 * Turns the template's columns into DataGrid columns.
 *
 * The page this replaces declared eleven of these in JSX, so adding a column
 * meant a code change and a deployment, and the monthly and annual pages
 * hardcoded every subject name as well. Nothing here knows what a trimester is.
 */

const STUDENT_COLUMN_WIDTH = 240;

/** Roughly what the header needs, so a long Georgian label is not clipped. */
const widthFor = (column) => {
    if (column.kind === "DERIVED") return 150;
    const label = column.label || "";
    if (label.length <= 3) return 62;
    if (label.length <= 12) return 120;
    return Math.min(240, 70 + label.length * 7);
};

export const buildColumns = ({
                                 columns, getCell, isStale, getConflict, decimalsOf,
                                 specialLabel = (code) => code, converted = false, canEdit = true
                             }) => {

    const studentColumn = {
        field: "__student",
        headerName: "მოსწავლის გვარი, სახელი",
        sortable: false,
        editable: false,
        width: STUDENT_COLUMN_WIDTH,
        minWidth: STUDENT_COLUMN_WIDTH,
        headerAlign: "center",
        align: "left",
        renderCell: ({row}) => (
            <div style={{display: "flex", alignItems: "center", height: "100%", paddingLeft: 4}}>
                {`${row.__index}. ${row.__lastName} ${row.__firstName}`}
            </div>
        )
    };

    const gradeColumns = columns.map(column => ({
        field: column.code,
        headerName: column.label,
        sortable: false,
        // Three conditions, all necessary. The template says whether the column
        // is an input at all; converted display is never editable, because a
        // banded scale is not reversible and a typed 10 cannot be turned back
        // into the mark that produced it; and canEdit is the server's own
        // answer about this caller - it was computed, sent and read by nothing,
        // so a MANAGE_GRADES holder without ADD_GRADES got a fully editable
        // grid where every save came back 403.
        editable: column.editable && !converted && canEdit,
        align: "center",
        headerAlign: "center",
        width: widthFor(column),
        // A number type would reject ჩთ, which is a legitimate mark rather
        // than a typo, so validation lives in the cell instead.
        type: "string",

        cellClassName: ({id}) => {
            const key = cellKey(id, column.code);
            const cell = getCell(id, column.code);
            const classes = [];
            if (getConflict(id, column.code)) classes.push("sgs-cell--conflict");
            else if (cell?.published) classes.push("sgs-cell--published");
            else if (isStale(key)) classes.push("sgs-cell--stale");
            else if (cell?.override) classes.push("sgs-cell--override");
            else if (column.kind === "DERIVED") classes.push("sgs-cell--derived");
            return classes.join(" ");
        },

        renderCell: ({id, value}) => {
            const cell = getCell(id, column.code);
            const conflict = getConflict(id, column.code);

            if (isStale(cellKey(id, column.code))) {
                return <span className="sgs-recalculating">…</span>;
            }
            if (cell?.specialValue) {
                // The stored form is a code (CHT); the school reads ჩთ. Never
                // converted: ჩთ means "not attested", not a number.
                return <span>{specialLabel(cell.specialValue)}</span>;
            }
            if (value === null || value === undefined || value === "") {
                return "";
            }

            // Converted values arrive from the server already on the printed
            // scale and deliberately unrounded — the school asked for the
            // formula's output verbatim, so 9.5 shows as 9.5.
            if (converted && cell?.convertedValue != null) {
                return (
                    <span className="sgs-cell--converted">
                        {String(cell.convertedValue)}
                    </span>
                );
            }

            const text = Number.isNaN(Number(value))
                ? value
                : Number(value).toFixed(decimalsOf(column.code));

            return (
                <span title={conflict ? conflictTitle(conflict) : undefined}>
                    {text}
                    {cell?.override ? <sup className="sgs-override-mark">•</sup> : null}
                </span>
            );
        }
    }));

    return [studentColumn, ...gradeColumns];
};

const conflictTitle = (conflict) => {
    switch (conflict.reason) {
        case "PUBLISHED":
            return "გამოქვეყნებულია — შეცვლა ცვლილების მოთხოვნით ხდება";
        case "NOT_EDITABLE":
            return "სვეტი გამოითვლება და არ ექვემდებარება შესწორებას";
        default:
            return `სხვამ შეცვალა: ${conflict.current ?? "—"}`;
    }
};

/**
 * The header groups, e.g. the seven ongoing marks under "მიმდინარე შეფასება".
 * The student column is given its own empty group so the header rows line up.
 */
export const buildColumnGroupingModel = (columnGroups) => {
    if (!columnGroups?.length) return [];
    return [
        {
            groupId: "__student",
            headerName: "",
            children: [{field: "__student"}],
            align: "center",
            headerAlign: "center"
        },
        ...columnGroups.map(group => ({
            groupId: group.label,
            headerName: group.label,
            children: group.componentCodes.map(code => ({field: code})),
            align: "center",
            headerAlign: "center"
        }))
    ];
};

/** One row per student, values pulled from the cell map by hash rather than scanned. */
export const buildRows = ({students, columns, cells}) =>
    students.map(student => {
        const row = {
            id: student.enrollmentId,
            __index: student.index,
            __firstName: student.firstName,
            __lastName: student.lastName,
            __studentId: student.studentId
        };
        columns.forEach(column => {
            const cell = cells.get(cellKey(student.enrollmentId, column.code));
            row[column.code] = cell?.specialValue ?? cell?.value ?? "";
        });
        return row;
    });
