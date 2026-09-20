import {modules as moduleColours} from "../../theme/primaryTheme";

/**
 * What colour a news card is.
 *
 * **Primary: one colour per category.** Rotating by position was the first
 * plan and it has a trap — the cards already carry a category chip, so a colour
 * that changes with position looks meaningful while meaning nothing, and the
 * same article is pink today and yellow next week as newer items push it down.
 * Keyed on the category instead, the colour says the same thing the chip says
 * and an article keeps it for good.
 *
 * **Everyone else: one colour**, the navy the landing buttons and the mark
 * cards use, because basic and secondary have no pastel scheme to join.
 */

/** Cycled across categories, in the order the school created them. */
const PRIMARY_CYCLE = [
    moduleColours.description,  // pink
    moduleColours.schedule,     // sand
    moduleColours.news,         // lilac
    moduleColours.menu,         // peach
    moduleColours.homework,     // blue
];

/**
 * Uncategorised news.
 *
 * The one module colour the weekday cards do not use, so "no category" is a
 * colour of its own rather than borrowing the first category's and implying
 * something that is not true.
 */
const PRIMARY_NONE = moduleColours.absence;

/** The navy of the landing tiles, with white on it. */
const STANDARD = {
    tile: "#01619b",
    label: "#ffffff",
    accent: "rgba(255, 255, 255, 0.45)",
};

/**
 * SCHEDULE reaches a primary child alone (ParentContentService.modulesFor), so
 * its presence is the same signal the server keys the rule on rather than a
 * guess of the console's own.
 */
export const isPrimary = (modules) => (modules || []).includes("SCHEDULE");

/**
 * @param primary    whether this is the primary school's parent
 * @param category   the item's category name, or null
 * @param order      category names in a stable order - the list the filter is
 *                   built from, which the server returns in its own order
 */
export const newsColour = (primary, category, order) => {
    if (!primary) {
        return STANDARD;
    }
    if (!category) {
        return PRIMARY_NONE;
    }
    const at = (order || []).indexOf(category);
    return PRIMARY_CYCLE[(at < 0 ? 0 : at) % PRIMARY_CYCLE.length];
};
