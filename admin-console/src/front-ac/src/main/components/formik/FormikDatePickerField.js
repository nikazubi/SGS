// import React, {useState} from 'react';
// import {KeyboardDatePicker, MuiPickersUtilsProvider} from '@material-ui/pickers';
// import {useField, useFormikContext} from 'formik';
// import DateFnsUtils from '@date-io/date-fns';
// import styled from '@material-ui/core/styles/styled';
// import {DATE_FORMAT} from "../../../constants/utils";
// import {Tooltip} from "@mui/material";
//
// const Picker = styled(KeyboardDatePicker)({
//   '& .MuiInputBase-input': {
//     fontSize: 16,
//   }
// });
//
// export const FormikDatePickerField = ({
//                                         label,
//                                         name,
//                                         labelDisabled = false,
//                                         inputVariant = "outlined",
//                                         readOnly,
//                                         inputProps,
//                                         disabled = false,
//                                         hideLabelOnOverflow = false,
//                                         ...rest
//                                       }) => {
//   const {t} = useTranslation();
//   const {setFieldValue} = useFormikContext();
//   const title = !labelDisabled ? (label || t(name)) : null;
//
//   const [focused, setFocused] = useState(false);
//   const [opened, setOpened] = useState(false);
//   const [field, meta] = useField({name, ...rest});
//   const errorText = meta.error && meta.touched ? meta.error : '';
//   const {onChange: formikOnChange, ...restField} = field;
//
//   return (
//     <MuiPickersUtilsProvider utils={DateFnsUtils}>
//       <Tooltip title={hideLabelOnOverflow && !focused && !opened && title && title.trim().length > 0 ?
//         <div style={{fontSize: "15px"}}>{title}</div> : ""}>
//         <div>
//           <Picker
//             error={!!errorText}
//             helperText={errorText?.trim()}
//             autoOk
//             disableToolbar
//             fullWidth
//             margin="dense"
//             inputVariant={inputVariant}
//             label={title}
//             InputLabelProps={{
//               ...(hideLabelOnOverflow ? {
//                 ...(focused || field.value ? {
//                   style: {
//                     overflow: "hidden",
//                     textOverflow: "ellipsis",
//                     whiteSpace: "nowrap",
//                     width: `calc(${rest?.style?.maxWidth || '100%'})`
//                   }
//                 } : {
//                   style: {
//                     overflow: "hidden",
//                     textOverflow: "ellipsis",
//                     whiteSpace: "nowrap",
//                     width: `calc(${rest?.style?.maxWidth || '100%'} - 40px)`
//                   }
//                 })
//               } : {})
//             }}
//             format={DATE_FORMAT}
//             KeyboardButtonProps={{
//               size: 'small',
//               color: 'primary'
//             }}
//             InputAdornmentProps={{position: 'start'}}
//             {...restField}
//             name={name}
//             value={field.value || null}
//             onChange={val => setFieldValue(name, val)}
//             onFocus={_ => setFocused(true)}
//             onBlur={_ => setFocused(false)}
//             onOpen={_ => setOpened(true)}
//             onClose={_ => setOpened(false)}
//             disabled={disabled}
//             InputProps={{
//               readOnly: readOnly,
//               ...inputProps
//             }}
//             {...rest}
//           />
//         </div>
//       </Tooltip>
//     </MuiPickersUtilsProvider>
//   );
// };
