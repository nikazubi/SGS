import {Link} from "react-router-dom";
import {useQuery} from "react-query";
import {fetchNews} from "../pages/journal/parentApi";
import NewsImage from "../pages/news/NewsImage";
import {excerpt} from "../pages/news/NewsPage";
import "./newsTile.css";

/**
 * The latest published news item, as a tile on a landing page.
 *
 * Shared by both landing pages rather than written twice. The two look
 * different — a light tinted card on primary, the navy box everywhere else —
 * but the part worth not duplicating is none of that: it is the four states.
 * Loading, failed, empty and an actual item have to stay distinguishable, and
 * the parent console has already shipped a screen that drew a broken request as
 * an empty one (TEST-RESULTS.md #9). One implementation is how that stays fixed
 * in both places at once.
 *
 * Colour comes from the caller through `className` and `style`; everything here
 * is structure and the four states.
 *
 * @param placeholderIcon shown when the item has no picture, or there is no item
 */
const NewsTile = ({className = "", style, placeholderIcon}) => {

    const {data, isLoading, isError} = useQuery(
        ["PARENT_NEWS_LATEST"], () => fetchNews({page: 0, size: 1}),
        {refetchOnWindowFocus: false});

    const item = (data?.items || [])[0];

    return (
        <Link to="/news" className={`newsTile ${className}`} style={style}>
            <div className="newsTile__thumb">
                {item?.imageUuid
                    ? <NewsImage uuid={item.imageUuid} className="newsTile__img"/>
                    : placeholderIcon
                        ? <img src={placeholderIcon} alt="" className="newsTile__placeholder"/>
                        : null}
            </div>

            <div className="newsTile__body">
                <span className="newsTile__eyebrow">სიახლეები</span>

                {isLoading ? (
                    <span className="newsTile__state">იტვირთება…</span>
                ) : isError ? (
                    <span className="newsTile__state newsTile__state--error">
                        ჩატვირთვა ვერ მოხერხდა
                    </span>
                ) : !item ? (
                    <span className="newsTile__state">სიახლეები ჯერ არ არის</span>
                ) : (
                    <>
                        <h3 className="newsTile__title">{item.title}</h3>
                        <p className="newsTile__excerpt">{excerpt(item.bodyHtml, 110)}</p>
                    </>
                )}

                <div className="newsTile__foot">
                    <span>{item?.date ? formatDate(item.date) : ""}</span>
                    <span className="newsTile__seeAll">ყველა</span>
                </div>
            </div>
        </Link>
    );
};

/** 2026-03-12 -> 12.03.2026 */
const formatDate = (iso) => {
    const [y, m, d] = iso.split("-");
    return `${d}.${m}.${y}`;
};

export default NewsTile;
