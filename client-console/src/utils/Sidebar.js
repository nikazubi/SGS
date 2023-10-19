import {Drawer} from '@mui/material';
import {styled} from '@mui/system';

const Sidebar = styled(Drawer)(({ theme }) => ({
    transition: '.5s',
    width: '200px',
    height: 'calc(100vh - 81px) !important',
    top: 'unset !important',
    flexShrink: 0,
    '& .MuiDrawer-paper': {
        width: '200px',
        boxSizing: 'border-box',
    },
    '@media (max-width: 960px)': {
        transform: 'translateX(-100%)',
        position: 'absolute'
    },
  }));

export default Sidebar;