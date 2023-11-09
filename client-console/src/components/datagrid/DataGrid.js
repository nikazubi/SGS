import React, {useEffect, useMemo, useState} from "react";
import CustomNoRowsOverlay from "./CustomNoRowsOverlay";
import PropTypes from "prop-types";
import GridFooter from "./GridFooter";
import {DataGridStyles} from "./DataGridStyles";
import {DataGrid} from "@mui/x-data-grid";
import {resolveColumns, resolveQueryData} from "./resolvers";
import {useQueryWithoutCache} from "../../hooks/useQueryWithoutCache";


const DataGridSGS = ({
                         fetchData,
                         rows,
                         queryKey,
                         columns,
                         filters: filtersData,
                         toolbar,
                         resolveQueryResult,
                         onSelectionModelChange,
                         pagination = true,
                         selectedRowCount,
                         queryParams,
                         onCellEditCommit,
                         rowIdField = "id",
                         loading,
                         initialState = {},
                         components,
                         componentsProps,
                         checkboxSelection,
                         colorGroups,
                         getRowClassName,
                         fullyHideFooter = false,
                         ...props
                     }) => {
    const maxPageSize = 5000;
    const resolvedColumns = useMemo(() => resolveColumns(columns), [columns]);
    const rowsPerPageOptions = [5000];
    const [selectionModel, setSelectionModel] = useState([]);
    const [pageParams, setPageParams] = useState({page: 0, size: 5000});
    const {
        data,
        isLoading,
        isSuccess,
        isFetching,
    } = useQueryWithoutCache([queryKey, filtersData, pageParams],
        fetchData,
        {
            select: data => resolveQueryData(data, resolveQueryResult, pagination, maxPageSize),
            enabled: !rows,
            ...useQueryOptions,
            ...queryParams
        });

    useEffect(() => {
        if (!checkboxSelection) setSelectionModel([]);
    }, [checkboxSelection]);

    const isAllChecked = (newSelectionModel) => {
        return newSelectionModel.length === data.content.length;
    };
    const HiddenFooter = (props) => {
        return (
            <div>
            </div>
        );
    }
    const handleCellCommit = (params) => {
        const row = data.content.find(element => element[rowIdField] === params.id);
        const field = params.field;
        const changedRow = {
            ...row,
            [field]: params.value
        };
        return onCellEditCommit(params, changedRow);
    };

    const handleSelectionModalChange = (newSelectionModel) => {
        if (!!onSelectionModelChange) {
            onSelectionModelChange(newSelectionModel, isAllChecked(newSelectionModel), data.totalElements);
            setSelectionModel(newSelectionModel);
        }
    };

    return (
        <div style={{display: 'flex', position: "relative", height: '100%', width: '100%'}}>
            <DataGrid
                columns={resolvedColumns}
                rows={!!rows ? rows : (isSuccess && data ? data.content : [])}
                paginationMode={"server"}
                pagination={pagination}
                rowsPerPageOptions={rowsPerPageOptions}
                pageSize={pageParams.size}
                rowCount={isSuccess && data ? data.totalElements : 0}
                onPageChange={(page) => setPageParams((prev) => ({...prev, page}))}
                onPageSizeChange={(size) => setPageParams((prev) => ({...prev, size}))}
                // loading={isLoading || isFetching || loading}
                disableColumnFilter
                rowBuffer={5000}
                columnBuffer={columns.length}
                getRowId={(row) => row[rowIdField]}
                getRowClassName={getRowClassName}
                onCellEditCommit={handleCellCommit}
                checkboxSelection={checkboxSelection}
                selectionModel={selectionModel}
                onSelectionModelChange={handleSelectionModalChange}
                initialState={{...initialState}}
                components={{
                    // Toolbar: GridToolBar,
                    // LoadingOverlay: Progress,
                    NoRowsOverlay: CustomNoRowsOverlay,
                    NoResultsOverlay: CustomNoRowsOverlay,
                    Footer: fullyHideFooter ? HiddenFooter : GridFooter,
                    ...components,
                }}
                componentsProps={{
                    footer: {selectedRowCount: selectedRowCount},
                    ...componentsProps
                }}
                sx={DataGridStyles(colorGroups)}
                {...props}
            />
        </div>
    );
};

DataGrid.propTypes = {
    columns: PropTypes.array.isRequired,
    queryKey: PropTypes.string.isRequired,
};

const useQueryOptions = {
    keepPreviousData: true,
    staleTime: 60 * 1000,
    useErrorBoundary: true,
    //todo set to true in production
    refetchOnWindowFocus: false,
    notifyOnChangeProps: 'tracked',
};

export default DataGridSGS;