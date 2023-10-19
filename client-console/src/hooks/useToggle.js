import {useCallback, useMemo, useState} from 'react';

export const useToggle = (initValue = false) => {
    const [open, setOpen] = useState(initValue);

    const handleClose = useCallback(() => {
        setOpen(false);
    }, [setOpen]);

    const handleOpen = useCallback(() => {
        setOpen(true);
    }, [setOpen]);

    return useMemo(() => ({open, handleOpen, handleClose}), [open, handleOpen, handleClose]);
};
