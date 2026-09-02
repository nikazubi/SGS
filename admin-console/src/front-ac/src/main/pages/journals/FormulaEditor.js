import React from "react";
import {
    Autocomplete, Button, Checkbox, FormControlLabel, IconButton,
    MenuItem, TextField, Typography
} from "@mui/material";
import {Add, Delete} from "@mui/icons-material";

/**
 * How a calculated column is worked out.
 *
 * The common case is a row per term: *this column, that percentage*. The
 * shapes the school's own grids need — the average of a group, a roll-up from
 * child periods, the same column across every subject — sit behind an advanced
 * toggle rather than being hidden, because without them the school could not
 * reproduce the journal it already has.
 */

const RULE_TYPES = [
    {value: "WEIGHTED_SUM", label: "შეწონილი ჯამი (x% + y%)"},
    {value: "AVERAGE", label: "საშუალო"},
    {value: "SUM", label: "ჯამი"},
    {value: "MIN", label: "მინიმუმი"},
    {value: "MAX", label: "მაქსიმუმი"},
    {value: "FIRST_NON_NULL", label: "პირველი შევსებული"}
];

const NULL_POLICIES = [
    {value: "IGNORE", label: "გამოტოვება — ითვლება შევსებულებზე"},
    {value: "AS_ZERO", label: "ნულად ჩათვლა"},
    {value: "BLOCK", label: "არ გამოითვალოს, სანამ ყველა არ შეივსება"}
];

const SOURCE_KINDS = [
    {value: "COMPONENT", label: "ერთი სვეტი"},
    {value: "GROUP", label: "რამდენიმე სვეტი ერთად"},
    {value: "ALL_SUBJECTS", label: "იგივე სვეტი ყველა საგანში"}
];

const REDUCERS = [
    {value: "FIRST_NON_NULL", label: "პირველი შევსებული"},
    {value: "AVERAGE", label: "საშუალო"},
    {value: "SUM", label: "ჯამი"},
    {value: "MIN", label: "მინიმუმი"},
    {value: "MAX", label: "მაქსიმუმი"},
    {value: "LATEST", label: "ბოლო"},
    {value: "COUNT", label: "რაოდენობა"}
];

const emptyTerm = () => ({
    weight: 1,
    sourceKind: "COMPONENT",
    reduce: "FIRST_NON_NULL",
    periodRef: "SAME",
    periodId: null,
    label: "",
    sources: []
});

const FormulaEditor = ({rule, onChange, columns, pickable, periods, advanced}) => {

    const setRule = (patch) => onChange({...rule, ...patch});

    const setTerm = (index, patch) => {
        const terms = rule.terms.map((t, i) => i === index ? {...t, ...patch} : t);
        onChange({...rule, terms});
    };

    return (
        <div>
            <div style={{display: "flex", gap: 16, marginBottom: 16}}>
                <TextField
                    select fullWidth size="small" label="გამოთვლის ტიპი"
                    value={rule.type}
                    onChange={(e) => setRule({type: e.target.value})}
                >
                    {RULE_TYPES.map(o =>
                        <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                </TextField>

                <TextField
                    select fullWidth size="small" label="შეუვსებელი უჯრა"
                    value={rule.nullPolicy}
                    onChange={(e) => setRule({nullPolicy: e.target.value})}
                >
                    {NULL_POLICIES.map(o =>
                        <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                </TextField>

                <TextField
                    type="number" size="small" label="ათწილადი" style={{width: 120}}
                    value={rule.decimals}
                    onChange={(e) => setRule({decimals: Number(e.target.value)})}
                />
            </div>

            <FormControlLabel
                control={
                    <Checkbox
                        checked={rule.renormalizeWeights}
                        onChange={(e) => setRule({renormalizeWeights: e.target.checked})}
                    />
                }
                label="წონების გადანაწილება, თუ რომელიმე შეფასება არ არის შეყვანილი"
            />
            <Typography variant="caption" style={{
                display: "block", color: "#666",
                marginBottom: 16
            }}>
                ჩართულია: მოსწავლე, რომელსაც ფინალური ტესტი არ ჩაუბარებია, შეფასდება
                იმაზე, რაც ჩააბარა — და არა შკალის 70%-ზე.
            </Typography>

            {(rule.terms || []).map((term, index) => (
                <div key={index} style={{
                    border: "1px solid #dde6ea", borderRadius: 4,
                    padding: 12, marginBottom: 12
                }}>
                    <div style={{display: "flex", gap: 12, alignItems: "center"}}>
                        <TextField
                            type="number" size="small" label="წონა %" style={{width: 110}}
                            value={Math.round((term.weight ?? 0) * 100)}
                            onChange={(e) =>
                                setTerm(index, {weight: Number(e.target.value) / 100})}
                        />

                        <Autocomplete
                            multiple={term.sourceKind !== "COMPONENT"}
                            style={{flex: 1}}
                            size="small"
                            options={pickable || []}
                            groupBy={(o) => o.journalName}
                            getOptionLabel={(o) => o.componentLabel}
                            isOptionEqualToValue={(o, v) =>
                                o.componentCode === v.componentCode
                                && o.journalUuid === v.journalUuid}
                            value={resolveValue(term, pickable, columns)}
                            onChange={(e, value) =>
                                setTerm(index, {sources: toSources(value)})}
                            renderInput={(params) =>
                                <TextField {...params} label="სვეტი"/>}
                        />

                        <IconButton
                            size="small"
                            onClick={() => onChange({
                                ...rule,
                                terms: rule.terms.filter((t, i) => i !== index)
                            })}
                        >
                            <Delete fontSize="small"/>
                        </IconButton>
                    </div>

                    {advanced ? (
                        <div style={{display: "flex", gap: 12, marginTop: 12}}>
                            <TextField
                                select size="small" label="წყარო" style={{width: 220}}
                                value={term.sourceKind}
                                onChange={(e) => setTerm(index, {sourceKind: e.target.value})}
                            >
                                {SOURCE_KINDS.map(o =>
                                    <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                            </TextField>

                            <TextField
                                select size="small" label="შეკრება" style={{width: 180}}
                                value={term.reduce}
                                onChange={(e) => setTerm(index, {reduce: e.target.value})}
                            >
                                {REDUCERS.map(o =>
                                    <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                            </TextField>

                            {/* Only asked when the two journals are filled in at
                                different frequencies. Same frequency means the
                                same occurrence, with nothing to infer. */}
                            <TextField
                                select size="small" label="პერიოდი" style={{width: 220}}
                                value={term.periodRef}
                                onChange={(e) => setTerm(index, {
                                    periodRef: e.target.value,
                                    periodId: e.target.value === "SPECIFIC"
                                        ? term.periodId : null
                                })}
                            >
                                <MenuItem value="SAME">იგივე პერიოდი</MenuItem>
                                <MenuItem value="CHILDREN">ქვეპერიოდები</MenuItem>
                                <MenuItem value="SPECIFIC">კონკრეტული პერიოდი</MenuItem>
                            </TextField>

                            {term.periodRef === "SPECIFIC" ? (
                                <TextField
                                    select size="small" label="რომელი" style={{width: 180}}
                                    value={term.periodId || ""}
                                    onChange={(e) =>
                                        setTerm(index, {periodId: Number(e.target.value)})}
                                >
                                    {(periods || []).map(p =>
                                        <MenuItem key={p.id} value={p.id}>{p.label}</MenuItem>)}
                                </TextField>
                            ) : null}
                        </div>
                    ) : null}

                    {needsPeriodChoice(term, pickable) ? (
                        <Typography variant="caption"
                                    style={{color: "#8a6d3b", display: "block", marginTop: 8}}>
                            ეს სვეტი სხვა სიხშირის ჟურნალშია — მიუთითეთ, რომელი პერიოდი
                            იგულისხმება.
                        </Typography>
                    ) : null}
                </div>
            ))}

            <Button
                size="small" startIcon={<Add/>}
                onClick={() => onChange({...rule, terms: [...(rule.terms || []), emptyTerm()]})}
                style={{textTransform: "none"}}
            >
                სვეტის დამატება
            </Button>
        </div>
    );
};

const resolveValue = (term, pickable, columns) => {
    const list = (term.sources || []).map(s =>
        (pickable || []).find(p =>
            p.componentCode === s.componentCode
            && (p.journalUuid === s.journalUuid
                || (!s.journalUuid && (columns || []).some(c => c.code === s.componentCode))))
        || {
            journalUuid: s.journalUuid, journalName: "", componentCode: s.componentCode,
            componentLabel: s.componentCode
        });
    return term.sourceKind === "COMPONENT" ? (list[0] || null) : list;
};

const toSources = (value) => {
    const list = Array.isArray(value) ? value : (value ? [value] : []);
    return list.map(v => ({journalUuid: v.journalUuid, componentCode: v.componentCode}));
};

/** A reference into a journal filled in at a different frequency must say which one. */
const needsPeriodChoice = (term, pickable) =>
    term.periodRef === "SAME"
    && (term.sources || []).some(s => {
        const ref = (pickable || []).find(p =>
            p.componentCode === s.componentCode && p.journalUuid === s.journalUuid);
        return ref && s.journalUuid && !ref.sameFrequencyAsCaller;
    });

export default FormulaEditor;
