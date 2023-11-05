import renderCellExpand from "./GridCellExpand";
import renderFormCellExpand from "./FormGridCellExpand";

export const resolveColumns = (columns) => {
    columns.forEach(column => {
        // column['sortable'] = false; //TODO Sorting is disabled until server side sorting is implemented
        if (column['type'] === "actions" && column["width"] === undefined && !!column["actionCount"]) {
            column["width"] = column["actionCount"] * 42;
            column["resizable"] = false;
        }
        if (column["flex"] === undefined && column["width"] === undefined) {
            column["flex"] = 1;
            if (column["minWidth"] === undefined) {
                column["minWidth"] = 150;
            }
        }
        if (column["renderCell"] === undefined && (column["type"] !== "actions") && (column["type"] !== "boolean")) {
            column["renderCell"] = (props) => renderCellExpand({
                ...props,
                valueProps: column["valueProps"],
                avatarProps: column["avatarProps"],
                translate: column["translate"],
                avatar: !!column["avatar"] ? column["avatar"](props) : null,
            });
        }
    });
    return columns;
};

export const resolveColumnsFormGrid = (columns, setValid) => {
    columns.forEach(column => {
        let isValid = column["isValid"]
        if (isValid === undefined || isValid == null) {
            isValid = () => true
        }
        column['isValid'] = (row) => isValid(row);
        if (column['type'] === "actions" && column["width"] === undefined && !!column["actionCount"]) {
            column["width"] = column["actionCount"] * 42;
        }
        if (column["flex"] === undefined && column["width"] === undefined) {
            column["flex"] = 1;
            if (column["minWidth"] === undefined) {
                column["minWidth"] = 150;
            }
        }
        if (column["renderCell"] === undefined && !column["type"]) {
            column["renderCell"] = (params) =>
                renderFormCellExpand({
                        ...params,
                        valueProps: column["valueProps"],
                        avatarProps: column["avatarProps"],
                        translate: column["translate"],
                        isValid: () => isValid(params),
                        setValid: setValid
                    }
                );
        }
    });
    return columns;
};

export const resolveGridCellTextValue = (value, valueProps, translate) => {
    let resolvedValue = (value !== undefined && value != null) ? value : '';
    if (valueProps?.isList) {
        resolvedValue = !translate ? value : value.map(v => translate(v));
        return !!valueProps?.delimiter ? resolvedValue.join(valueProps.delimiter) : resolvedValue.join(", ");
    } else {
        return !translate ? value : translate(value);
    }
}

export const resolveQueryData = (data, resolveQueryResult, pagination, maxPageSize) => {
    if (!!resolveQueryResult && pagination) {
        return {
            totalElements: data.totalElements,
            content: resolveQueryResult(data.content)
        };
    } else if (!!resolveQueryResult && !pagination) {
        return {
            totalElements: maxPageSize,
            content: resolveQueryResult(data)
        };
    } else if (!resolveQueryResult && !pagination) {
        return {
            totalElements: maxPageSize,
            content: data
        };
    }
    return data;
};

export const AvatarWithTextGridValueGetter = (textValue, avatar) => ({
    textValue: textValue,
    avatar: !!avatar ? avatar : null
});