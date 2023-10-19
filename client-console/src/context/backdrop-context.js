import React, {createContext, useContext, useMemo} from 'react';
import {useToggle} from "../hooks/useToggle";

const BackdropContext = createContext(null);

export const useBackdrop = () => {
    const context = useContext(BackdropContext);

    if (!context) {
        throw new Error('You forgot to wrap your component with BackdropProvider');
    }

    return context;
};

export const BackdropProvider = (props) => {
    const {open, handleOpen, handleClose} = useToggle();

    const value = useMemo(
        () => ({isBackdropOpen: open, setBackdrop: handleOpen, removeBackdrop: handleClose}),
        [handleOpen, handleClose, open]
    );

    return <BackdropContext.Provider value={value} {...props}/>;
};
