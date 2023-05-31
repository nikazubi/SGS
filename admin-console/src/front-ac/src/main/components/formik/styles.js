import styled from '@material-ui/core/styles/styled';
import TextField from '@material-ui/core/TextField';
import DateRange from '@material-ui/icons/DateRange';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Box from '@material-ui/core/Box';
import Button from '@material-ui/core/Button';
import Select from '@material-ui/core/Select';
import {KeyboardDatePicker, KeyboardDateTimePicker} from '@material-ui/pickers';
import {Autocomplete} from "@material-ui/lab";
import {ListItemIcon, ListItemText, MenuItem, MenuList} from "@mui/material";
import {theme} from "../../../assets/theme";

export const StyledAutoComplete = styled(Autocomplete)({
  '&   .MuiAutocomplete-tag': {
    margin: '5px',
  },
  '& .MuiAutocomplete-popper': {
    width: '400px!important',
  },
  '& .MuiAutocomplete-popupIndicator': {
    padding: '0px!important',
    marginTop: "2px!important",
  },
  '& .MuiAutocomplete-clearIndicator': {
    padding: '0px!important',
    marginTop: "2px!important",
  },
});

export const StyledAutoCompleteWithNoPedding = styled(Autocomplete)({
  '& .MuiAutocomplete-inputRoot': {
    padding: '0px!important',
    paddingRight: '65px!important',
  },
  '& .MuiAutocomplete-input': {
    padding: '7px 10px!important',
  },
  '& .MuiAutocomplete-popper': {
    width: '400px!important',
  },
  '& .MuiAutocomplete-popupIndicator': {
    padding: '0px!important',
    marginTop: "2px!important",
  },
  '& .MuiAutocomplete-clearIndicator': {
    padding: '0px!important',
    marginTop: "2px!important",
  },
});

export const FilterTextField = styled(TextField)({
  margin: '0 5px 0 0',
  '& input': {
    paddingTop: 7,
    paddingBottom: 7,
  },
  '& .MuiSelect-select': {
    paddingTop: 7,
    paddingBottom: 7,
  }
});

export const FilterDateTimeField = styled(KeyboardDateTimePicker)({
  margin: '0 5px 0 0',
  '& input': {
    paddingTop: 7,
    paddingBottom: 7,
  },
  '& .MuiSelect-select': {
    paddingTop: 7,
    paddingBottom: 7,
  }
});

export const FilterDateField = styled(KeyboardDatePicker)({
  margin: '0 5px 0 0',
  '& input': {
    paddingTop: 7,
    paddingBottom: 7,
  },
  '& .MuiSelect-select': {
    paddingTop: 7,
    paddingBottom: 7,
  }
});

export const FilterSelect = styled(Select)(() => ({
  margin: '0 5px 0 0',
  '& input': {
    paddingTop: 7,
    paddingBottom: 7,
  },
  '& .MuiSelect-select': {
    paddingTop: 7,
    paddingBottom: 7,
  }
}));

export const FilterButton = styled(Button)(() => ({
  margin: '0 5px 0 0',
  padding: '4px 0 3px 0',
  minWidth: 45
}));

export const DateRangeIcon = styled(DateRange)({
  color: '#ccc'
});

export const CheckboxControl = styled(FormControlLabel)({
  marginLeft: 0,
  marginRight: 0,
  '& .MuiTypography-root': {
    whiteSpace: 'nowrap'
  },
});

export const CheckboxWrapper = styled(Box)(({theme}) => ({
  paddingLeft: 5,
  paddingRight: 8,
  marginTop: 5,
  marginBottom: 5,
  border: '1px solid',
  borderRadius: theme.shape.borderRadius,
  borderColor: theme.palette.primary.borderColor,
  '& .MuiIconButton-root': {
    padding: 3.5,
  }
}));


export const ClearButton = styled(Button)(() => ({
  padding: '3.5px 10px',
  minWidth: 40,
  marginLeft: 4,
  marginRight: 8,
}));

export const PopperMenuListWrapper = styled(MenuList)(() => ({
  backgroundColor: '#ddf1e5'
}));

export const PopperMenuItemWrapper = styled(MenuItem)(() => ({
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  height: "50px"
}));

export const PopperListItemIconWrapper = styled(ListItemIcon)(() => ({
  color: `${theme.palette.primary.main} !important`,
  marginRight: "4px",
}));

export const PopperListItemTextWrapper = styled(ListItemText)(() => ({
  color: theme.palette.primary.main
}));
