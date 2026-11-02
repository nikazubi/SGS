import React, {useMemo} from "react";
import {Checkbox, FormControlLabel} from "@mui/material";

/**
 * Classes as checkboxes, grouped by school, with a box per school that ticks
 * all of its classes.
 *
 * For "these classes" choices: the bulk grade release and news. `classes` are
 * the class-picker options ({id, name, schoolName}), which the server has
 * already narrowed to the classes this person may see. `value` is the ticked
 * ids, in the order they were ticked.
 */
const ClassPicker = ({classes, value, onChange}) => {

    const schools = useMemo(() => {
        const bySchool = new Map();
        classes.forEach(c => {
            if (!bySchool.has(c.schoolName)) bySchool.set(c.schoolName, []);
            bySchool.get(c.schoolName).push(c);
        });
        return [...bySchool.entries()];
    }, [classes]);

    const toggleClass = (id) => onChange(value.includes(id)
        ? value.filter(c => c !== id) : [...value, id]);

    const toggleSchool = (schoolClasses) => {
        const ids = schoolClasses.map(c => c.id);
        const all = ids.every(id => value.includes(id));
        onChange(all ? value.filter(id => !ids.includes(id))
            : [...new Set([...value, ...ids])]);
    };

    return schools.map(([school, schoolClasses]) => {
        const picked = schoolClasses.filter(c => value.includes(c.id)).length;
        return (
            <div key={school} style={{marginTop: 8}}>
                <FormControlLabel
                    label={<b>{school}</b>}
                    control={<Checkbox size="small"
                                       checked={picked === schoolClasses.length}
                                       indeterminate={picked > 0 && picked < schoolClasses.length}
                                       onChange={() => toggleSchool(schoolClasses)}/>}/>
                <div style={{display: "flex", flexWrap: "wrap", paddingLeft: 28}}>
                    {schoolClasses.map(c => (
                        <FormControlLabel key={c.id} label={c.name} style={{minWidth: 90}}
                                          control={<Checkbox size="small"
                                                             checked={value.includes(c.id)}
                                                             onChange={() => toggleClass(c.id)}/>}/>
                    ))}
                </div>
            </div>
        );
    });
};

export default ClassPicker;
