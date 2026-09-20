import React from "react";
import {Link, useHistory, useLocation} from "react-router-dom";
import {useQuery} from "react-query";
import {fetchNews, fetchNewsCategories, fetchParentModules} from "../journal/parentApi";
import NewsImage from "./NewsImage";
import {isPrimary, newsColour} from "./newsTheme";
import "./news.css";

const PAGE_SIZE = 10;

/**
 * The school's news, newest first.
 *
 * Cards in the shape of the weekly ones - a picture, a title, a plain-text
 * preview and a way in - with the whole item on its own page rather than in a
 * dialog. A long article in a modal is awkward on a phone, and an article is a
 * thing a parent may want to send to the other parent.
 *
 * **Which page and which filter live in the address**, so going back from an
 * article returns to the page you left rather than to the first one, and so a
 * reload does not quietly move you.
 *
 * The preview is stripped to text on purpose: rendering the school's HTML into
 * a card would let one item's heading resize every other card.
 */
const NewsPage = () => {

    const history = useHistory();
    const {search} = useLocation();
    const params = new URLSearchParams(search);
    const page = Math.max(0, Number(params.get("page") || 0));
    const categoryId = params.get("category") ? Number(params.get("category")) : null;

    const {data: modules} = useQuery(
        ["PARENT_MODULES"], fetchParentModules, {refetchOnWindowFocus: false});

    const {data: categories} = useQuery(
        ["PARENT_NEWS_CATEGORIES"], fetchNewsCategories, {refetchOnWindowFocus: false});

    const {data, isLoading, isError} = useQuery(
        ["PARENT_NEWS", categoryId, page],
        () => fetchNews({categoryId, page, size: PAGE_SIZE}),
        {keepPreviousData: true, refetchOnWindowFocus: false});

    const items = data?.items || [];
    const lastPage = Math.max(0, Math.ceil((data?.total || 0) / PAGE_SIZE) - 1);
    const primary = isPrimary(modules);
    // The filter's own list, which the server returns in a stable order, is what
    // gives each category its colour. Two screens reading the same order means
    // the chip and the card cannot disagree.
    const order = (categories || []).map(c => c.name);

    const go = (next) => history.push(`/news?${next.toString()}`);

    const setPage = (n) => {
        const next = new URLSearchParams(search);
        next.set("page", String(n));
        go(next);
    };

    const setCategory = (id) => {
        const next = new URLSearchParams(search);
        if (id === null) {
            next.delete("category");
        } else {
            next.set("category", String(id));
        }
        // A new filter is a new list, so the page number from the old one means
        // nothing and would land on an empty page.
        next.delete("page");
        go(next);
    };

    return (
        <div className="news">
            <h1 className="news__pageTitle">სიახლეები</h1>

            {(categories || []).length > 0 ? (
                <div className="news__filter">
                    <button type="button" onClick={() => setCategory(null)}
                            className={`news__chip${categoryId === null ? " news__chip--on" : ""}`}>
                        ყველა
                    </button>
                    {categories.map(category => (
                        <button type="button" key={category.id}
                                onClick={() => setCategory(category.id)}
                                className={`news__chip${
                                    categoryId === category.id ? " news__chip--on" : ""}`}>
                            {category.name}
                        </button>
                    ))}
                </div>
            ) : null}

            {/* Loading, empty and failed stay apart: a parent told "no news"
                when the request broke will not look again. */}
            {isLoading ? (
                <p className="news__state">იტვირთება…</p>
            ) : isError ? (
                <p className="news__state news__state--error">ვერ ჩაიტვირთა.</p>
            ) : items.length === 0 ? (
                <p className="news__state">სიახლეები ჯერ არ არის.</p>
            ) : (
                <div className="news__list">
                    {items.map(item => {
                        const colour = newsColour(primary, item.categoryName, order);
                        return (
                            <article key={item.uuid} className="news__card"
                                     style={{background: colour.tile, color: colour.label}}>
                                <div className="news__thumbBox">
                                    {item.imageUuid ? (
                                        <NewsImage uuid={item.imageUuid} className="news__thumb"/>
                                    ) : null}
                                </div>

                                <div className="news__body">
                                    <div className="news__head">
                                        <h2 className="news__title">{item.title}</h2>
                                        {item.date ? (
                                            <span className="news__date">
                                                <ClockIcon/> {formatDate(item.date)}
                                            </span>
                                        ) : null}
                                    </div>

                                    {item.categoryName ? (
                                        <span className="news__tag"
                                              style={{borderColor: colour.accent}}>
                                            {item.categoryName}
                                        </span>
                                    ) : null}

                                    <p className="news__excerpt">{excerpt(item.bodyHtml)}</p>

                                    {/* The whole card is not a link: the body may
                                        hold one, and a link inside a link is not
                                        something a browser can represent. */}
                                    <Link className="news__more"
                                          style={{color: colour.label}}
                                          to={`/news/${item.uuid}?${params.toString()}`}>
                                        ვრცლად →
                                    </Link>
                                </div>
                            </article>
                        );
                    })}
                </div>
            )}

            {lastPage > 0 ? (
                <div className="news__pager">
                    <button type="button" disabled={page === 0}
                            onClick={() => setPage(page - 1)} aria-label="წინა">‹
                    </button>
                    <span>{page + 1} / {lastPage + 1}</span>
                    <button type="button" disabled={page >= lastPage}
                            onClick={() => setPage(page + 1)} aria-label="შემდეგი">›
                    </button>
                </div>
            ) : null}
        </div>
    );
};

/**
 * A plain-text preview of rich text.
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
        text = new DOMParser().parseFromString(html, "text/html").body.textContent || "";
    } catch (e) {
        text = html;
    }
    text = text.replace(/\s+/g, " ").trim();
    return text.length > limit ? `${text.slice(0, limit).trimEnd()}…` : text;
};

/** 2026-03-12 -> 12.03.2026 */
export const formatDate = (iso) => {
    const [y, m, d] = iso.split("-");
    return `${d}.${m}.${y}`;
};

export const ClockIcon = () => (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" strokeWidth="2" aria-hidden="true">
        <circle cx="12" cy="12" r="9"/>
        <polyline points="12 7 12 12 15 14"/>
    </svg>
);

export default NewsPage;
