import React from 'react';
import { Box } from '@material-ui/core';
import { TableRefProvider } from '../../contexts/table-ref-context';
import ErrorPage from '../../components/ErrorPage';
import { ErrorBoundary } from 'react-error-boundary';
import styled from '@material-ui/core/styles/styled';
import { useNavigate } from '../../contexts/navigation-context';

const Wrapper = styled(Box)({
  flexGrow: 1,
  height: 0,
  overflow: 'auto',
});

const TabPanels = () => {
  const { tabList, activeTab } = useNavigate()

  return (
    tabList.map(({ id, component }) => (
      <Wrapper  style={{overflowY: "hidden"}} key={id} hidden={id !== activeTab}>
        <TableRefProvider pageId={id} >
          <ErrorBoundary FallbackComponent={ErrorPage}>
            {component}
          </ErrorBoundary>
        </TableRefProvider>
      </Wrapper>
    ))
  );
};

export default TabPanels;
