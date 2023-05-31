import React, { useEffect, useRef, useState } from 'react';
import TextField from './TextField';
import { useField, useFormikContext } from 'formik';
import Progress from "../Progress";
import { Chip } from "@material-ui/core";
import { IconPickerItem } from "react-fa-icon-picker";
import { Cancel } from "@mui/icons-material";
import { Avatar, Tooltip } from "@mui/material";
import * as S from "./styles.js";

export const FormikAutocomplete = ({
                                     label,
                                     name,
                                     onFetch,
                                     loading,
                                     getOptionLabel: getOptionLabelImpl,
                                     getOptionSelected,
                                     resolveData,
                                     setInitialVulue,
                                     additionalOptions,
                                     multiple = true,
                                     minLengthForSearch = 0,
                                     readOnly,
                                     query,
                                     disabled,
                                     labelDisabled = false,
                                     variant = 'outlined',
                                     onChange,
                                     resolveValue,
                                     handleClear,
                                     shouldUseChipsInOptions = false,
                                     smallChips = false,
                                     hideTagsOnOverflow = false,
                                     hideLabelOnOverflow = false,
                                     getChipColor = () => 'green',
                                     imagePlacement,
                                     selectedImageViewEnabled,
                                     ...props
                                   }) => {

  const [options, setOptions] = useState([]);
  const [open, setOpen] = useState(false);
  const {setFieldValue} = useFormikContext();
  const [focused, setFocused] = useState(false);
  const [inputField, setInputField] = useState('');
  const [field, meta] = useField({name});
  const getOptionLabelRef = useRef(getOptionLabelImpl);
  const getOptionLabel = getOptionLabelRef.current;

  const value = resolveValue ? resolveValue(field.value) : field.value;
  const title = !labelDisabled ? (label) : null;
  const errorText = meta.touched && meta.error ? meta.error : '';

  useEffect(() => {
    let active = true;
    const currLabel = !!value ? getOptionLabel(value) : null;
    const queryKey = currLabel === inputField ? '' : inputField;
    const queryReq = !!query ? {...query, queryKey} : {queryKey};
    if (inputField.length >= minLengthForSearch) {
      onFetch(queryReq).then((data) => {
        if (data) {
          let resolvedData = data;
          if (resolveData && typeof resolveData === 'function') {
            resolvedData = resolveData(data);
          }

          if (active) {
            setOptions(() => {
              const allOptions = resolvedData;
              if (additionalOptions) {
                allOptions.unshift(...additionalOptions);
              }
              return allOptions.filter((opt, index, self) => {
                return self.findIndex(el => getOptionSelected(el, opt)) === index;
              });
            });
          }
        }
      });
    }
    return () => {
      active = false;
    };
  }, [value, inputField, query, onFetch, getOptionLabel, getOptionSelected, minLengthForSearch, resolveData, additionalOptions, setOptions]);

  useEffect(() => {
    if (value === "" && options.length !== 0 && !!setInitialVulue && typeof setInitialVulue === 'function') {
      const initialValue = setInitialVulue(options);
      if (initialValue !== undefined) {
        setFieldValue(name, setInitialVulue(options));
      }
    }
  }, [value, options, name, setFieldValue, setInitialVulue])

  const readOnlyProps = readOnly ? {
    open: false,
    ChipProps: {size: 'small', onDelete: null},
    disableClearable: true,
    forcePopupIcon: false,
  } : {};

  const showEmptyValue = readOnly && (Array.isArray(value) ? value.length === 0 : !value);

  const hideTagsProps = hideTagsOnOverflow ? {
    renderTags: (tags, getTagProps) => {
      return (
        <div style={{
          ...(focused ? {} : {
            width: `calc(${props?.style?.maxWidth || props?.style?.width || '100%'} - 60px)`,
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap"
          })
        }}>
          {tags.map((tag, index) => {
            return <Chip
              key={index}
              label={
                <div
                  style={{
                    maxWidth: `calc(${props?.style?.maxWidth || props?.style?.width || '100%'})`,
                    overflow: "hidden",
                    textOverflow: "ellipsis"
                  }}
                >
                  {getOptionLabel(tag)}
                </div>
              }
              {...(smallChips ? {
                style: {height: "18px", fontSize: "13px"},
                deleteIcon: <Cancel style={{transform: "scale(0.7)"}}/>
              } : {})}
              {...getTagProps(index)}
              {...(focused ? {} : {onDelete: null})}
            />;
          })}
        </div>
      );
    }
  } : {};

  return (
    <>
      {showEmptyValue ?
        <TextField
          variant={variant}
          value="მონაცემები არ მოიძებნა"
          label={title}
          InputProps={{readOnly}}
        /> :
        <S.StyledAutoComplete
          open={open}
          disabled={disabled}
          onOpen={() => setOpen(true)}
          onClose={() => setOpen(false)}
          loading={loading}
          multiple={multiple}
          options={options}
          value={value === '' ? null : value}
          // filterSelectedOptions
          getOptionSelected={getOptionSelected}
          getOptionLabel={getOptionLabel}
          onChange={(event, value, status) => {
            if (status === "clear" && !!handleClear) {
              handleClear();
            } else {
              if (onChange) {
                onChange(event, value);
              } else {
                setFieldValue(name, value);
              }
            }
          }}
          onInputChange={(e, val, status) => {
            if (status === "clear" && !!handleClear) {
              handleClear();
              setInputField("");
            } else {
              setInputField(val);
            }
          }}
          filterOptions={x => x}
          noOptionsText={('nothingFound')}
          disableCloseOnSelect={multiple}
          ChipProps={{
            ...(smallChips ? {
              style: {height: "18px", fontSize: "13px"},
              deleteIcon: <Cancel style={{transform: "scale(0.7)"}}/>
            } : {size: 'small'})
          }}
          renderInput={(params) => {
            return (
              <Tooltip title={
                multiple && !focused && ((hideLabelOnOverflow && title && title.trim().length > 0) || (field.value && field.value.length > 0)) ?
                  <div>
                    {hideLabelOnOverflow && title && title.trim().length > 0 &&
                    <div style={{fontSize: "15px"}}>{title}</div>}
                    {hideLabelOnOverflow && title && title.trim().length > 0 && field.value && field.value.length > 0 &&
                    <div style={{backgroundColor: "white", marginTop: "7px", height: "1px", width: "100%"}}/>}
                    <ul style={{listStyleType: 'none', margin: '0', padding: '0', fontSize: "15px"}}>
                      {field.value && field.value.map((tag, index) => (
                        <li key={index} style={{paddingTop: "3px", paddingBottom: "3px"}}>{getOptionLabel(tag)}</li>
                      ))}
                    </ul>
                  </div>
                  :
                  !multiple && !focused && ((hideLabelOnOverflow && title && title.trim().length > 0) || (field.value)) ?
                    <div>
                      {hideLabelOnOverflow && title && title.trim().length > 0 &&
                      <div style={{fontSize: "15px"}}>{title}</div>}
                      {hideLabelOnOverflow && title && title.trim().length > 0 && field.value &&
                      <div style={{backgroundColor: "white", marginTop: "7px", height: "1px", width: "100%"}}/>}
                      {field.value &&
                      <div style={{fontSize: "15px", marginTop: "3px"}}>{getOptionLabel(field.value)}</div>}
                    </div>
                    :
                    ""
              }>
                <div>
                  <TextField
                    {...params}
                    error={!!errorText}
                    helperText={errorText !== ' ' && errorText}
                    variant={variant}
                    label={!labelDisabled ? title : null}
                    onFocus={_ => setFocused(true)}
                    onBlur={_ => setFocused(false)}
                    InputProps={{
                      ...params.InputProps,
                      readOnly,
                      endAdornment: (
                        <>
                          {loading ? <Progress/> : null}
                          {params.InputProps.endAdornment}
                          {(selectedImageViewEnabled && (field.value?.logoUrl || field.value?.imageUrl || field.value?.image)) &&  (
                            <Avatar
                              src={field.value.logoUrl || field.value.imageUrl || field.value.image}
                              sx={{width: 24, height: 24}}
                              variant={"square"}
                            />
                          )}
                        </>
                      ),
                      style: {
                        ...(multiple && focused && field.value && field.value.length > 0 ? {} : {height: variant === "outlined" ? "40px" : "29px"})
                      }
                    }}
                    InputLabelProps={{
                      ...(hideLabelOnOverflow ? {
                        ...(focused || (field.value && (multiple ? field.value.length > 0 : true)) ? {
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
                            width: `calc(${props?.style?.maxWidth || '100%'} - 45px)`
                          }
                        })
                      } : {})
                    }}
                  />
                </div>
              </Tooltip>
            );
          }}
          renderOption={data => {
           if (shouldUseChipsInOptions) {
              return <Chip
                style={{
                  color: data?.color || getChipColor(data),
                  borderColor: data?.color || getChipColor(data)
                }}
                avatar={
                  <IconPickerItem
                    icon={data?.icon}
                    size={16}
                    color={data?.color || getChipColor(data)}
                    containerStyles={{marginTop: data?.icon ? 6 : 0, marginLeft: data?.icon ? 6 : 0}}
                  />
                }
                variant="outlined"
                label={getOptionLabel(data)}
                size="small"
                onDelete={null}
              />;
            } else if (data.color) {
              return <div style={data?.color ? {color: data?.color} : {}}> {getOptionLabel(data)} </div>;
            } else {
              return getOptionLabel(data);
            }
          }}
          {...props}
          {...hideTagsProps}
          {...readOnlyProps}
        />}
    </>
  );
};

export default FormikAutocomplete;
