import {createMuiTheme} from '@material-ui/core/styles';
import red from '@material-ui/core/colors/red';

export const theme = createMuiTheme({
  typography: {
    fontFamily: `"Roboto", "DejaVu Sans"`,
  },
  palette: {
    primary: {
      main: '#0e7fb7',
      contrastText: '#ffffff',
      borderColor: '#c4c4c4',
    },
    secondary: {
      main: '#4AE6C1',
      contrastText: '#ffffff',
      borderColor: '#c4c4c4',
    },
    accent: {
      main: '#0e7fb7'
    },
    error: {
      main: red.A400,
    },
    background: {
      default: '#eceff1',
    },
    company: {
      default: '#0e7fb7'
    },
  },
  navigation: {
    primary: {
      main: '#ffffff',

      contrastText: '#000000',
      borderColor: '#c4c4c4',
    },
    background: {
      main: '#0e7fb7',
      secondary: '#e3f2fa'
    },
    text: {
      main: '#ffffff',
      secondary: '#0e7fb7'
    }
  },
  overrides: {
    MuiTooltip: {
      tooltip: {
        fontSize: 13
      }
    }
  }
});
