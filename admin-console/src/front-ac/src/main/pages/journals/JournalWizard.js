import React, {useState} from "react";
import {
    Button, Dialog, DialogActions, DialogContent, DialogTitle,
    FormControlLabel, MenuItem, Radio, RadioGroup, TextField, Typography
} from "@mui/material";
import {createJournal} from "./journalApi";

/**
 * Creating a journal.
 *
 * Three questions, in this order on purpose: the name becomes the menu label,
 * and frequency and shape both change what a column *means*. Asking them last
 * would let someone build twelve columns before discovering the grid is the
 * wrong shape — and neither can be changed afterwards, because there is no
 * sensible reinterpretation of a trimester mark as a weekly one.
 */

const FREQUENCIES = [
    {
        value: "ONCE_A_YEAR", label: "წელიწადში ერთხელ",
        hint: "ერთი ცხრილი. პერიოდის არჩევა საერთოდ არ ჩნდება."
    },
    {
        value: "TRIMESTER", label: "ტრიმესტრში ერთხელ",
        hint: "იგივე სვეტები მეორდება თითო ტრიმესტრზე."
    },
    {
        value: "MONTH", label: "თვეში ერთხელ",
        hint: "იგივე სვეტები მეორდება თითო თვეზე."
    }
    // No DAY. JournalFrequency still has it and the engine still reaches that
    // far, but db/028 deleted every depth-3 period when daily absence moved to
    // its own table - so a journal created at that frequency would have no
    // periods at all: the picker stays empty and the grid never loads, with
    // nothing to say why. Offer it again if a scheme ever seeds days.
];

const JournalWizard = ({open, onClose, onCreated, onError, periodSchemeId}) => {

    const [step, setStep] = useState(0);
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [frequency, setFrequency] = useState("ONCE_A_YEAR");
    const [subjectScoped, setSubjectScoped] = useState("true");
    const [busy, setBusy] = useState(false);

    const close = () => {
        setStep(0);
        setName("");
        setDescription("");
        setFrequency("ONCE_A_YEAR");
        setSubjectScoped("true");
        onClose();
    };

    const submit = async () => {
        setBusy(true);
        try {
            const created = await createJournal({
                draft: {
                    name: name.trim(),
                    description: description.trim(),
                    frequency,
                    subjectScoped: subjectScoped === "true"
                },
                periodSchemeId
            });
            onCreated(created);
            close();
        } catch (e) {
            onError(e);
        } finally {
            setBusy(false);
        }
    };

    return (
        <Dialog open={open} onClose={close} maxWidth="sm" fullWidth>
            <DialogTitle>ახალი ჟურნალი</DialogTitle>
            <DialogContent>

                {step === 0 ? (
                    <>
                        <TextField
                            fullWidth autoFocus label="დასახელება"
                            placeholder="მაგ. ეთიკური ნორმა"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            style={{marginTop: 8, marginBottom: 8}}
                        />
                        <Typography variant="caption" style={{color: "#666"}}>
                            ეს დასახელება გამოჩნდება მენიუში და ჩანართზე. მისი შეცვლა
                            მოგვიანებით თავისუფლად შეიძლება.
                        </Typography>
                        <TextField
                            fullWidth multiline minRows={2} label="აღწერა (არასავალდებულო)"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            style={{marginTop: 20}}
                        />
                    </>
                ) : null}

                {step === 1 ? (
                    <>
                        <Typography variant="subtitle2" style={{marginBottom: 8}}>
                            რამდენად ხშირად ივსება?
                        </Typography>
                        <TextField
                            select fullWidth value={frequency}
                            onChange={(e) => setFrequency(e.target.value)}
                        >
                            {FREQUENCIES.map(f =>
                                <MenuItem key={f.value} value={f.value}>{f.label}</MenuItem>)}
                        </TextField>
                        <Typography variant="caption"
                                    style={{display: "block", color: "#666", marginTop: 8}}>
                            {FREQUENCIES.find(f => f.value === frequency)?.hint}
                        </Typography>
                        <Typography variant="caption"
                                    style={{display: "block", color: "#8a6d3b", marginTop: 16}}>
                            შემდეგ ამის შეცვლა ვეღარ მოხერხდება — უკვე შეყვანილი
                            შეფასებების სხვა პერიოდზე გადატანა შეუძლებელია.
                        </Typography>
                    </>
                ) : null}

                {step === 2 ? (
                    <>
                        <Typography variant="subtitle2" style={{marginBottom: 8}}>
                            როგორ იყოფა?
                        </Typography>
                        <RadioGroup value={subjectScoped}
                                    onChange={(e) => setSubjectScoped(e.target.value)}>
                            <FormControlLabel
                                value="true" control={<Radio/>}
                                label="თითო საგანზე თითო ცხრილი"
                            />
                            <Typography variant="caption"
                                        style={{color: "#666", marginLeft: 32, marginBottom: 8}}>
                                აკადემიური ნიშნებივით — მასწავლებელი ირჩევს კლასს და საგანს.
                            </Typography>
                            <FormControlLabel
                                value="false" control={<Radio/>}
                                label="ერთი ცხრილი მთელ კლასზე"
                            />
                            <Typography variant="caption"
                                        style={{color: "#666", marginLeft: 32}}>
                                ეთიკური ნორმისა და გაცდენებივით — საგანი არ ერევა.
                            </Typography>
                        </RadioGroup>
                    </>
                ) : null}
            </DialogContent>

            <DialogActions>
                <Button onClick={close}>გაუქმება</Button>
                {step > 0 ? <Button onClick={() => setStep(step - 1)}>უკან</Button> : null}
                {step < 2 ? (
                    <Button
                        variant="contained"
                        disabled={step === 0 && name.trim() === ""}
                        onClick={() => setStep(step + 1)}
                    >
                        შემდეგ
                    </Button>
                ) : (
                    <Button variant="contained" disabled={busy} onClick={submit}>
                        შექმნა
                    </Button>
                )}
            </DialogActions>
        </Dialog>
    );
};

export default JournalWizard;
