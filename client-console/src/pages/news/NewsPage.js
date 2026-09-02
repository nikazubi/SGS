import React, {useState} from "react";
import {useQuery} from "react-query";
import {fetchNews, fetchNewsCategories} from "../journal/parentApi";
import NewsImage from "./NewsImage";
import NewsModal from "./NewsModal";
import "./news.css";

const PAGE_SIZE = 10;

/**
 * School news, newest first.
 *
 * The ordinary shape of a news page: picture on the left, title and an excerpt
 * beside it, the date in the corner, and the full item in a dialog. The body
 * travels with the list rather than being fetched on open — an item is a few
 * paragraphs, and a request per click would fetch text the browser already had.
 *
 * Institution-wide. Every parent sees every item, which the school confirmed;
 * the category is a label to filter by, not a visibility rule.
 */
const NewsPage = () => {

    const [categoryId, setCategoryId] = useState(null);
    const [page, setPage] = useState(0);
    const [opened, setOpened] = useState(null);

    const {data: categories} = useQuery(
        ["PARENT_NEWS_CATEGORIES"], fetchNewsCategories,
        {refetchOnWindowFocus: false, staleTime: 5 * 60 * 1000});

    const {data, isLoading} = useQuery(
        ["PARENT_NEWS", categoryId, page],
        () => fetchNews({categoryId, page, size: PAGE_SIZE}),
        {keepPreviousData: true, refetchOnWindowFocus: false}
    );

    const total = data?.total || 0;
    const lastPage = Math.max(0, Math.ceil(total / PAGE_SIZE) - 1);

    return (
        <div className="ib__center column">
            <div className="pageName">სიახლეები</div>

            {(categories || []).length > 0 ? (
                <div className="news__filter">
                    <button
                        type="button"
                        className={`news__chip${categoryId === null ? " news__chip--on" : ""}`}
                        onClick={() => {
                            setCategoryId(null);
                            setPage(0);
                        }}
                    >
                        ყველა
                    </button>
                    {categories.map(category => (
                        <button
                            type="button"
                            key={category.id}
                            className={`news__chip${
                                categoryId === category.id ? " news__chip--on" : ""}`}
                            onClick={() => {
                                setCategoryId(category.id);
                                setPage(0);
                            }}
                        >
                            {category.name}
                        </button>
                    ))}
                </div>
            ) : null}

            <div className="news__list">
                {isLoading ? <div className="news__empty">…</div> : null}

                {!isLoading && (data?.items || []).length === 0 ? (
                    <div className="news__empty">სიახლეები ჯერ არ არის.</div>
                ) : null}

                {(data?.items || []).map(item => (
                    <article key={item.uuid} className="news__card"
                             onClick={() => setOpened(item)}>
                        {/* Keeps the space whether or not there is a picture,
                            so the text column does not jump. */}
                        <NewsImage uuid={item.imageUuid} className="news__thumb"/>

                        <div className="news__body">
                            <div className="news__head">
                                <h3 className="news__title">{item.title}</h3>
                                {item.date ? (
                                    <span className="news__date">
                                        <ClockIcon/> {formatDate(item.date)}
                                    </span>
                                ) : null}
                            </div>

                            {item.categoryName ? (
                                <span className="news__tag">{item.categoryName}</span>
                            ) : null}

                            <p className="news__excerpt">{excerpt(item.bodyHtml)}</p>

                            <button type="button" className="news__more">
                                ვრცლად
                            </button>
                        </div>
                    </article>
                ))}
            </div>

            {lastPage > 0 ? (
                <div className="news__pager">
                    <button type="button" disabled={page === 0}
                            onClick={() => setPage(p => p - 1)}>‹
                    </button>
                    <span>{page + 1} / {lastPage + 1}</span>
                    <button type="button" disabled={page >= lastPage}
                            onClick={() => setPage(p => p + 1)}>›
                    </button>
                </div>
            ) : null}

            <NewsModal item={opened} onClose={() => setOpened(null)}/>
        </div>
    );
};

/**
 * A plain-text preview of rich text.
 *
 * The body is HTML the school wrote. Rendering it into the card would let one
 * item's heading or list resize every other card, so the preview is stripped to
 * text and the markup is kept for the dialog.
 *
 * Parsed rather than regexed: a regex over HTML turns "&lt;p&gt;3 &lt; 5&lt;/p&gt;" into
 * nonsense, and DOMParser is in every browser this console supports.
 */
export const excerpt = (html, limit = 220) => {
    if (!html) {
        return "";
    }
    let text = "";
    try {
        text = new DOMParser().parseFromString(html, "text/html")
            .body.textContent || "";
    } catch (e) {
        text = html;
    }
    text = text.replace(/\s+/g, " ").trim();
    return text.length > limit ? `${text.slice(0, limit).trimEnd()}…` : text;
};

/** 2026-03-12 -> 12.03.2026 */
const formatDate = (iso) => {
    const [y, m, d] = iso.split("-");
    return `${d}.${m}.${y}`;
};

const ClockIcon = () => (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" strokeWidth="2" aria-hidden="true">
        <circle cx="12" cy="12" r="9"/>
        <polyline points="12 7 12 12 15 14"/>
    </svg>
);

export default NewsPage;
