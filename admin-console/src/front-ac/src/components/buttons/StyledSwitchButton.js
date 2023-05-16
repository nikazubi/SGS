import Switch from '@mui/material/Switch';
import { alpha, styled } from '@mui/material/styles';

const StyledSwitchButton = styled(Switch)(({ theme }) => ({
  '& .MuiSwitch-switchBase.Mui-checked': {
    color: '#00A651',
    '&:hover': {
      backgroundColor: alpha('#00A651', theme.palette.action.hoverOpacity),
    },
  },
  '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
    backgroundColor: '#00A651',
  },
}));

export default StyledSwitchButton;