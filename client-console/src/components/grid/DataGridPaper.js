import {Paper} from "@mui/material";

const DataGridPaper = ({
                           children,
                           visible = true
                       }) => {
    return (
        <Paper square variant="outlined"
               sx={{height: "100%", display: visible ? "flex" : 'none', flexDirection: "column"}}>
            {children}
        </Paper>
    );
};

export default DataGridPaper;
