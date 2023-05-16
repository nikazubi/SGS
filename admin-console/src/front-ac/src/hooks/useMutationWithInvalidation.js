import { useMutation, useQueryClient } from "react-query";


const useMutationWithInvalidation = (query, queryKeys) => {
  const queryClient = useQueryClient();

  return useMutation(query, {
    onSuccess: () => {
      queryClient.invalidateQueries(queryKeys);
    },
  });
};

export default useMutationWithInvalidation;