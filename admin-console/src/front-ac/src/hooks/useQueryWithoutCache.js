import { useQuery } from "react-query";

export const useQueryWithoutCache = (queryKey, queryFn, params) => {
  return useQuery(queryKey, queryFn, {
    cacheTime: 0,
    staleTime: 0,
    ...params
  });
};