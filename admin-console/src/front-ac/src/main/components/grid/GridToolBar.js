import React from "react";
import { GridToolbarColumnsButton, GridToolbarContainer, GridToolbarDensitySelector } from "@mui/x-data-grid";


const GridToolBar = () => {
  return (
    <GridToolbarContainer>
      <GridToolbarColumnsButton/>
      <GridToolbarDensitySelector/>
    </GridToolbarContainer>
  )
}

export default GridToolBar;