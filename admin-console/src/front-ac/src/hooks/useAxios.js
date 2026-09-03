import axios from 'axios';
import { deleteAuth, getAccessToken } from "../utils/auth";

const customAxios = axios.create({
    baseURL: process.env.REACT_APP_BACKEND_BASE_URL,
    headers: {
        'X-Requested-With': 'XMLHttpRequest',
    }
});

// Single-JWT backend, no refresh token: clear the stored auth and return to login on 401.
const isAuthRequest = (url = '') => url.includes('authenticate');

const logout = () => {
    deleteAuth();
    window.location.href = window.location.origin;
};

const useAxios = () => {
    customAxios.interceptors.response.use(
        response => response,
        error => {
            if (error.response?.status === 401 && !isAuthRequest(error.config?.url)) {
                logout();
            }
            return Promise.reject(error);
        }
    );

    customAxios.interceptors.request.use(async config => {
        const token = getAccessToken();
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
