const authKey = 'auth';

export const getAuth = () => {
    const authJson = localStorage.getItem(authKey);
    if (!authJson) {
        return null;
    }
    return JSON.parse(authJson);
};

export const getAccessToken = () => {
    return getAuth()?.accessToken;
};

export const getRefreshToken = () => {
    return getAuth()?.refreshToken;
};

export const setAuth = (auth) => {
    localStorage.setItem(authKey, JSON.stringify(auth));
};

export const deleteAuth = () => {
    localStorage.removeItem(authKey);
};