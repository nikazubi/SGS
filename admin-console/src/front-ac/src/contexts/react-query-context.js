import {QueryClient, QueryClientProvider} from 'react-query'
import React from 'react'
import { ReactQueryDevtools } from 'react-query/devtools'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000,
      //todo set to true in production
      refetchOnWindowFocus: false,
      notifyOnChangeProps: 'tracked',
      //todo check validity
      // structuralSharing: false
    }
  }
});

export const ReactQueryProvider = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    {children}
    <ReactQueryDevtools initialIsOpen={true} />
  </QueryClientProvider>
)
