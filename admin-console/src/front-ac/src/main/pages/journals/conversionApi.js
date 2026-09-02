import axios from "../../../utils/axios";

/**
 * The one formula marks are printed on.
 *
 * The school grades out of 7 and must report to the government out of 10; they
 * are moving to a 9-point scale and have not settled the mapping, so the
 * formula is theirs to change.
 *
 * Representation only — no grade is ever stored converted, so saving this
 * cannot affect a single mark.
 */

export const fetchConversionFormula = async () => {
    const {data} = await axios.get("/api/gradebook/conversion-formula");
    return data || null;
};

export const saveConversionFormula = async ({name, multiplier, offsetValue}) => {
    const {data} = await axios.post("/api/gradebook/conversion-formula",
        {name, multiplier, offsetValue});
    return data;
};

/**
 * What the formula does to one number, for the editor's live preview.
 *
 * Deliberately a second implementation of the server's rule, and only ever used
 * to show someone what they are typing — never to store or print anything. The
 * grid and the exports both read the server's value, so this cannot drift into
 * being believed.
 */
export const previewFormula = (formula, raw) => {
    if (formula == null || raw === null || raw === "" || Number.isNaN(Number(raw))) {
        return "";
    }
    const multiplier = formula.multiplier == null ? 1 : Number(formula.multiplier);
    const offset = formula.offsetValue == null ? 0 : Number(formula.offsetValue);
    // Not rounded, matching the server: the school asked for the formula's
    // output verbatim, so 6.5 through "+3" reads 9.5.
    return String(Number((Number(raw) * multiplier + offset).toFixed(6)));
};
