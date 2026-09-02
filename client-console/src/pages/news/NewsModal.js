import React, {useEffect} from "react";
import NewsImage from "./NewsImage";

/**
 * One news item in full.
 *
 * The body is the school's own HTML, sanitised on write against a fixed
 * allowlist (decision 84) rather than here — sanitising on read would leave the
 * unsafe original in the database for whatever renders it next.
 *
 * Closes on Escape and on the backdrop, which is what every dialog on the web
 * does and what a parent will try first.
 */
const NewsModal = ({item, onClose}) => {

    useEffect(() => {
        if (!item) {
            return undefined;
        }
        const onKey = (e) => {
            if (e.key === "Escape") {
                onClose();
            }
        };
        window.addEventListener("keydown", onKey);

        // The page behind must not scroll while the dialog is open, or a phone
        // scrolls the list instead of the article.
        const previous = document.body.style.overflow;
        document.body.style.overflow = "hidden";

        return () => {
            window.removeEventListener("keydown", onKey);
            document.body.style.overflow = previous;
        };
    }, [item, onClose]);

    if (!item) {
        return null;
    }

    return (
        <div className="news__backdrop" onClick={onClose}>
            {/* Stops a click inside the dialog from reaching the backdrop. */}
            <div className="news__dialog" onClick={(e) => e.stopPropagation()}>
                <button type="button" className="news__close" onClick={onClose}
                        aria-label="დახურვა">×
                </button>

                {item.imageUuid ? (
                    <NewsImage uuid={item.imageUuid} className="news__hero"/>
                ) : null}

                <div className="news__dialogBody">
                    <div className="news__head">
                        <h2 className="news__dialogTitle">{item.title}</h2>
                        {item.date ? (
                            <span className="news__date">{formatDate(item.date)}</span>
                        ) : null}
                    </div>

                    {item.categoryName ? (
                        <span className="news__tag">{item.categoryName}</span>
                    ) : null}

                    <div className="news__full"
                         dangerouslySetInnerHTML={{__html: item.bodyHtml}}/>

                    {item.links.length > 0 ? (
                        <ul className="news__links">
                            {item.links.map((link, i) => (
                                <li key={i}>
                                    <a href={link.url} target="_blank"
                                       rel="noopener noreferrer">
                                        {link.label || link.url}
                                    </a>
                                </li>
                            ))}
                        </ul>
                    ) : null}
                </div>
            </div>
        </div>
    );
};

const formatDate = (iso) => {
    const [y, m, d] = iso.split("-");
    return `${d}.${m}.${y}`;
};

export default NewsModal;
