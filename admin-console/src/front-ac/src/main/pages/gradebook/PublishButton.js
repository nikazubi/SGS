import React, {useState} from "react";
import {
    Button, Dialog, DialogActions, DialogContent, DialogTitle,
    FormControlLabel, Switch, Typography
} from "@mui/material";
import {Send} from "@mui/icons-material";
import {publish} from "./gradebookApi";

/**
 * Releases the journal to parents.
 *
 * The moment the working document becomes the one parents read. Behind a
 * confirmation because it is outward-facing and cannot be taken back — there is
 * no unpublish, since retracting something a parent has already seen is worse
 * than correcting it forward.
 *
 * @param locksOnPublish whether released cells also become read-only. True for
 *        grades, where changing one afterwards goes through the director. False
 *        for the absence register, whose hours accumulate through a month and
 *        are republished as they do. The dialog said the first unconditionally,
 *        so on the register it promised a freeze and an approval that neither
 *        happen — telling the coordinator the opposite of the truth.
 */
const PublishButton = ({
                           classGroup, period, subject, journalUuid, locksOnPublish = true,
                           disabled, onPublished, onError
                       }) => {

    const [open, setOpen] = useState(false);
    const [thisSubjectOnly, setThisSubjectOnly] = useState(false);
    const [busy, setBusy] = useState(false);

    const run = async () => {
        setBusy(true);
        try {
            const result = await publish({
                classGroupId: classGroup.id,
                periodId: period.id,
                subjectId: thisSubjectOnly && subject ? subject.id : null,
                // Only this journal: publication reaches sub-periods, so an
                // unscoped release would take every other journal with it.
                journalUuid
            });
            onPublished(result);
            setOpen(false);
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
                startIcon={<Send/>}
                disabled={disabled}
                onClick={() => setOpen(true)}
                style={{textTransform: "none"}}
            >
                გამოქვეყნება
            </Button>

            <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>
                    {locksOnPublish ? "ნიშნების გამოქვეყნება" : "გამოქვეყნება მშობლებისთვის"}
                </DialogTitle>
                <DialogContent>
                    <Typography variant="body2" style={{marginBottom: 16}}>
                        {`${classGroup?.name || ""} — ${period?.label || ""}`}
                    </Typography>

                    <Typography variant="body2" style={{marginBottom: 16}}>
                        {locksOnPublish
                            ? `გამოქვეყნების შემდეგ ნიშნები გახდება ხილული მშობლებისთვის და
                               დაიბლოკება რედაქტირებისთვის. ცვლილება მოითხოვს დირექციის
                               თანხმობას.`
                            : `გამოქვეყნების შემდეგ მონაცემები გახდება ხილული მშობლებისთვის.
                               რედაქტირება რჩება — ახალი ცვლილება მშობელს ხელახალი
                               გამოქვეყნებით მიუვა.`}
                    </Typography>

                    {/* Class-wide is the normal action; the filter is for when
                        one teacher is late and the rest should not wait. Hidden
                        entirely where there are no subjects to choose between -
                        the absence register is class-wide. */}
                    {subject ? <FormControlLabel
                        control={
                            <Switch
                                checked={thisSubjectOnly}
                                onChange={(e) => setThisSubjectOnly(e.target.checked)}
                            />
                        }
                        label={`მხოლოდ ამ საგნის (${subject.name})`}
                    /> : null}

                    <Typography variant="caption" style={{display: "block", color: "#666"}}>
                        {thisSubjectOnly
                            ? "გამოქვეყნდება მხოლოდ არჩეული საგნის ნიშნები."
                            : "გამოქვეყნდება კლასის ყველა საგნის ნიშნები ამ პერიოდისთვის."}
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpen(false)}>გაუქმება</Button>
                    <Button variant="contained" disabled={busy} onClick={run}>
                        გამოქვეყნება
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
};

export default PublishButton;
