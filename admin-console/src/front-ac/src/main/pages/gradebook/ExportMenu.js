import React, {useState} from "react";
import {
    Button, Checkbox, Divider, ListItemIcon, ListItemText, ListSubheader, Menu,
    MenuItem
} from "@mui/material";
import {FileDownload} from "@mui/icons-material";
import {exportBulkDetail, exportBulkMatrix, exportDetail, exportMatrix}
    from "./gradebookApi";

/**
 * Excel exports for what is currently on screen, and for every class at once.
 *
 * Two shapes rather than the four the old console offered, because those four
 * differed only in which column and which periods they printed — and three of
 * them were built around monthly and semester periods that no longer exist.
 *
 * The bulk entries take no class: the server scopes them to the caller's own
 * grants, so the same item gives a coordinator their class and a director the
 * school. They archive a whole trimester in one download rather than 47.
 */
const ExportMenu = ({
                        classGroup, subject, period, columns, disabled, onError,
                        journalUuid, convertible
                    }) => {

    const [anchor, setAnchor] = useState(null);
    const [busy, setBusy] = useState(false);

    /**
     * Print on the reported scale rather than the stored one.
     *
     * The school marks out of 7 and reports to the government out of 10, so
     * this is what an official spreadsheet needs. It is the legacy
     * isDecimalSystem checkbox, except the mapping is now configuration.
     */
    const [converted, setConverted] = useState(false);

    // The column a summary matrix is most likely to want: the last calculated
    // one the template defines for this period.
    const summaryColumn = (columns || []).filter(c => c.kind === "DERIVED").slice(-1)[0];

    const run = async (fn) => {
        setBusy(true);
        setAnchor(null);
        try {
            await fn();
        } catch (e) {
            onError(e);
        } finally {
            setBusy(false);
        }
    };

    return (
        <>
            <Button
                size="small"
                startIcon={<FileDownload/>}
                disabled={disabled || busy}
                onClick={(e) => setAnchor(e.currentTarget)}
                style={{textTransform: "none"}}
            >
                ექსპორტი
            </Button>

            <Menu anchorEl={anchor} open={Boolean(anchor)} onClose={() => setAnchor(null)}>
                {convertible ? (
                    <MenuItem onClick={() => setConverted(v => !v)} dense>
                        <ListItemIcon>
                            <Checkbox edge="start" size="small" checked={converted}
                                      disableRipple tabIndex={-1}/>
                        </ListItemIcon>
                        <ListItemText
                            primary="გადაყვანილი შკალით"
                            secondary="სახელმწიფოსთვის — ათქულიანი"
                        />
                    </MenuItem>
                ) : null}
                {convertible ? <Divider/> : null}

                <MenuItem
                    disabled={!subject}
                    onClick={() => run(() => exportDetail({
                        classGroupId: classGroup.id,
                        subjectId: subject.id,
                        periodId: period.id,
                        className: classGroup.name,
                        journalUuid,
                        converted
                    }))}
                >
                    <ListItemText
                        primary="ეს საგანი — სრული ჟურნალი"
                        secondary="ყველა სვეტი, როგორც ეკრანზეა"
                    />
                </MenuItem>

                <MenuItem
                    disabled={!summaryColumn}
                    onClick={() => run(() => exportMatrix({
                        classGroupId: classGroup.id,
                        periodId: period.id,
                        componentCode: summaryColumn.code,
                        splitByChildPeriod: false,
                        className: classGroup.name,
                        journalUuid,
                        converted
                    }))}
                >
                    <ListItemText
                        primary="ყველა საგანი — შემაჯამებელი"
                        secondary={summaryColumn
                            ? `${summaryColumn.label}, საგნების მიხედვით`
                            : ""}
                    />
                </MenuItem>

                <MenuItem
                    disabled={!summaryColumn}
                    onClick={() => run(() => exportMatrix({
                        classGroupId: classGroup.id,
                        periodId: period.id,
                        componentCode: summaryColumn.code,
                        // A column per child period, which is what the annual
                        // export was: trimester by trimester across the year.
                        splitByChildPeriod: true,
                        className: classGroup.name,
                        journalUuid,
                        converted
                    }))}
                >
                    <ListItemText
                        primary="ყველა საგანი — პერიოდების მიხედვით"
                        secondary="თითო სვეტი თითო ქვეპერიოდზე"
                    />
                </MenuItem>

                <Divider/>
                <ListSubheader style={{lineHeight: "32px", fontSize: 12}}>
                    ყველა კლასი — ZIP
                </ListSubheader>

                <MenuItem
                    onClick={() => run(() => exportBulkDetail({
                        periodId: period.id,
                        journalUuid,
                        label: period.label,
                        converted
                    }))}
                >
                    <ListItemText
                        primary="სრული ჟურნალი — ყველა კლასი და საგანი"
                        secondary="თითო ფაილი თითო საგანზე, კლასების საქაღალდეებში"
                    />
                </MenuItem>

                <MenuItem
                    disabled={!summaryColumn}
                    onClick={() => run(() => exportBulkMatrix({
                        periodId: period.id,
                        componentCode: summaryColumn.code,
                        splitByChildPeriod: false,
                        journalUuid,
                        label: period.label,
                        converted
                    }))}
                >
                    <ListItemText
                        primary="შემაჯამებელი — ყველა კლასი"
                        secondary={summaryColumn ? `${summaryColumn.label}, თითო ფაილი თითო კლასზე` : ""}
                    />
                </MenuItem>
            </Menu>
        </>
    );
};

export default ExportMenu;
