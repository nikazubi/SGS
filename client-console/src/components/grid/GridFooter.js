import {
    GridFooterContainer,
    GridRowCount,
    GridSelectedRowCount,
    gridTopLevelRowCountSelector,
    selectedGridRowsCountSelector,
    useGridApiContext,
    useGridRootProps,
    useGridSelector
} from "@mui/x-data-grid";

const GridFooter = ({selectedRowCount, ...props}) => {
    const apiRef = useGridApiContext();
    const rootProps = useGridRootProps();
    const totalTopLevelRowCount = useGridSelector(apiRef, gridTopLevelRowCountSelector);
    const selectedRowCountUncontrolled = useGridSelector(apiRef, selectedGridRowsCountSelector);
    const selectedRowCountElement =
        !rootProps.hideFooterSelectedRowCount && selectedRowCountUncontrolled > 0 ? (
            <GridSelectedRowCount
                selectedRowCount={!!selectedRowCount ? selectedRowCount : selectedRowCountUncontrolled}/>
        ) : (
            <div/>
        );

    const rowCountElement =
        !rootProps.hideFooterRowCount && !rootProps.pagination ? (
            <GridRowCount rowCount={totalTopLevelRowCount}/>
        ) : null;

    const paginationElement = rootProps?.pagination &&
        !rootProps.hideFooterPagination &&
        rootProps?.components?.Pagination && (
            <rootProps.components.Pagination {...rootProps.componentsProps?.pagination} />
        );

    return (
        <GridFooterContainer {...props}>
            {selectedRowCountElement}
            {rowCountElement}
            {paginationElement}
        </GridFooterContainer>
    );
}

export default GridFooter;