import axios from "../../../utils/axios";
import { ENDPOINT_FILTER } from "../../../constants/endpoints";

export const filterHttpPost = async ({queryKey}) => {
  const [key, filtersData, pageParams] = queryKey;
  const { data } = await axios.post(ENDPOINT_FILTER, {...filtersData, filters: filtersData.filters || [] }, {
    params: {
      ...pageParams,
      filterType: key
    }
  });
  return data;
};
