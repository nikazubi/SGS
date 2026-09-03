import axios from 'axios';
import secureLocalStorage from "react-secure-storage";

const customAxios = axios.create({
    baseURL: process.env.REACT_APP_BACKEND_BASE_URL,
    headers: {
        'X-Requested-With': 'XMLHttpRequest',
    }
});

// Single-JWT backend, no refresh token: clear the session and return to login on 401.
const isAuthRequest = (url = '') => url.includes('authenticate');

const forceLogout = () => {
    secureLocalStorage.clear();
    window.location.href = window.location.origin;
};

const useAxios = () => {
    customAxios.interceptors.response.use(
        response => response,
        error => {
            if (error.response?.status === 401 && !isAuthRequest(error.config?.url)) {
                forceLogout();
            }
            return Promise.reject(error);
        }
    );

    customAxios.interceptors.request.use(async config => {
        const token = secureLocalStorage.getItem("jwtToken");
        if (token && !config.headers.Authorization) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    }, error => Promise.reject(error));

    return {
        axios: customAxios,
    };
};

export default useAxios;
