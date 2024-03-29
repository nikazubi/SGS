import React, {createContext, useCallback, useContext, useMemo, useState} from 'react';
import {useConvertError} from "./convertError";

const NotificationContext = createContext(null);

export const useNotification = () => {
    const context = useContext(NotificationContext);

    if (!context) {
        throw new Error('You forgot to wrap your component with NotificationProvider');
    }

    return context;
};

export const NotificationProvider = (props) => {
    const [notification, setNotification] = useState(null);
    const [error, setError] = useState(null);
    const [open, setOpen] = useState(false);
    const {convert} = useConvertError();

    const removeNotification = useCallback(() => {
        setNotification(null);
        setOpen(false);
    }, [setNotification, setOpen]);

    const makeNotification = useCallback(notif => {
        setNotification(notif);
        setOpen(true);
    }, [setNotification, setOpen]);

    const setErrorMessage = useCallback((error, includeStatus = true, needsConvert = true) => {
        setError(error)
        let message = error;
        let statusCode = "500";
        if (needsConvert) {
            const converted = convert(error, includeStatus);
            message = converted?.message;
            statusCode = converted?.statusCode;
        }
        makeNotification({message, statusCode, severity: 'error'});
    }, [makeNotification, convert]);


    const value = useMemo(
        () => ({notification, open, removeNotification, setNotification: makeNotification, setErrorMessage}),
        [notification, removeNotification, makeNotification, open, setErrorMessage]
    );

    return <NotificationContext.Provider value={value} {...props}/>;
};
