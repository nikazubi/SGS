import React, {useEffect, useState} from "react";
import {useMutation, useQuery, useQueryClient} from "react-query";
import {
    Button, Dialog, DialogActions, DialogContent, DialogTitle,
    TextField, Typography
} from "@mui/material";
import {fetchConversionFormula, previewFormula, saveConversionFormula} from "./conversionApi";

/**
 * The one formula marks are printed on.
 *
 * The school grades German-style out of 7 and is legally required to report to
 * the government out of 10 — so this is a compliance output, not a convenience.
 * Today that conversion is a hardcoded "+3" in two copy-pasted export methods.
 * They are moving to a 9-point scale and have not settled the mapping, which is
 * the whole reason it is configurable.
 *
 * Nothing stored ever changes. The formula applies when someone turns the grid
 * toggle on and when they tick the box on an export — nowhere else, and never
 * on the parent portal.
 */
const ConversionFormulaDialog = ({open, onClose, onError}) => {

    const queryClient = useQueryClient();
    const {data: formula} = useQuery("CONVERSION_FORMULA", fetchConversionFormula,
        {enabled: open});

    const [draft, setDraft] = useState({name: "", multiplier: 1, offsetValue: 0});
    const [probe, setProbe] = useState("7");

    useEffect(() => {
        if (formula) {
            setDraft({
                name: formula.name || "",
                multiplier: formula.multiplier ?? 1,
                offsetValue: formula.offsetValue ?? 0
            });
        }
    }, [formula]);

    const save = useMutation(saveConversionFormula, {
        onSuccess: () => {
            queryClient.invalidateQueries("CONVERSION_FORMULA");
            // The grid decides whether to offer the toggle from this.
            queryClient.invalidateQueries("GRADEBOOK_GRID");
            onClose();
        },
        onError
    });

    const set = (patch) => setDraft(d => ({...d, ...patch}));

    return (
        <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
            <DialogTitle>ბეჭდვის შკალა</DialogTitle>
            <DialogContent>
                <Typography variant="body2" style={{color: "#666", marginBottom: 20}}>
                    ნიშნები ინახება ისე, როგორც შეყვანილია. ეს ფორმულა მხოლოდ
                    ჩვენებას ცვლის — ჟურნალში გადამრთველით და Excel-ში
                    ექსპორტისას. მშობლის გვერდზე არ მოქმედებს.
                </Typography>

                <TextField
                    fullWidth size="small" label="დასახელება"
                    value={draft.name}
                    onChange={(e) => set({name: e.target.value})}
                    style={{marginBottom: 16}}
                />

                <div style={{display: "flex", gap: 12}}>
                    <TextField
                        type="number" size="small" label="გამრავლდეს"
                        value={draft.multiplier ?? ""}
                        onChange={(e) => set({multiplier: numberOrNull(e.target.value)})}
                    />
                    <TextField
                        type="number" size="small" label="დაემატოს"
                        value={draft.offsetValue ?? ""}
                        onChange={(e) => set({offsetValue: numberOrNull(e.target.value)})}
                    />
                </div>
                <Typography variant="caption" style={{color: "#666"}}>
                    7-ქულიანიდან 10-ქულიანზე: × 1, + 3
                </Typography>

                {/* Typed in, converted live. The mapping has changed repeatedly
                    and is about to change again, so seeing what it does to a
                    real mark before saving is the point of the screen. */}
                <div style={{display: "flex", gap: 12, alignItems: "center", marginTop: 20}}>
                    <TextField
                        type="number" size="small" label="შემოწმება"
                        style={{width: 120}}
                        value={probe}
                        onChange={(e) => setProbe(e.target.value)}
                    />
                    <Typography variant="body2" style={{color: "#5b7c8d"}}>
                        {probe === "" ? "" : `→ ${previewFormula(draft, probe) || "—"}`}
                    </Typography>
                </div>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>გაუქმება</Button>
                <Button
                    variant="contained"
                    disabled={save.isLoading || !draft.name.trim()}
                    onClick={() => save.mutate(draft)}
                >
                    შენახვა
                </Button>
            </DialogActions>
        </Dialog>
    );
};

const numberOrNull = (v) => v === "" || v === null ? null : Number(v);

export default ConversionFormulaDialog;
