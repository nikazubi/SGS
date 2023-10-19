import axios from 'axios';
import secureLocalStorage from "react-secure-storage";

const customAxios = axios.create({
    baseURL: process.env.REACT_APP_BACKEND_BASE_URL,
    headers: {
        'X-Requested-With': 'XMLHttpRequest',
    }
});

let isRefreshing = false;
let needRefresh = [];

const processQueue = (error, token = null) => {
    needRefresh.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    needRefresh = [];
    isRefreshing = false;
};

const createPromise = (originalRequest) => {
    return new Promise(function (resolve, reject) {
        needRefresh.push({resolve, reject});
    })
        .then(token => {
            originalRequest.headers.authorization = `Bearer ${token}`;
            return customAxios(originalRequest);
        })
        .catch(err => {
            return Promise.reject(err);
        });
};

const logout = (error) => {
    // secureLocalStorage.clear();
    // processQueue(error, null);
}

const onError = async (error) => {
    if (error.response.status === 401) {
        await logout(error);
        return Promise.reject(error);
    }
    return Promise.reject(error);
};

//TODO does not refresh within useMutation
const useAxios = () => {
    customAxios.interceptors.response.use(
        response => response,
        error => onError(error)
    );

    customAxios.interceptors.request.use(async config => {
        const token = secureLocalStorage.getItem("jwtToken");

        if (token && !config.headers.Authorization) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    }, (error) => Promise.reject(error));

    return {
        axios: customAxios,
    };
};
export default useAxios;