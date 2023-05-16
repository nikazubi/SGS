import React from 'react';
import IconButton from "@mui/material/IconButton";
import { adjustBrightness } from "../../../utils/StyleUtils";

const IconButtonRound = ({
                           icon,
                           onClick,
                           color = "primary",
                           diameter = "50px",
                           ...rest}) => {
  return(
    <IconButton
      onClick={onClick}
      sx={{
        backgroundColor: color,
        borderRadius: 9999,
        width: diameter,
        height: diameter,
        ':hover': {
          bgcolor: adjustBrightness(color, -40),
        }
      }}
      {...rest}
    >
      {icon}
    </IconButton>
  )
}

export default IconButtonRound;