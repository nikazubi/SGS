/**
 * Colour tokens for the primary parent console.
 *
 * Three rules keep this consistent:
 *   1. `brand` is the only colour used for functional elements — links, focus
 *      rings, active nav, buttons. Module colours are decoration only.
 *   2. `badge` means unread and nothing else. Never reuse it.
 *   3. `state.under` / `state.over` belong to the absence chart. They are the
 *      only greens and reds in the interface outside illustration.
 */

export const brand = {
    base: '#2285B2', // fills, icons, headings, large text
    text: '#1A6689', // links and any text under 18px — meets 4.5:1 on white
    deep: '#12506E',
    tint: '#DCEBF4',
    surface: '#F0F9FD', // section header bands
};

export const pattern = {
    base: '#EDF7FC',
    dot: '#C9E2F0',
    arc: '#EFD6B4',
    speck: '#D5CDF0',
    tick: '#F2CBB9',
};

/** Unread count. Loudest thing on the screen by design. */
export const badge = {
    fill: '#E8730E',
    text: '#FFFFFF',
    onColor: {fill: '#FFFFFF', text: '#D9660A'}, // when the tile itself is saturated
};

/** Absence chart only. */
export const state = {
    under: '#2F8F63', // within the monthly allowance
    over: '#C0392B', // past it
    unset: '#A9BFCB', // no allowance recorded — never guessed
};

/**
 * One entry per landing module. `tile` is the card background, `label` the text
 * on it. `key` matches the module name returned by /api/parent/modules.
 */
export const modules = {
    homework: {tile: '#DCEBF4', label: '#12506E', accent: '#2E86B4'},
    news: {tile: '#E9E5F8', label: '#3E3480', accent: '#8E82D2'},
    schedule: {tile: '#F5E6CE', label: '#7E5210', accent: '#EDD0A2'},
    menu: {tile: '#F7E0D8', label: '#8E3A24', accent: '#D9705A'},
    description: {tile: '#F8E1EA', label: '#8A3357', accent: '#EFBFD2'},
    absence: {tile: '#E1EAEF', label: '#3D5C6D', accent: '#7C9CAE'},
};

/**
 * Monday to Friday, one pastel each.
 *
 * Drawn from the same six the landing tiles use, so the weekly pages read as
 * part of the same set rather than a second palette. Named separately rather
 * than indexed into `modules`, because a weekday is not a module and changing
 * one should not move the other: these five are free to be retuned for
 * legibility across a row without touching what a homework tile looks like.
 */
export const weekdays = [
    modules.homework,     // ორშაბათი
    modules.news,         // სამშაბათი
    modules.schedule,     // ოთხშაბათი
    modules.menu,         // ხუთშაბათი
    modules.description,  // პარასკევი
];

/** Neutrals for cards, body copy and dividers. */
export const neutral = {
    card: '#FFFFFF',
    ink: '#22414F',
    body: '#3B5A6B',
    muted: '#5B7A8A',
    faint: '#93AAB6',
    disabled: '#C4D5DE',
    divider: '#EBF1F4',
    shadow: 'rgba(28, 74, 95, 0.07)',
};

/** Shape tokens shared with the card shell already in the console. */
export const shape = {
    cardRadius: '0 20px 20px 20px',
    tileRadius: '0 24px 24px 24px',
    iconRadius: '0 13px 13px 13px',
};

const primaryTheme = {brand, pattern, badge, state, modules, weekdays, neutral, shape};
export default primaryTheme;
