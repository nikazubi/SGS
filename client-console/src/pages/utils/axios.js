import axios from 'axios';
import secureLocalStorage from "react-secure-storage";

const axiosInstance = axios.create({
    baseURL: process.env.REACT_APP_BACKEND_BASE_URL,
    headers: {
        'X-Requested-With': 'XMLHttpRequest',
    }
});

// The backend issues a single JWT and has no refresh-token flow, so when the token is
// rejected (401) we simply clear the session and send the user back to the login screen.
const isAuthRequest = (url = '') => url.includes('authenticate');

const forceLogout = () => {
    secureLocalStorage.clear();
    window.location.href = window.location.origin;
};

axiosInstance.interceptors.request.use(
    config => {
        const jwt = secureLocalStorage.getItem("jwtToken");
        if (jwt && !config.headers.authorization) {
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
            forceLogout();
        }
        return Promise.reject(error);
    }
);

export default axiosInstance;
