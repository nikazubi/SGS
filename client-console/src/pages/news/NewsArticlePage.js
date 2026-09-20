import React from "react";
import {Link, useLocation, useParams} from "react-router-dom";
import {useQuery} from "react-query";
import {fetchNewsCategories, fetchNewsItem, fetchParentModules} from "../journal/parentApi";
import NewsImage from "./NewsImage";
import {isPrimary, newsColour} from "./newsTheme";
import {ClockIcon, formatDate} from "./NewsPage";
import "./news.css";

/**
 * One news item, in full, at its own address.
 *
 * A page rather than the dialog this replaces. A dialog was fine on a desktop
 * and poor on a phone - a long article in a panel with the list scrolling
 * behind it - and it could not be linked to, bookmarked, or reached by the
 * browser's back button. Those three are the whole reason this is a route.
 *
 * The list's page and filter ride in the query string, so the back link returns
 * a parent to the page they left rather than to the top of the news.
 *
 * The body is the school's own HTML, sanitised **on write** against a fixed
 * allowlist (decision 84). Sanitising here instead would leave the unsafe
 * original in the database for whatever renders it next.
 */
const NewsArticlePage = () => {

    const {uuid} = useParams();
    const {search} = useLocation();

    const {data: modules} = useQuery(
        ["PARENT_MODULES"], fetchParentModules, {refetchOnWindowFocus: false});
    const {data: categories} = useQuery(
        ["PARENT_NEWS_CATEGORIES"], fetchNewsCategories, {refetchOnWindowFocus: false});

    const {data: item, isLoading, isError} = useQuery(
        ["PARENT_NEWS_ITEM", uuid], () => fetchNewsItem(uuid),
        {refetchOnWindowFocus: false});

    const colour = newsColour(isPrimary(modules), item?.categoryName,
        (categories || []).map(c => c.name));

    return (
        <div className="news news--article">
            {/* Back before anything else, so it is where a thumb reaches on a
                phone and does not move when the article is slow. */}
            <Link className="news__back" to={`/news${search || ""}`}>← სიახლეები</Link>

            {isLoading ? (
                <p className="news__state">იტვირთება…</p>
            ) : isError ? (
                <p className="news__state news__state--error">ვერ ჩაიტვირთა.</p>
            ) : !item ? (
                <p className="news__state">სიახლე ვერ მოიძებნა.</p>
            ) : (
                <article className="news__article"
                         style={{background: colour.tile, color: colour.label}}>
                    {item.imageUuid ? (
                        <NewsImage uuid={item.imageUuid} className="news__hero"/>
                    ) : null}

                    <div className="news__articleBody">
                        <h1 className="news__articleTitle">{item.title}</h1>

                        <div className="news__articleMeta">
                            {item.date ? (
                                <span className="news__date">
                                    <ClockIcon/> {formatDate(item.date)}
                                </span>
                            ) : null}
                            {item.categoryName ? (
                                <span className="news__tag"
                                      style={{borderColor: colour.accent}}>
                                    {item.categoryName}
                                </span>
                            ) : null}
                        </div>

                        <div className="news__full"
                             dangerouslySetInnerHTML={{__html: item.bodyHtml}}/>

                        {(item.links || []).length > 0 ? (
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
                </article>
            )}
        </div>
    );
};

export default NewsArticlePage;
