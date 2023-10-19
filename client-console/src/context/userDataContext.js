import {createContext, useContext, useState} from "react";
import {HashRouter} from "react-router-dom";
import {UserContextProvider} from "./user-context";
import {NotificationProvider} from "./notification-context";
import {BackdropProvider} from "./backdrop-context";
import {ConvertErrorProvider} from "./convertError";
import {ReactQueryProvider} from "./react-query-context";

const MyContext = createContext();
const MyContext2 = createContext();


export const useUserData = () => useContext(MyContext);

export const useUpdate = () => {
    return useContext(MyContext2)
}


const UserDataProvider = ({children}) => {

    const [fetchStudent, setFetchStudent] = useState([])

    const fetchUser = (data) => {
        setFetchStudent(data)
    }

    return (
        <HashRouter>
            <ReactQueryProvider>
                <ConvertErrorProvider>
                    <UserContextProvider>
                        {/*<InitialDataProvider>*/}
                        {/*<NavigationProvider>*/}
                        <NotificationProvider>
                            <BackdropProvider>
                                {/*<MuiThemeProvider theme={theme}>*/}
                                {children}
                                {/*</MuiThemeProvider>*/}
                            </BackdropProvider>
                        </NotificationProvider>
                        {/*</NavigationProvider>*/}
                        {/*</InitialDataProvider>*/}
                    </UserContextProvider>
                </ConvertErrorProvider>
            </ReactQueryProvider>
        </HashRouter>
    );
};

export default UserDataProvider;