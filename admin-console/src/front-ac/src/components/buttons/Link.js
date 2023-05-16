import React from 'react';
import { Link as RouterLink } from "react-router-dom";

const Link = ({children, ...rest}) => (
    <RouterLink
      style={{ textDecoration: 'none' }}
      {...rest}
    >
      {children}
    </RouterLink>
)

export default Link;
