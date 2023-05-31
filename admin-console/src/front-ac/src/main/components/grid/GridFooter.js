import {
  GridFooterContainer, GridRowCount, GridSelectedRowCount,
  gridTopLevelRowCountSelector,
  gridVisibleTopLevelRowCountSelector,
  selectedGridRowsCountSelector,
  useGridApiContext,
  useGridRootProps,
  useGridSelector
} from "@mui/x-data-grid-pro";

const GridFooter = ({selectedRowCount, ...props}) => {
  const apiRef = useGridApiContext();
  const rootProps = useGridRootProps();
  const totalTopLevelRowCount = useGridSelector(apiRef, gridTopLevelRowCountSelector);
  const selectedRowCountUncontrolled = useGridSelector(apiRef, selectedGridRowsCountSelector);
  const visibleTopLevelRowCount = useGridSelector(apiRef, gridVisibleTopLevelRowCountSelector);

  const selectedRowCountElement =
    !rootProps.hideFooterSelectedRowCount && selectedRowCountUncontrolled > 0 ? (
      <GridSelectedRowCount selectedRowCount={!!selectedRowCount ? selectedRowCount : selectedRowCountUncontrolled} />
    ) : (
      <div />
    );

  const rowCountElement =
    !rootProps.hideFooterRowCount && !rootProps.pagination ? (
      <GridRowCount rowCount={totalTopLevelRowCount} visibleRowCount={visibleTopLevelRowCount} />
    ) : null;

  const paginationElement = rootProps.pagination &&
    !rootProps.hideFooterPagination &&
    rootProps.components.Pagination && (
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