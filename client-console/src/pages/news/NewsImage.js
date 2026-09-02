import React, {useEffect, useState} from "react";
import axios from "../utils/axios";

/**
 * A news picture.
 *
 * Fetched as a blob rather than set as a plain {@code src}, because the endpoint
 * is behind the parent token and an {@code <img>} tag sends no Authorization
 * header — the browser would request it anonymously and get a 401.
 *
 * The object URL is revoked on unmount and whenever the uuid changes. Without
 * that, scrolling a long news list leaks one blob per picture for as long as the
 * tab is open.
 */
const NewsImage = ({uuid, className}) => {

    const [url, setUrl] = useState(null);

    useEffect(() => {
        if (!uuid) {
            setUrl(null);
            return undefined;
        }
        let revoked = false;
        let created = null;

        axios.get(`/api/parent/news/images/${uuid}`, {responseType: "blob"})
            .then(response => {
                // The component may have unmounted while this was in flight.
                if (revoked) {
                    return;
                }
                created = window.URL.createObjectURL(response.data);
                setUrl(created);
            })
            .catch(() => setUrl(null));

        return () => {
            revoked = true;
            if (created) {
                window.URL.revokeObjectURL(created);
            }
        };
    }, [uuid]);

    if (!url) {
        // Holds the space so the text column does not jump when the picture
        // arrives, and so an item without one still lines up.
        return <div className={`${className} news__thumb--none`}/>;
    }
    return <img className={className} src={url} alt=""/>;
};

export default NewsImage;
