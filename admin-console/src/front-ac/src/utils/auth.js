const authKey = 'auth';

export const getAuth = () => {
    const authJson = localStorage.getItem(authKey);
    if (!authJson) {
        return null;
    }
    return JSON.parse(authJson);
};

export const getAccessToken = () => {
    return "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuaWthIiwic2NwIjoiIiwiaWF0IjoxNjg2MTQ5MzY1LCJleHAiOjE2ODYxNjczNjV9.1-bQaSOdjMFsRH5xnm2bWElRlniopuLTWvhsQgfFsFZNNE6XgV-x2Yztx9ZrBpsTBJRp-2_MUkXFzFMlddACXA"
   // return getAuth()?.accessToken;
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