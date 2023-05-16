import axios from 'axios';
import { deleteAuth, getAccessToken, getRefreshToken, setAuth } from "../utils/auth";
import { ENDPOINT_AUTHENTICATION_REFRESH } from "../constants/endpoints";

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
  deleteAuth();
  processQueue(error, null);
}

const onError = async (error) => {
  const originalRequest = error.config;
  if (error.response.status === 401) {

    const refreshToken = getRefreshToken();

    if (originalRequest.url === ENDPOINT_AUTHENTICATION_REFRESH || !refreshToken) {
      await logout(error);
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return createPromise(originalRequest);
    }

    try {
      isRefreshing = true;
      const {data} = await customAxios.get(ENDPOINT_AUTHENTICATION_REFRESH, {
        headers: {
          authorization: `Bearer ${refreshToken}`
        }
      });
      if (!data.accessToken || !data.refreshToken) {
        await logout(error);
        return Promise.reject(error);
      }

      setAuth(data);
      const token = data.accessToken;
      originalRequest.headers.authorization = `Bearer ${token}`;
      processQueue(null, data.accessToken);
      return customAxios(originalRequest);
    } catch (error) {
      await logout(error);
      return Promise.reject(error);
    }
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
    const token = getAccessToken();

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