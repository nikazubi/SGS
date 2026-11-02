import React, {useMemo, useState} from "react";
import {useQuery} from "react-query";
import {
    Button,
    Checkbox,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControlLabel,
    MenuItem,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    TextField,
    Typography
} from "@mui/material";
import {bulkPublish, fetchBulkPublishOptions, previewBulkPublish} from "../gradebook/gradebookApi";
import ClassPicker from "../../components/ClassPicker";

/**
 * Releasing several classes and journals at once.
 *
 * The director used to go journal by journal and class by class - one
 * publish-and-confirm per pair, each emailing that class's families. Here they
 * pick a period, the journals and the classes, see what each pair would release,
 * and release them together.
 *
 * Three steps, never skipping the middle one: nothing reaches a parent until the
 * director has seen the counts. A pair that fails does not undo the others, and
 * the result offers to retry only those.
 */
const BulkPublishDialog = ({open, onClose, onPublished, onError}) => {

    const [step, setStep] = useState("choose");
    const [periodId, setPeriodId] = useState("");
    const [journalUuids, setJournalUuids] = useState(null);
    const [classIds, setClassIds] = useState([]);
    const [items, setItems] = useState([]);
    const [busy, setBusy] = useState(false);

    const {data: options} = useQuery(["BULK_PUBLISH_OPTIONS"], fetchBulkPublishOptions,
        {enabled: open, refetchOnWindowFocus: false, onError});

    const journals = useMemo(() => options?.journals || [], [options]);
    const classes = useMemo(() => options?.classes || [], [options]);
    // Tree order - each trimester followed by its own months - rather than the
    // server's level order, so the indentation reads as the nesting the
    // release follows.
    const periods = useMemo(() => {
        const all = options?.periods || [];
        const byOrdinal = (a, b) => a.ordinal - b.ordinal;
        const walk = (parentId) => all.filter(p => (p.parentId ?? null) === parentId)
            .sort(byOrdinal)
            .flatMap(p => [p, ...walk(p.id)]);
        return walk(null);
    }, [options]);
    // Every journal until the director unticks one.
    const chosenJournals = journalUuids ?? journals.map(j => j.uuid);

    const targetsFor = (classGroupIds, uuids) => classGroupIds.flatMap(classGroupId =>
        uuids.map(journalUuid => ({classGroupId, journalUuid})));

    const reset = () => {
        setStep("choose");
        setItems([]);
    };

    const close = () => {
        if (busy) return;
        if (step === "result") onPublished();
        reset();
        onClose();
    };

    const run = async (request, next) => {
        setBusy(true);
        try {
            next(await request());
        } catch (e) {
            onError(e);
        } finally {
            setBusy(false);
        }
    };

    const preview = () => run(
        () => previewBulkPublish({periodId, targets: targetsFor(classIds, chosenJournals)}),
        result => {
            setItems(result);
            setStep("preview");
        });

    const publish = () => run(
        () => bulkPublish({periodId, targets: targetsFor(classIds, chosenJournals)}),
        result => {
            setItems(result);
            setStep("result");
        });

    // Only the pairs that failed; everything that went stays as it went.
    const retryFailed = () => {
        const failed = items.filter(i => i.status === "FAILED")
            .map(i => ({classGroupId: i.classGroupId, journalUuid: i.journalUuid}));
        return run(() => bulkPublish({periodId, targets: failed}), retried => {
            const again = new Map(retried.map(i => [`${i.classGroupId}:${i.journalUuid}`, i]));
            setItems(items.map(i => again.get(`${i.classGroupId}:${i.journalUuid}`) || i));
        });
    };

    const toggleJournal = (uuid) => setJournalUuids(chosenJournals.includes(uuid)
        ? chosenJournals.filter(u => u !== uuid) : [...chosenJournals, uuid]);

    const total = (status) => items.filter(i => i.status === status)
        .reduce((sum, i) => sum + i.count, 0);
    const failedCount = items.filter(i => i.status === "FAILED").length;
    // Grades email on release; the absence register does not. Said before the
    // director presses publish, because it is the part that leaves the school.
    const emailsFamilies = items.some(i => i.status === "READY"
        && journals.find(j => j.uuid === i.journalUuid)?.locksOnPublish);

    const period = periods.find(p => p.id === periodId);

    return (
        <Dialog open={open} onClose={close} maxWidth="md" fullWidth>
            <DialogTitle>
                ნიშნების გამოქვეყნება
                {step !== "choose" && period ? ` — ${period.label}` : ""}
            </DialogTitle>

            <DialogContent dividers>
                {step === "choose" ? (
                    <>
                        <TextField select fullWidth size="small" label="პერიოდი"
                                   value={periodId}
                                   onChange={(e) => setPeriodId(e.target.value)}
                                   helperText="გამოქვეყნდება არჩეული პერიოდი და ყველა პერიოდი მის შიგნით">
                            {periods.map(p => (
                                <MenuItem key={p.id} value={p.id}>
                                    {/* The year, its trimesters, their months - indented
                                        so the nesting the release follows is visible. */}
                                    {" ".repeat(p.depth) + p.label}
                                </MenuItem>
                            ))}
                        </TextField>

                        <Typography variant="subtitle2" style={{marginTop: 20}}>ჟურნალები</Typography>
                        {journals.map(j => (
                            <FormControlLabel key={j.uuid} label={j.name}
                                              control={<Checkbox size="small"
                                                                 checked={chosenJournals.includes(j.uuid)}
                                                                 onChange={() => toggleJournal(j.uuid)}/>}/>
                        ))}

                        <Typography variant="subtitle2" style={{marginTop: 20}}>
                            კლასები
                            <Button size="small" style={{marginLeft: 12, textTransform: "none"}}
                                    onClick={() => setClassIds(classIds.length === classes.length
                                        ? [] : classes.map(c => c.id))}>
                                {classIds.length === classes.length ? "მონიშვნის მოხსნა" : "ყველას მონიშვნა"}
                            </Button>
                        </Typography>
                        <ClassPicker classes={classes} value={classIds} onChange={setClassIds}/>
                    </>
                ) : (
                    <PairTable items={items} classes={classes} journals={journals}
                               chosenJournals={chosenJournals} classIds={classIds} step={step}/>
                )}

                {step === "preview" ? (
                    <Typography variant="body2" style={{marginTop: 16}}>
                        {total("READY") > 0
                            ? `გამოქვეყნდება ${total("READY")} შეფასება.`
                            + (emailsFamilies ? " ყოველი ოჯახი მიიღებს ერთ შეტყობინებას." : "")
                            : "გამოსაქვეყნებელი არაფერია — ყველაფერი უკვე გამოქვეყნებულია ან ცარიელია."}
                    </Typography>
                ) : null}

                {step === "result" ? (
                    <Typography variant="body2" style={{marginTop: 16}}>
                        {`გამოქვეყნდა ${total("PUBLISHED")} შეფასება.`}
                        {failedCount > 0 ? ` ვერ გამოქვეყნდა: ${failedCount}.` : ""}
                    </Typography>
                ) : null}
            </DialogContent>

            <DialogActions>
                {step === "choose" ? (
                    <>
                        <Button onClick={close}>გაუქმება</Button>
                        <Button variant="contained" onClick={preview}
                                disabled={busy || !periodId || classIds.length === 0
                                    || chosenJournals.length === 0}>
                            წინასწარი ნახვა
                        </Button>
                    </>
                ) : null}
                {step === "preview" ? (
                    <>
                        <Button onClick={() => setStep("choose")} disabled={busy}>უკან</Button>
                        <Button variant="contained" onClick={publish}
                                disabled={busy || total("READY") === 0}>
                            {`გამოქვეყნება (${total("READY")})`}
                        </Button>
                    </>
                ) : null}
                {step === "result" ? (
                    <>
                        {failedCount > 0 ? (
                            <Button onClick={retryFailed} disabled={busy}>
                                {`ხელახლა ცდა (${failedCount})`}
                            </Button>
                        ) : null}
                        <Button variant="contained" onClick={close} disabled={busy}>დახურვა</Button>
                    </>
                ) : null}
            </DialogActions>
        </Dialog>
    );
};

/**
 * Classes down, journals across - the shape the director chose them in.
 */
const PairTable = ({items, classes, journals, chosenJournals, classIds, step}) => {
    const byPair = new Map(items.map(i => [`${i.classGroupId}:${i.journalUuid}`, i]));
    const shownJournals = journals.filter(j => chosenJournals.includes(j.uuid));
    const shownClasses = classes.filter(c => classIds.includes(c.id));

    return (
        <Table size="small">
            <TableHead>
                <TableRow>
                    <TableCell>კლასი</TableCell>
                    {shownJournals.map(j => <TableCell key={j.uuid} align="center">{j.name}</TableCell>)}
                </TableRow>
            </TableHead>
            <TableBody>
                {shownClasses.map(c => (
                    <TableRow key={c.id}>
                        <TableCell>{c.name}</TableCell>
                        {shownJournals.map(j => (
                            <TableCell key={j.uuid} align="center">
                                <PairCell item={byPair.get(`${c.id}:${j.uuid}`)} step={step}/>
                            </TableCell>
                        ))}
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    );
};

const PairCell = ({item, step}) => {
    if (!item) return "—";
    switch (item.status) {
        case "READY":
            return <b>{item.count}</b>;
        case "PUBLISHED":
            return <span style={{color: "#2e7d32", fontWeight: 600}}>{`✓ ${item.count}`}</span>;
        case "FAILED":
            return <span style={{color: "#c0392b"}}>{`✗ ${item.error || "ვერ გამოქვეყნდა"}`}</span>;
        default:
            // Nothing new since the last release - in the result as in the preview.
            return <span style={{color: "#8a99a3"}}>{step === "result" ? "უცვლელი" : "—"}</span>;
    }
};

export default BulkPublishDialog;
