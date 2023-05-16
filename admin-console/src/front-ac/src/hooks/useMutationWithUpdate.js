import { useMutation, useQueryClient } from "react-query";

const useMutationWithUpdate = (query, queryKeys, id = "id") => {
  const queryClient = useQueryClient();

  return useMutation(query, {
    onSuccess: (data) => {
      queryClient.setQueriesData(queryKeys, (oldData) => {
        if (Array.isArray(oldData)) {
          return oldData.map(old => old[id] === data[id] ? data : old);
        } else {
          if (!!oldData.content) {
            return {
              ...oldData,
              content: oldData.content.map(old => old[id] === data[id] ? data : old)
            };
          } else {
            return data;
          }
        }
      });
    },
  });
};

export default useMutationWithUpdate;