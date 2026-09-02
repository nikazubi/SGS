import React, {useState} from "react";
import {
    Accordion, AccordionDetails, AccordionSummary, Button, Checkbox, Chip,
    FormControlLabel, IconButton, MenuItem, TextField, Typography
} from "@mui/material";
import {Add, ArrowDownward, ArrowUpward, Delete, ExpandMore, Functions} from "@mui/icons-material";
import FormulaEditor from "./FormulaEditor";

/**
 * The columns of a journal.
 *
 * One row per column, in the order they appear on the grid. Everything the
 * engine needs to render and validate a cell is here — the page this configures
 * used to declare eleven columns in JSX, so adding one meant a deployment.
 */

const emptyRule = () => ({
    type: "WEIGHTED_SUM",
    nullPolicy: "IGNORE",
    renormalizeWeights: true,
    roundingMode: "HALF_UP",
    decimals: 0,
    terms: []
});

/**
 * Which tier a column sits on, from how often the journal is filled in.
 *
 * There is no control for this in the editor, so the default is the answer.
 * A column on a tier the journal has no periods of is invisible: the grid asks
 * for a period and finds nothing defined for it.
 */
const periodKindFor = (frequency) => {
    switch (frequency) {
        case "ONCE_A_YEAR":
            return "YEAR";
        case "MONTH":
            return "REPORTING";
        default:
            return "ROLLUP";   // TRIMESTER, and anything new
    }
};

const emptyColumn = (ordinal, subjectScoped, frequency) => ({
    code: "",
    label: "",
    ordinal,
    groupLabel: "",
    kind: "INPUT",
    periodKind: periodKindFor(frequency),
    subjectScoped,
    scaleMin: 1,
    scaleMax: 7,
    decimals: 0,
    allowSpecialValues: true,
    allowOverride: true,
    parentVisible: true,
    rule: null
});

const ColumnEditor = ({columns, onChange, journal, pickable, periods, issuesByCode}) => {

    const [advanced, setAdvanced] = useState(false);

    const setColumn = (index, patch) =>
        onChange(columns.map((c, i) => i === index ? {...c, ...patch} : c));

    const move = (index, by) => {
        const target = index + by;
        if (target < 0 || target >= columns.length) return;
        const next = [...columns];
        [next[index], next[target]] = [next[target], next[index]];
        onChange(next.map((c, i) => ({...c, ordinal: i})));
    };

    return (
        <div>
            <div style={{
                display: "flex", justifyContent: "space-between",
                alignItems: "center", marginBottom: 12
            }}>
                <FormControlLabel
                    control={<Checkbox checked={advanced}
                                       onChange={(e) => setAdvanced(e.target.checked)}/>}
                    label="დამატებითი პარამეტრები"
                />
                <Button
                    startIcon={<Add/>}
                    onClick={() => onChange([...columns,
                        emptyColumn(columns.length, journal.subjectScoped,
                            journal.frequency)])}
                    style={{textTransform: "none"}}
                >
                    სვეტის დამატება
                </Button>
            </div>

            {columns.map((column, index) => {
                const issues = issuesByCode[column.code] || [];
                const errors = issues.filter(i => i.severity === "ERROR");

                return (
                    <Accordion key={index} defaultExpanded={!column.code}>
                        <AccordionSummary expandIcon={<ExpandMore/>}>
                            <div style={{
                                display: "flex", alignItems: "center",
                                gap: 12, width: "100%"
                            }}>
                                <Typography style={{minWidth: 28, color: "#999"}}>
                                    {index + 1}
                                </Typography>
                                <Typography style={{flex: 1, fontWeight: 500}}>
                                    {column.label || column.code || "ახალი სვეტი"}
                                </Typography>

                                {column.kind === "DERIVED"
                                    ? <Chip size="small" icon={<Functions/>} label="გამოთვლადი"/>
                                    : null}
                                {column.groupLabel
                                    ? <Chip size="small" variant="outlined"
                                            label={column.groupLabel}/>
                                    : null}
                                {errors.length
                                    ? <Chip size="small" color="error"
                                            label={errors[0].message}/>
                                    : null}

                                <IconButton size="small" onClick={(e) => {
                                    e.stopPropagation();
                                    move(index, -1);
                                }}><ArrowUpward fontSize="small"/></IconButton>
                                <IconButton size="small" onClick={(e) => {
                                    e.stopPropagation();
                                    move(index, 1);
                                }}><ArrowDownward fontSize="small"/></IconButton>
                                <IconButton size="small" onClick={(e) => {
                                    e.stopPropagation();
                                    onChange(columns.filter((c, i) => i !== index)
                                        .map((c, i) => ({...c, ordinal: i})));
                                }}><Delete fontSize="small"/></IconButton>
                            </div>
                        </AccordionSummary>

                        <AccordionDetails>
                            <div style={{display: "flex", gap: 12, marginBottom: 12}}>
                                <TextField
                                    size="small" label="დასახელება" style={{flex: 1}}
                                    value={column.label || ""}
                                    onChange={(e) => setColumn(index, {
                                        label: e.target.value
                                    })}
                                    // Derived when the label is finished, not
                                    // per keystroke. Deriving it on every
                                    // change took the code from the FIRST
                                    // CHARACTER - "I \u10e2\u10e0\u10d8\u10db\u10d4\u10e1\u10e2\u10e0\u10d8" became "I", and
                                    // so did every other label starting with
                                    // the same word, which then collided on
                                    // activation with "duplicate code".
                                    onBlur={(e) => {
                                        if (!column.code) {
                                            setColumn(index, {code: slug(e.target.value)});
                                        }
                                    }}
                                />
                                <TextField
                                    size="small" label="ჯგუფი (სათაური)" style={{flex: 1}}
                                    placeholder="მაგ. მიმდინარე შეფასება"
                                    value={column.groupLabel || ""}
                                    onChange={(e) =>
                                        setColumn(index, {groupLabel: e.target.value})}
                                />
                                <TextField
                                    select size="small" label="ტიპი" style={{width: 170}}
                                    value={column.kind}
                                    onChange={(e) => setColumn(index, {
                                        kind: e.target.value,
                                        rule: e.target.value === "DERIVED"
                                            ? (column.rule || emptyRule()) : null
                                    })}
                                >
                                    <MenuItem value="INPUT">ხელით შეყვანა</MenuItem>
                                    <MenuItem value="DERIVED">გამოითვლება</MenuItem>
                                </TextField>
                            </div>

                            <div style={{display: "flex", gap: 12, marginBottom: 12}}>
                                <TextField
                                    type="number" size="small" label="მინიმუმი"
                                    style={{width: 110}}
                                    value={column.scaleMin ?? ""}
                                    onChange={(e) =>
                                        setColumn(index, {scaleMin: numberOrNull(e.target.value)})}
                                />
                                <TextField
                                    type="number" size="small" label="მაქსიმუმი"
                                    style={{width: 110}}
                                    value={column.scaleMax ?? ""}
                                    onChange={(e) =>
                                        setColumn(index, {scaleMax: numberOrNull(e.target.value)})}
                                />
                                <TextField
                                    type="number" size="small" label="ათწილადი"
                                    style={{width: 110}}
                                    value={column.decimals}
                                    onChange={(e) =>
                                        setColumn(index, {decimals: Number(e.target.value)})}
                                />
                                <FormControlLabel
                                    control={
                                        <Checkbox
                                            checked={column.allowSpecialValues}
                                            onChange={(e) => setColumn(index,
                                                {allowSpecialValues: e.target.checked})}
                                        />
                                    }
                                    label="ჩთ დაშვებულია"
                                />
                            </div>

                            {column.kind === "DERIVED" ? (
                                <>
                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={column.allowOverride}
                                                onChange={(e) => setColumn(index,
                                                    {allowOverride: e.target.checked})}
                                            />
                                        }
                                        label="ხელით შესწორება დაშვებულია"
                                    />
                                    <Typography variant="caption"
                                                style={{
                                                    display: "block", color: "#666",
                                                    marginBottom: 12
                                                }}>
                                        ფორმულა დამხმარეა და არა შემზღუდველი — ჩაკეტვა
                                        განზრახ გამონაკლისია.
                                    </Typography>

                                    <FormulaEditor
                                        rule={column.rule || emptyRule()}
                                        onChange={(rule) => setColumn(index, {rule})}
                                        columns={columns}
                                        pickable={pickable}
                                        periods={periods}
                                        advanced={advanced}
                                    />
                                </>
                            ) : null}

                            {advanced ? (
                                <div style={{display: "flex", gap: 12, marginTop: 16}}>
                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={column.subjectScoped}
                                                onChange={(e) => setColumn(index,
                                                    {subjectScoped: e.target.checked})}
                                            />
                                        }
                                        label="საგნის მიხედვით"
                                    />
                                </div>
                            ) : null}
                        </AccordionDetails>
                    </Accordion>
                );
            })}

            {columns.length === 0 ? (
                <Typography variant="body2" style={{
                    color: "#888", padding: 24,
                    textAlign: "center"
                }}>
                    ჯერ სვეტები არ არის. დაამატეთ პირველი.
                </Typography>
            ) : null}
        </div>
    );
};

/**
 * A stable ASCII code from a label; the label stays free to change.
 *
 * The console is Georgian, and Georgian has no uppercase ASCII form - so
 * stripping [^A-Z0-9_] left nothing but the underscores the spaces became.
 * "მიმდინარე შეფასება" produced "_", and the second multi-word column in any
 * journal collided with the first: applyComponents kept one, and activation
 * then failed with "duplicate code: _".
 *
 * So the label is only turned into a code when the *whole* of it survives the
 * transformation. Anything else gets a generated code, which is what the
 * fallback was always for - it just could never be reached.
 *
 * "Keeps something recognisable" is not enough: "I ტრიმესტრი" reduces to
 * "I_", and so does every other label beginning with the same word.
 */
const slug = (label) => {
    const upper = (label || "").trim().toUpperCase();
    const ascii = upper.replace(/\s+/g, "_");
    return /^[A-Z0-9_]+$/.test(ascii)
        ? ascii
        : `COL_${Date.now().toString(36).toUpperCase()}`
        + Math.floor(Math.random() * 1296).toString(36).toUpperCase();
};

const numberOrNull = (v) => v === "" || v === null ? null : Number(v);

export default ColumnEditor;
