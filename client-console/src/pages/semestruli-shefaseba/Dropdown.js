import * as React from 'react';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select from '@mui/material/Select';

export default function Dropdown() {
  const [subject, setSubject] = React.useState('');

  const handleChange = (event) => {
    setSubject(event.target.value);
  };

  return (
    <FormControl sx={{ m: 1, minWidth: 120 }} size="small">
      <InputLabel id="demo-select-small-label">აირჩიე საგანი</InputLabel>
      <Select
        labelId="demo-select-small-label"
        id="demo-select-small"
        value={subject}
        label="აირჩიე საგანი"
        onChange={handleChange}
      >
        <MenuItem value={'1ა'}>1ა</MenuItem>
        <MenuItem value={'2ბ'}>2ბ</MenuItem>
        <MenuItem value={'3გ'}>3გ</MenuItem>
      </Select>
    </FormControl>
  );
}