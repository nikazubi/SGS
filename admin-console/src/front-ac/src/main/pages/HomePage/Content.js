import React from 'react';
import TabNavigation from '../../components/TabNavigation';
import TabPanels from '../../components/TabPanels';
import styled from '@material-ui/core/styles/styled';

const Wrapper = styled('div')({
  flexGrow: 1,
  display: 'flex',
  flexDirection: 'row',
  width: 'calc(100% - 300px)',
  backgroundColor: "#e3f2fa"
});

const Content = () => {
  return (  
    <Wrapper>
      <TabNavigation />
      <TabPanels />
    </Wrapper>
  );
};

export default Content;
