/**
 * Confetti background used behind the primary parent console.
 *
 * The pattern is generated as an inline SVG data URI, so there is no asset to
 * ship and every colour can be changed at runtime. Pass any subset of `colors`
 * to retheme it; pass `tile` to change the density.
 *
 *   <PatternBackground>
 *     <LandingGrid />
 *   </PatternBackground>
 *
 *   <PatternBackground colors={{ base: '#FDF8F0', dot: '#E4D3BC' }} tile={72}>
 *     ...
 *   </PatternBackground>
 *
 * To drive it from CSS variables instead, use `patternDataUri()` directly and
 * assign the result to a custom property.
 */

export const PATTERN_COLORS = {
    base: '#EDF7FC', // page surface the marks sit on
    dot: '#C9E2F0', // large blue dot
    arc: '#EFD6B4', // sand arc
    speck: '#D5CDF0', // small lilac dot
    tick: '#F2CBB9', // coral tick
};

/**
 * Builds the repeating tile as a data URI.
 * Kept separate so it can be used in plain CSS-in-JS or a CSS variable.
 */
export function patternDataUri(colors = PATTERN_COLORS, tile = 58) {
    const c = {...PATTERN_COLORS, ...colors};
    const svg = [
        `<svg xmlns="http://www.w3.org/2000/svg" width="${tile}" height="${tile}" viewBox="0 0 58 58">`,
        `<circle cx="12" cy="15" r="4" fill="${c.dot}"/>`,
        `<path d="M36 30 q6 -6 12 0" fill="none" stroke="${c.arc}" stroke-width="3" stroke-linecap="round"/>`,
        `<circle cx="26" cy="48" r="3.4" fill="${c.speck}"/>`,
        `<path d="M48 8 l6 5" stroke="${c.tick}" stroke-width="3" stroke-linecap="round"/>`,
        `</svg>`,
    ].join('');

    return `url("data:image/svg+xml,${encodeURIComponent(svg)}")`;
}

export default function PatternBackground({
                                              colors = PATTERN_COLORS,
                                              tile = 58,
                                              as: Tag = 'div',
                                              className = '',
                                              style = {},
                                              children,
                                              ...rest
                                          }) {
    const {base} = {...PATTERN_COLORS, ...colors};

    return (
        <Tag
            className={className}
            style={{
                backgroundColor: base,
                backgroundImage: patternDataUri(colors, tile),
                backgroundRepeat: 'repeat',
                backgroundSize: `${tile}px ${tile}px`,
                ...style,
            }}
            {...rest}
        >
            {children}
        </Tag>
    );
}
