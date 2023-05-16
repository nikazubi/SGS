import { useState } from 'react';

export const useTablePaging = ({ pSize = 20 }) => {
  const [pageInfo, setPageInfo] = useState({
    pageSize: pSize,
    currentPage: 0
  });

  const handleChangePage = (event, newPage) => {
    setPageInfo(prev => ({...prev, currentPage: newPage}));
  };

  const handleChangeRowsPerPage = event => {
    setPageInfo(prev => ({...prev, pageSize: Number(event.target.value)}));
  };

  return {
    pageInfo,
    setPageInfo,
    handleChangePage,
    handleChangeRowsPerPage
  }
}
