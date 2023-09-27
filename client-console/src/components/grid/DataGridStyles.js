import {adjustBrightness} from "../../../utils/StyleUtils";

export const DataGridStyles = (colorGroups) => {

    const getRowColorStyle = (colorGroupColor) => {
        return {
            backgroundColor: colorGroupColor,
            '&:hover': {
                backgroundColor: adjustBrightness(colorGroupColor, -40)
            }
        };
    };

    const generateRowColorsByColorGroup = (colorGroups) => {
        const styles = {};
        if (!colorGroups) {
            return {};
        }
        for (let colorGroupName of Object.keys(colorGroups)) {
            styles[`& .${colorGroupName}`] = getRowColorStyle(colorGroups[colorGroupName]);
        }
        return styles;
    };

    return {
        "& .MuiDataGrid-actionsCell": {
            gridGap: 0
        },
        "& .MuiDataGrid-checkboxInput": {
            color: "rgb(0,151,61)"
        },
        "& .MuiDataGrid-checkboxInput.Mui-checked": {
            color: "rgb(0,151,61)"
        },
        ...generateRowColorsByColorGroup(colorGroups)
    };
};
