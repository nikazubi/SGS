import React from "react";
import {ResponsiveContainer} from "recharts";

/**
 * The box every parent chart is drawn in.
 *
 * One place, because the charts that were here sized themselves in pixels —
 * one at a fixed 640, the legacy pair off `window.innerWidth - 460`. A number
 * computed from the window once at render is not a layout: it is wrong at every
 * width except the one it was measured at, and on a phone it is wrong by more
 * than the screen is wide.
 *
 * `ResponsiveContainer` with an aspect ratio instead. The chart is as wide as
 * its column and as tall as that implies, so it fits a phone and a desktop for
 * the same reason rather than by two sets of numbers.
 *
 * @param ratio width divided by height. Wider on a big screen, closer to square
 *              on a narrow one, because a 3:1 chart on a phone is a line.
 */
const ChartFrame = ({title, children, ratio = 2.4, minHeight = 200}) => (
    <figure className="journalChart">
        {title ? <figcaption className="journalChart__title">{title}</figcaption> : null}
        <div className="journalChart__box" style={{minHeight}}>
            <ResponsiveContainer width="100%" aspect={ratio} minHeight={minHeight}>
                {children}
            </ResponsiveContainer>
        </div>
    </figure>
);

/** Shared axis and grid styling, so three charts cannot drift into three looks. */
export const chartTheme = {
    grid: {stroke: "#dce8ee", strokeDasharray: "3 3", vertical: false},
    axis: {
        tick: {fontSize: 12, fill: "#5b7a8a"}, tickLine: false,
        axisLine: {stroke: "#cddde5"}
    },
    tooltip: {
        contentStyle: {
            borderRadius: "0 10px 10px 10px",
            border: "1px solid #cddde5",
            fontSize: 13,
        },
        cursor: {fill: "rgba(34, 133, 178, 0.06)"},
    },
    bar: "#2285b2",
    line: "#f25d23",
};

export default ChartFrame;
