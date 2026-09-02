import React, {useEffect, useState} from "react";
import {Image} from "@mui/icons-material";
import {fetchImageObjectUrl} from "./contentApi";

/**
 * A news picture, or the placeholder where there is none.
 *
 * The bytes come through axios rather than from an <img src>: the endpoint is
 * behind MANAGE_NEWS and auth is a bearer token added by an interceptor, so a
 * direct src would come back 403.
 *
 * The object URL is revoked on unmount and whenever the uuid changes. Without
 * that, scrolling a news grid leaks a blob per row per render.
 */
const NewsImage = ({uuid, size = 64}) => {

    const [src, setSrc] = useState(null);

    useEffect(() => {
        let cancelled = false;
        let created = null;

        if (uuid) {
            fetchImageObjectUrl(uuid).then(url => {
                if (cancelled) {
                    // Unmounted while the request was in flight; nothing will
                    // revoke this unless we do it here.
                    if (url) window.URL.revokeObjectURL(url);
                    return;
                }
                created = url;
                setSrc(url);
            }).catch(() => {
                if (!cancelled) setSrc(null);
            });
        } else {
            setSrc(null);
        }

        return () => {
            cancelled = true;
            if (created) window.URL.revokeObjectURL(created);
        };
    }, [uuid]);

    const box = {
        width: size, height: size, borderRadius: 4, objectFit: "cover",
        display: "flex", alignItems: "center", justifyContent: "center",
        backgroundColor: "#eceff1", color: "#90a4ae", flexShrink: 0
    };

    if (!src) {
        // The school's grid shows a placeholder where an item has no picture,
        // so no picture is an expected state rather than a broken one.
        return <div style={box}><Image fontSize="small"/></div>;
    }
    return <img src={src} alt="" style={box}/>;
};

export default NewsImage;
