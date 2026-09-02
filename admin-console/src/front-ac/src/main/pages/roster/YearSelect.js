import React from "react";
import {useQuery} from "react-query";
import {MenuItem, TextField} from "@mui/material";
import {fetchYears} from "./rosterApi";

/**
 * Which academic year the screen is about.
 *
 * Every roster screen carries one, because the model does: a class belongs to a
 * year and a child is enrolled in a class *for* a year. Without it, "which
 * class is this child in" has no answer at all.
 *
 * Defaults to the current year, which is the one anybody opening the screen
 * means. Selecting an old one is how last year's classes are read.
 */
export const useYears = () => useQuery(["ROSTER_YEARS"], fetchYears, {
    refetchOnWindowFocus: false,
    staleTime: 5 * 60 * 1000
});

export const currentYearId = (years) => {
    if (!years || !years.length) return null;
    const current = years.find(y => y.current);
    return (current || years[0]).id;
};

const YearSelect = ({value, onChange, years, disabled}) => (
    <TextField
        select
        size="small"
        label="სასწავლო წელი"
        value={value || ""}
        disabled={disabled || !years || !years.length}
        onChange={(e) => onChange(Number(e.target.value))}
        sx={{minWidth: 180}}
    >
        {(years || []).map(year => (
            <MenuItem key={year.id} value={year.id}>
                {year.code}{year.current ? " (მიმდინარე)" : ""}
            </MenuItem>
        ))}
    </TextField>
);

export default YearSelect;
