import React, {useState} from "react";
import {
    Button, Dialog, DialogActions, DialogContent, DialogTitle,
    TextField, Typography
} from "@mui/material";
import {raiseChangeRequest} from "./gradebookApi";

/**
 * Asking to change a grade parents have already seen.
 *
 * The only route into a published cell. The reason is required rather than
 * optional: the director is being asked to sign off on something, and an
 * unexplained request cannot be judged.
 */
const ChangeRequestModal = ({target, onClose, onRaised, onError, specialValues}) => {

    const [value, setValue] = useState("");
    const [reason, setReason] = useState("");
    const [saving, setSaving] = useState(false);

    const close = () => {
        setValue("");
        setReason("");
        onClose();
    };

    const submit = async () => {
        setSaving(true);
        try {
            const text = value.trim();
            const numeric = text !== "" && !Number.isNaN(Number(text));
            // Matched against the codes the journal declares, not upper-cased:
            // the uppercase of ჩთ is Mtavruli Georgian, not the code CHT.
            const special = (specialValues || []).find(sv =>
                sv.code.toLocaleLowerCase() === text.toLocaleLowerCase()
                || (sv.label || "").toLocaleLowerCase() === text.toLocaleLowerCase());
            await raiseChangeRequest({
                gradeEntryId: target.gradeEntryId,
                requestedValue: numeric ? Number(text) : null,
                requestedSpecialValue: numeric || text === "" ? null : (special?.code || text),
                reason: reason.trim()
            });
            onRaised();
            close();
        } catch (e) {
            onError(e);
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open={Boolean(target)} onClose={close} maxWidth="sm" fullWidth>
            <DialogTitle>ნიშნის ცვლილების მოთხოვნა</DialogTitle>
            <DialogContent>
                {target ? (
                    <>
                        <Typography variant="body2" style={{color: "#666", marginBottom: 4}}>
                            {`${target.lastName} ${target.firstName} — ${target.componentLabel}`}
                        </Typography>
                        <Typography variant="body2" style={{marginBottom: 20}}>
                            {`მიმდინარე (გამოქვეყნებული) მნიშვნელობა: ${
                                target.publishedValue ?? target.publishedSpecialValue ?? "—"}`}
                        </Typography>
                    </>
                ) : null}

                <TextField
                    fullWidth
                    autoFocus
                    label="ახალი მნიშვნელობა"
                    value={value}
                    onChange={(e) => setValue(e.target.value)}
                    style={{marginBottom: 20}}
                />
                <TextField
                    fullWidth
                    multiline
                    minRows={3}
                    label="ახსნა-განმარტება"
                    placeholder="რატომ საჭიროებს ეს ნიშანი შეცვლას?"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                />
                <Typography variant="caption" style={{color: "#8a6d3b"}}>
                    მოთხოვნას განიხილავს დირექცია. დამტკიცების შემდეგ ცვლილება
                    ავტომატურად აისახება მშობლის გვერდზე.
                </Typography>
            </DialogContent>
            <DialogActions>
                <Button onClick={close}>გაუქმება</Button>
                <Button
                    variant="contained"
                    disabled={saving || reason.trim() === "" || value.trim() === ""}
                    onClick={submit}
                >
                    გაგზავნა
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default ChangeRequestModal;
