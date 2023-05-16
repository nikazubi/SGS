import { useMutation, useQueryClient } from "react-query";

const setQueryData = (oldData, newData, id) => {
  if (Array.isArray(oldData)) {
    return oldData.map(old => old[id] === newData[id] ? newData : old);
  } else {
    if (!!oldData.content) {
      return {
        ...oldData,
        content: oldData.content.map(old => old[id] === newData[id] ? newData : old)
      };
    } else {
      return newData;
    }
  }
}

const useMutationWithOptimisticUpdate = (query, queryKeys, id = "id") => {
  const queryClient = useQueryClient();

  return useMutation(query, {
    onMutate: async (data) => {
      await queryClient.cancelQueries(queryKeys)
      const previousData = queryClient.getQueryData(queryKeys)
      queryClient.setQueriesData(queryKeys, (oldData) => setQueryData(oldData, data, id));
      return { previousData }
    },
    onSuccess: (data) => {
      queryClient.setQueriesData(queryKeys, (oldData) => setQueryData(oldData, data, id));
    },
    onError: (err, variables, context) => {
      queryClient.setQueriesData(queryKeys, () => context.previousData)
    },
    // onSettled: () => {
    //   queryClient.invalidateQueries(queryKeys)
    // },
  });
};

export default useMutationWithOptimisticUpdate;