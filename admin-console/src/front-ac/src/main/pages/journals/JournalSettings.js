import React, {useEffect, useState} from "react";
import {
    Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle,
    FormControlLabel, MenuItem, TextField, Typography
} from "@mui/material";
import {updateJournal} from "./journalApi";

/**
 * Who sees a journal, and how it is drawn for them.
 *
 * Separate from the wizard because both are safe to change at any time: they
 * affect visibility and presentation, never what a stored cell means. Frequency
 * and shape are the ones that cannot move, and those stay fixed at creation.
 */

/** Charts the parent console has. Adding one is a file there plus a line here. */
const CHARTS = [
    {value: "", label: "დიაგრამის გარეშე"},
    {value: "GRADE_TREND", label: "შეფასებების დინამიკა (ხაზოვანი)"},
    {value: "ABSENCE_BARS", label: "პერიოდების მიხედვით (სვეტოვანი)"}
];

const JournalSettings = ({journal, onClose, onSaved, onError}) => {

    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [parentVisible, setParentVisible] = useState(false);
    const [chartKey, setChartKey] = useState("");
    const [busy, setBusy] = useState(false);

    // The dialog is always mounted, so initial state ran once with no journal.
    // A visible journal therefore opened showing an unticked box, and saving
    // silently un-published it.
    useEffect(() => {
        if (journal) {
            setName(journal.name || "");
            setDescription(journal.description || "");
            setParentVisible(Boolean(journal.parentVisible));
            setChartKey(journal.chartKey || "");
        }
    }, [journal]);

    const save = async () => {
        setBusy(true);
        try {
            await updateJournal({
                uuid: journal.uuid,
                draft: {
                    name: name.trim(),
                    description: description.trim() || null,
                    frequency: journal.frequency,
                    subjectScoped: journal.subjectScoped,
                    parentVisible,
                    chartKey: chartKey || null
                }
            });
            onSaved();
            onClose();
        } catch (e) {
            onError(e);
        } finally {
            setBusy(false);
        }
    };

    return (
        <Dialog open={Boolean(journal)} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>{journal?.name}</DialogTitle>
            <DialogContent>
                {/* The name is the menu label and the tab's heading. Renaming
                    is safe: a tab is keyed by the journal's uuid, so the label
                    changes and everything already entered stays where it is. */}
                <TextField
                    fullWidth margin="dense" label="დასახელება"
                    value={name} onChange={(e) => setName(e.target.value)}
                />
                <TextField
                    fullWidth margin="dense" label="აღწერა (არასავალდებულო)"
                    multiline minRows={2}
                    value={description} onChange={(e) => setDescription(e.target.value)}
                    style={{marginBottom: 12}}
                />
                <FormControlLabel
                    control={
                        <Checkbox checked={parentVisible}
                                  onChange={(e) => setParentVisible(e.target.checked)}/>
                    }
                    label="მშობლებისთვის ხილვადი"
                />
                <Typography variant="caption"
                            style={{display: "block", color: "#666", marginBottom: 20}}>
                    ჩართვის შემდეგ ჟურნალი გამოჩნდება მშობლის გვერდზე ცალკე ღილაკად.
                    მშობელი ხედავს მხოლოდ გამოქვეყნებულ შეფასებებს — მიმდინარე
                    სამუშაო მნიშვნელობებს არასოდეს.
                </Typography>

                <TextField
                    select fullWidth label="დიაგრამა მშობლის გვერდზე"
                    value={chartKey}
                    onChange={(e) => setChartKey(e.target.value)}
                    disabled={!parentVisible}
                >
                    {CHARTS.map(c =>
                        <MenuItem key={c.value} value={c.value}>{c.label}</MenuItem>)}
                </TextField>
                <Typography variant="caption" style={{color: "#666"}}>
                    დიაგრამის გარეშეც გვერდი სრულად მუშაობს.
                </Typography>

            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>გაუქმება</Button>
                <Button variant="contained" disabled={busy || !name.trim()} onClick={save}>შენახვა</Button>
            </DialogActions>
        </Dialog>
    );
};

export default JournalSettings;
