import {createContext, useCallback, useContext} from "react";

const ConvertErrorContext = createContext(null);

export const useConvertError = () => {
    const context = useContext(ConvertErrorContext);
    if (!context) {
        throw new Error('You forgot to wrap your component with ConvertErrorProvider');
    }

    return context;
};

export const ConvertErrorProvider = props => {
    const convert = useCallback((error, includeStatus = true) => {
        return convertError(error, includeStatus);
    }, []);
    return <ConvertErrorContext.Provider value={{convert}} {...props} />;
};


const convertError = (error, includeStatus) => {
    const response = error?.response;
    if (!response) {
        return;
    }

    const status = response.status ? response.status.toString() : null;
    const statusMessage = !!status ? (status) : "";
    let errorMessage = '';
    if (!!response.data) {
        if (Array.isArray(response.data)) {
            errorMessage = response.data.map(convertErrorToMessage())
                .filter(text => !!text)
                .reduce((curr, now) => curr + now + ', ', '');
            errorMessage = errorMessage.length === 0 ? errorMessage : errorMessage.substr(0, errorMessage.length - 2);
        } else {
            errorMessage = convertErrorToMessage()(response.data);
        }
    }
    return {
        message: includeStatus ? `${statusMessage}, ${errorMessage}` : errorMessage,
        statusCode: response?.status ?? 500
    };
};


const convertErrorToMessage = () => {
    return error => {
        if (error.message.includes(" ")) return error.message
        const messageKey = `${error.message}`;
        // if (!!error.row) {
        //   return t("spreadSheetException", {message: t(messageKey), row: error.row});
        // } else if (!!error.field) {
        //   return t(`${error.message}`, error.arguments);
        // } else {
        return (messageKey);
        // }
    };
};