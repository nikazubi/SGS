import styled from '@material-ui/core/styles/styled';
import Paper from '@material-ui/core/Paper';
import Box from '@material-ui/core/Box';
import Table from '@material-ui/core/Table';
import TableCell from '@material-ui/core/TableCell';

export const StickyTabsWrapper = styled(Paper)({
  position: 'sticky',
  top: 0,
  backgroundColor: '#fff',
  zIndex: 9999,
});

export const ErrorPage = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  alignItems: 'center',
  height: '100%',
}));

export const BorderedTable = styled(Table)(({ theme }) => ({
  '& > .MuiTableHead-root th, & > .MuiTableBody-root td': {
    border: '1px solid',
    borderColor: theme.palette.primary.borderColor,
  }
}));

export const BorderedTableCell = styled(TableCell)(({ theme }) => ({
  border: '1px solid',
  borderColor: theme.palette.primary.borderColor,
}));
