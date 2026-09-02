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
      {/* Closed by default: the open panel sits over the bottom-left of every
        page, which in a 1600-wide window covers the login button. */}
      <ReactQueryDevtools initialIsOpen={false}/>
  </QueryClientProvider>
)
