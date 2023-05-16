import axios from "../utils/axios";
import { ENDPOINT_AUTHENTICATION_REFRESH } from "../constants/endpoints";
import { getRefreshToken } from "../utils/auth";
import { useQueryWithoutCache } from "./useQueryWithoutCache";

export const fetchLoggedInUser = async () => {
  try {
    const {data} = await axios.get(ENDPOINT_AUTHENTICATION_REFRESH, {
      headers: {
        authorization: `Bearer ${getRefreshToken()}`
      }
    });
    return data;
  } catch (e) {
    if ([401, 403].includes(e.response.status)) {
      return null;
    } else {
      throw e;
    }
  }
};

export const useLoggedInUser = () => useQueryWithoutCache("loggedInUser", fetchLoggedInUser);