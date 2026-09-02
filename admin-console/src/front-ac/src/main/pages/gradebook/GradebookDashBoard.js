import React, {useCallback, useEffect, useMemo, useState} from "react";
import {Menu, MenuItem} from "@mui/material";
import DataGridSGS from "../../components/grid/DataGrid";
import DataGridPaper from "../../components/grid/DataGridPaper";
import {getFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import {useUserContext} from "../../../contexts/user-context";
import GradebookToolbar from "./GradebookToolbar";
import ExplainPanel from "./ExplainPanel";
import ChangeRequestModal from "./ChangeRequestModal";
import useGradeSheet, {cellKey} from "./useGradeSheet";
import {buildColumnGroupingModel, buildColumns, buildRows} from "./gridColumns";
import {gradebookGridSx} from "./gradebookStyles";

/**
 * The grade entry screen.
 *
 * Nothing here knows what a trimester is, or how many ongoing marks there are,
 * or how the trimester assessment is calculated. The columns, their grouping,
 * their scales and which of them are editable all come from the template. The
 * page it replaces declared eleven columns in JSX and posted one request per
 * cell, then reloaded the whole grid afterwards.
 */
const GradebookDashBoard = ({journalUuid, journalName}) => {

    // Filters are remembered per journal: a class and period chosen in one
    // grid mean nothing in another.
    const pageId = journalUuid ? `GRADEBOOK_${journalUuid}` : "GRADEBOOK";
    const [filters, setFilters] = useState({...getFiltersOfPage(pageId)});
    const {setErrorMessage, setNotification} = useNotification();
    const {hasPermission} = useUserContext();

    const classGroupId = filters?.classGroup?.id;
    const subjectId = filters?.subject?.id;
    const periodId = filters?.period?.id;

    const {
        grid, isLoading, isError, error, refetch, specialValues,
        cells, conflicts, stale,
        status, pending,
        editCell, revertToCalculated, flushNow
    } = useGradeSheet({classGroupId, subjectId, periodId, journalUuid});

    /**
     * Showing marks on the printed scale rather than the stored one.
     *
     * A view mode, not a setting: it changes nothing and is not remembered
     * across journals. While it is on the grid is read-only, because a banded
     * scale cannot be reversed - see gridColumns.
     */
    const [converted, setConverted] = useState(false);

    const [explainTarget, setExplainTarget] = useState(null);
    const [changeRequestTarget, setChangeRequestTarget] = useState(null);
    const [contextMenu, setContextMenu] = useState(null);

    useEffect(() => {
        if (isError && error) setErrorMessage(error);
    }, [isError, error, setErrorMessage]);

    const getCell = useCallback(
        (enrollmentId, code) => cells.get(cellKey(enrollmentId, code)), [cells]);

    const getConflict = useCallback(
        (enrollmentId, code) => conflicts.get(cellKey(enrollmentId, code)), [conflicts]);

    const isStale = useCallback((key) => stale.has(key), [stale]);

    const decimalsOf = useCallback((code) => {
        const column = (grid?.columns || []).find(c => c.code === code);
        return column ? column.decimals : 2;
    }, [grid]);

    const specialLabel = useCallback((code) =>
        specialValues.find(sv => sv.code === code)?.label || code, [specialValues]);

    // Only offered where the journal or one of its columns actually names a
    // scale. Absence and ethics never do, so the toggle simply is not there.
    const convertible = useMemo(
        () => (grid?.columns || []).some(c => c.hasConversion), [grid]);

    useEffect(() => {
        if (!convertible && converted) setConverted(false);
    }, [convertible, converted]);

    const columns = useMemo(() => grid ? buildColumns({
        columns: grid.columns, getCell, isStale, getConflict, decimalsOf, specialLabel,
        converted: converted && convertible,
        canEdit: grid.capabilities?.canEdit !== false
    }) : [], [grid, getCell, isStale, getConflict, decimalsOf, specialLabel,
        converted, convertible]);

    const columnGroupingModel = useMemo(
        () => buildColumnGroupingModel(grid?.columnGroups), [grid]);

    const rows = useMemo(() => grid
        ? buildRows({students: grid.students, columns: grid.columns, cells})
        : [], [grid, cells]);

    /**
     * Local and synchronous. The flush is scheduled separately, which is the
     * whole point: awaiting the network here is what made the old grid slow.
     */
    const processRowUpdate = useCallback((newRow, oldRow) => {
        const changed = Object.keys(newRow).find(
            key => !key.startsWith("__") && key !== "id" && newRow[key] !== oldRow[key]);
        if (changed) editCell(newRow.id, changed, newRow[changed]);
        return newRow;
    }, [editCell]);

    const onProcessRowUpdateError = useCallback((e) => setErrorMessage(e), [setErrorMessage]);

    const openContextMenu = useCallback((event) => {
        const cellEl = event.target.closest?.("[data-field]");
        const rowEl = cellEl?.closest("[data-id]");
        if (!cellEl || !rowEl) return;

        const field = cellEl.getAttribute("data-field");
        const column = (grid?.columns || []).find(c => c.code === field);
        if (!column) return;

        const enrollmentId = Number(rowEl.getAttribute("data-id"));
        const cell = getCell(enrollmentId, field);

        // A published cell can be disputed; a calculated one can be explained
        // or reverted. An ordinary empty input column has nothing to offer.
        const derived = column.kind === "DERIVED";
        if (!derived && !cell?.published) return;

        event.preventDefault();
        const student = grid.students.find(s => s.enrollmentId === enrollmentId);

        setContextMenu({
            mouseX: event.clientX + 2,
            mouseY: event.clientY - 6,
            enrollmentId,
            componentCode: field,
            componentLabel: column.label,
            derived,
            overridden: Boolean(cell?.override),
            published: Boolean(cell?.published),
            changeRequestPending: Boolean(cell?.changeRequestPending),
            gradeEntryId: cell?.id,
            // What parents were shown - not the working value, which is
            // exactly the number the request is asking to move away from.
            publishedValue: cell?.publishedValue ?? cell?.publishedSpecialValue,
            firstName: student?.firstName,
            lastName: student?.lastName
        });
    }, [grid, getCell]);

    const closeContextMenu = () => setContextMenu(null);

    return (
        <div>
            <GradebookToolbar
                filters={filters}
                setFilters={setFilters}
                pageId={pageId}
                journalUuid={journalUuid}
                status={status}
                pending={pending}
                onFlush={flushNow}
                columns={grid?.columns}
                convertible={convertible}
                converted={converted}
                onConvertedChange={setConverted}
                // MANAGE_CLOSED_PERIOD alone, which is all POST /publish
                // requires. Also gating on canEdit - the server's ADD_GRADES
                // answer - hid the button from a director who may release but
                // not enter marks, which is the likeliest publisher there is.
                canPublish={hasPermission("MANAGE_CLOSED_PERIOD")}
                // The server works this out and says why: the export endpoints
                // are all MANAGE_GRADES, so offering the menu to a teacher who
                // only has ADD_GRADES is a button that always 403s. The field
                // was computed, sent, and read by nothing.
                canExport={grid?.capabilities?.canExport !== false}
                onPublished={(result) => {
                    setNotification({
                        message: `გამოქვეყნდა ${result.released} შეფასება`,
                        severity: "success"
                    });
                    refetch();
                }}
                onError={setErrorMessage}
            />

            <div style={{
                height: "calc(100vh - 130px)", width: "98%",
                marginLeft: 15, marginRight: 15
            }}
                 onContextMenu={openContextMenu}>
                <DataGridPaper>
                    {grid ? (
                        <div style={{
                            textAlign: "center", marginTop: 10, marginBottom: 10,
                            width: "100%", backgroundColor: "white"
                        }}>
                            <div style={{fontSize: 20, fontWeight: "bold"}}>
                                {[journalName, filters?.subject?.name, grid.period.label]
                                    .filter(Boolean).join(" — ")}
                            </div>
                            {/* Who teaches it to this class, as the old header
                                showed - occasionally two names, for a subject
                                that is co-taught. */}
                            {filters?.subject?.teacherName ? (
                                <div style={{fontSize: 14, color: "#5b7c8d", marginTop: 2}}>
                                    {`პედაგოგი: ${filters.subject.teacherName}`}
                                </div>
                            ) : null}
                        </div>
                    ) : null}

                    <DataGridSGS
                        queryKey="GRADEBOOK_GRID"
                        rows={rows}
                        columns={columns}
                        loading={isLoading}
                        experimentalFeatures={{columnGrouping: true}}
                        columnGroupingModel={columnGroupingModel}
                        editMode="cell"
                        processRowUpdate={processRowUpdate}
                        onProcessRowUpdateError={onProcessRowUpdateError}
                        getRowHeight={() => 46}
                        disableColumnMenu
                        disableSelectionOnClick
                        fullyHideFooter
                        sx={gradebookGridSx}
                    />
                </DataGridPaper>
            </div>

            <Menu
                open={Boolean(contextMenu)}
                onClose={closeContextMenu}
                anchorReference="anchorPosition"
                anchorPosition={contextMenu
                    ? {top: contextMenu.mouseY, left: contextMenu.mouseX}
                    : undefined}
            >
                {contextMenu?.published ? (
                    <MenuItem
                        disabled={contextMenu?.changeRequestPending}
                        onClick={() => {
                            setChangeRequestTarget({...contextMenu});
                            closeContextMenu();
                        }}
                    >
                        {contextMenu?.changeRequestPending
                            ? "მოთხოვნა უკვე გაგზავნილია"
                            : "ცვლილების მოთხოვნა"}
                    </MenuItem>
                ) : null}

                {contextMenu?.derived ? (
                    <MenuItem
                        disabled={!contextMenu?.overridden}
                        onClick={() => {
                            revertToCalculated(contextMenu.enrollmentId,
                                contextMenu.componentCode);
                            closeContextMenu();
                        }}
                    >
                        გამოთვლილ მნიშვნელობაზე დაბრუნება
                    </MenuItem>
                ) : null}
                {contextMenu?.derived ? (
                    <MenuItem
                        onClick={() => {
                            setExplainTarget({
                                enrollmentId: contextMenu.enrollmentId,
                                componentCode: contextMenu.componentCode,
                                firstName: contextMenu.firstName,
                                lastName: contextMenu.lastName
                            });
                            closeContextMenu();
                        }}
                    >
                        როგორ გამოითვალა?
                    </MenuItem>
                ) : null}
            </Menu>

            <ChangeRequestModal
                target={changeRequestTarget}
                specialValues={specialValues}
                onClose={() => setChangeRequestTarget(null)}
                onRaised={() => {
                    setNotification({
                        message: "მოთხოვნა გაიგზავნა დირექციასთან",
                        severity: "success"
                    });
                    refetch();
                }}
                onError={setErrorMessage}
            />

            <ExplainPanel
                target={explainTarget}
                subjectId={subjectId}
                periodId={periodId}
                journalUuid={journalUuid}
                onClose={() => setExplainTarget(null)}
            />
        </div>
    );
};

export default GradebookDashBoard;
