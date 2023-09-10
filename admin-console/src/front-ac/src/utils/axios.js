import axios from 'axios';
import { deleteAuth, getAccessToken, getRefreshToken, setAuth } from "./auth";

const axiosInstance = axios.create({
    baseURL: process.env.REACT_APP_BACKEND_BASE_URL,
  // baseURL: 'http://195.69.143.211:8080/sgs-core',
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
      return axiosInstance(originalRequest);
    })
    .catch(err => {
      return Promise.reject(err);
    });
};

const logout = (error, refreshToken) => {
  // if(!refreshToken){
  //   processQueue(error, null);
  //   return
  // }
  deleteAuth();
  processQueue(error, null);
  window.location.href = window.location.origin;
}

const onError = async (error) => {
  const originalRequest = error.config;
  if (error.response.status === 401) {
    // const refreshToken = getRefreshToken();
    //
    // if (originalRequest.url === ENDPOINT_AUTHENTICATION_REFRESH || !refreshToken) {
    //   await logout(error, refreshToken);
    //   return Promise.reject(error);
    // }
    //
    // if (isRefreshing) {
    //   return createPromise(originalRequest);
    // }
    //
    // try {
    //   isRefreshing = true;
    //   const {data} = await axiosInstance.get(ENDPOINT_AUTHENTICATION_REFRESH, {
    //     headers: {
    //       authorization: `Bearer ${refreshToken}`
    //     }
    //   });
    //   if (!data.accessToken || !data.refreshToken) {
    //     await logout(error);
    //     return Promise.reject(error);
    //   }
    //
    //   setAuth(data);
    //   const token = data.accessToken;
    //   originalRequest.headers.authorization = `Bearer ${token}`;
    //   processQueue(null, data.accessToken);
    //   return axiosInstance(originalRequest);
    // } catch (error) {
      await logout(error);
      return Promise.reject(error);

  } else {
      return Promise.reject(error);
  }
};

axiosInstance.interceptors.request.use(
  config => {
      const jwt = getAccessToken();
    if (!!jwt  && !config.headers.authorization) {
      config.headers.authorization = `Bearer ${jwt}`;
    }
    return config;
  },
  error =>  error
);

axiosInstance.interceptors.response.use(
  response => response,
  error => onError(error)
);

export default axiosInstance;
