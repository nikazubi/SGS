import { Avatar, Box, Tooltip } from "@mui/material";
import * as React from 'react';
import PropTypes from 'prop-types';
import { useEffect, useMemo, useRef, useState } from "react";
import { styled } from "@mui/material/styles";
import { resolveGridCellTextValue } from "./resolvers";
import ListToolTipTitle from "./ListToolTipTitle";

const CustomWidthTooltip = styled(({ className, ...props }) => (
  <Tooltip {...props} classes={{ popper: className }} />
))(({width}) => ({
  maxWidth: width
}));

function isOverflown(element) {
  return (
    element.scrollHeight > element.clientHeight ||
    element.scrollWidth > element.clientWidth
  );
}

const FormGridCellExpand = React.memo((props) => {
  const { value, width, valueProps, avatarProps, translate, field, isValid, setValid, rowId} = props;
  const cellValue = useRef(null);
  const [showToolTip, setShowToolTip] = useState(false);
  const resolvedValue = resolveGridCellTextValue(value, valueProps, translate);

  const isFieldValid = useMemo(() => {
    return isValid();
  }, [isValid])

  const resolveAvatar = () => {
    let alt;
    if (!!avatarProps?.alt) {
      alt = avatarProps.alt(resolvedValue);
    }
    return (
      <Avatar src={value.avatar}
              variant={!!avatarProps?.variant ? avatarProps.variant : "circular"}
              sx={{marginRight: 1}}
      >
        {alt?.toUpperCase() || resolvedValue?.charAt(0)?.toUpperCase()}
      </Avatar>
    );
  };

  const handleMouseEnter = () => {
    const isCurrentlyOverflown = isOverflown(cellValue.current);
    setShowToolTip(isCurrentlyOverflown);
  };

  const handleMouseLeave = () => {
    setShowToolTip(false);
  };

  const getTooltipTitle = () => {
    const title = !valueProps?.isList ? resolvedValue : ListToolTipTitle(value)
    return !!title ? title : "";
  }

  useEffect(() => {
    if (!showToolTip) {
      return undefined;
    }

    function handleKeyDown(nativeEvent) {
      if (nativeEvent.key === 'Escape' || nativeEvent.key === 'Esc') {
        setShowToolTip(false);
      }
    }

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [setShowToolTip, showToolTip]);

  useEffect(() => {
    setValid(prev => ({
      ...prev,
      [rowId]:
        {
        ...prev[rowId],
        [field]: isFieldValid
        }
    }))
  }, [field, isFieldValid, value, setValid, rowId])

  return (
    <Box
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      sx={{
        alignItems: 'center',
        lineHeight: '24px',
        width: 1,
        height: 1,
        position: 'relative',
        display: 'flex',
      }}
    >
      {value.avatar !== undefined && (
        resolveAvatar()
      )}
      <CustomWidthTooltip
        title={getTooltipTitle()}
        width={width}
        open={showToolTip}
        interactive={true}
      >
        <Box
          ref={cellValue}
          sx={{whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}
        >
          {resolvedValue}
        </Box>
      </CustomWidthTooltip>
    </Box>
  );
});

FormGridCellExpand.propTypes = {
  value: PropTypes.any.isRequired,
  width: PropTypes.number.isRequired,
};

const renderFormCellExpand = (params) => {
  return (
    <FormGridCellExpand
      value={params.value || ''}
      width={params.colDef.computedWidth}
      valueProps={params.valueProps}
      avatarProps={params.avatarProps}
      translate={params.translate}
      setValid={params.setValid}
      isValid={params.isValid}
      field={params.field}
      rowId={params.id}
    />
  );
}

renderFormCellExpand.propTypes = {
  colDef: PropTypes.object.isRequired,
  value: PropTypes.any.isRequired,
};

export default renderFormCellExpand;