import axios from 'axios';
import { deleteAuth, getAccessToken } from "./auth";

const axiosInstance = axios.create({
    baseURL: process.env.REACT_APP_BACKEND_BASE_URL,
    headers: {
        'X-Requested-With': 'XMLHttpRequest',
    }
});

// The backend issues a single JWT with no refresh-token flow, so on 401 we clear the
// stored auth and send the user back to the login screen.
const isAuthRequest = (url = '') => url.includes('authenticate');

const logout = () => {
    deleteAuth();
    window.location.href = window.location.origin;
};

axiosInstance.interceptors.request.use(
    config => {
        const jwt = getAccessToken();
        if (!!jwt && !config.headers.authorization) {
            config.headers.authorization = `Bearer ${jwt}`;
        }
        return config;
    },
    error => Promise.reject(error)
);

axiosInstance.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 401 && !isAuthRequest(error.config?.url)) {
            logout();
        }
        return Promise.reject(error);
    }
);

export default axiosInstance;
