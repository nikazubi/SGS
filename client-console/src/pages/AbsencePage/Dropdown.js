import * as React from 'react';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select from '@mui/material/Select';

export default function Dropdown({data, select, label}) {
  const [currentSelection, setCurrentSelection] = React.useState('');

  const handleChange = (event) => {
    select(event.target.value)
    setCurrentSelection(event.target.value);
  };

  return (
    <FormControl sx={{ m: 1, minWidth: 120 }} size="small">
      <InputLabel id="demo-select-small-label">{label}</InputLabel>
      <Select
        labelId="demo-select-small-label"
        id="demo-select-small"
        value={currentSelection}
        label={label}
        onChange={handleChange}
      >
        {data.map(subject=><MenuItem value={subject}>{subject}</MenuItem>)}
      </Select>
    </FormControl>
  );
}