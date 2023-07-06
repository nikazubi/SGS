import { createContext, useContext, useState } from "react";

const MyContext = createContext();
const MyContext2 = createContext();


export const useUserData = () => useContext(MyContext);

export const useUpdate = () => {
    return useContext(MyContext2)
}


const UserDataProvider = ({ children }) => {
  
  const [fetchStudent, setFetchStudent] = useState([])

  const fetchUser = (data) => {
    setFetchStudent(data)
  }

  return (
    <MyContext.Provider value={fetchStudent}>
      <MyContext2.Provider value={fetchUser}>
        {children}
      </MyContext2.Provider>
    </MyContext.Provider>
  );
};

export default UserDataProvider;