import React, {useState} from 'react';
import TextField from './TextField';
import {useField} from 'formik';
import {Tooltip} from "@mui/material";

const FormikTextField = ({
                             label,
                             readOnly,
                             shouldPrevent,
                             onChange,
                             errorText: receivedErrorText,
                             multiline,
                             rowNum,
                             value,
                             labelDisabled = false,
                             inputProps,
                             hideLabelOnOverflow = false,
                             removeAutofill = false,
                             ...props
                         }) => {
    const [focused, setFocused] = useState(false);
    const [field, meta] = useField(props);
    const errorText = receivedErrorText ?? (meta.error && meta.touched ? meta.error : '');
    const {onChange: formikOnChange, ...restField} = field;
    const title = !labelDisabled ? (label) : null;

    return (
        <Tooltip title={hideLabelOnOverflow && !labelDisabled && !focused && title && title.trim().length > 0 ?
            <div style={{fontSize: "15px"}}>{title}</div> : ""}>
            <div>
                <TextField
                    helperText={errorText !== ' ' && errorText}
                    error={!!errorText}
                    InputProps={{
                        readOnly: readOnly,
                        ...inputProps
                    }}
                    inputProps={
                        removeAutofill ? {
                            autocomplete: 'new-password',
                            form: {
                                autocomplete: 'off',
                            },
                        } : {}
                    }
                    InputLabelProps={{
                        ...(hideLabelOnOverflow ? {
                            ...(focused || field.value ? {
                                style: {
                                    overflow: "hidden",
                                    textOverflow: "ellipsis",
                                    whiteSpace: "nowrap",
                                    width: `calc(${props?.style?.maxWidth || '100%'})`
                                }
                            } : {
                                style: {
                                    overflow: "hidden",
                                    textOverflow: "ellipsis",
                                    whiteSpace: "nowrap",
                                    width: `calc(${props?.style?.maxWidth || '100%'} - 20px)`
                                }
                            })
                        } : {})
                    }}
                    label={title}
                    {...restField}
                    value={(field.value !== null && typeof field.value !== 'undefined') ? field.value : (value !== null && typeof value !== 'undefined') ? value : ''}
                    onChange={(event) => {
                        if (shouldPrevent && shouldPrevent(event)) {
                            event.preventDefault();
                            return;
                        }
                        if (onChange) {
                            onChange(event);
                        } else {
                            formikOnChange(event);
                        }
                    }}
                    onFocus={_ => setFocused(true)}
                    onBlur={_ => setFocused(false)}
                    autoComplete="off"
                    multiline={multiline}
                    rows={rowNum}
                    {...props}
                />
            </div>
        </Tooltip>
    );
};

export default FormikTextField;
