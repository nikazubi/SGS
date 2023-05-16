import React from 'react';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from './IconButton';

const IconButtonWithTooltip = ({ tooltip, tooltipZIndex, icon, onClick, ...rest }) => (
  <Tooltip title={tooltip} PopperProps={{style: {...(tooltipZIndex ? {zIndex: tooltipZIndex} : {})}}}>
    <div>
      <IconButton
        icon={icon}
        onClick={onClick}
        style={{ padding: 8 }}
        {...rest}
      />
    </div>
  </Tooltip>
);

export default IconButtonWithTooltip;
