import {Link} from "react-router-dom";
import {useQuery} from "react-query";
import PatternBackground from "../../components/PatternBackground";
import {badge, modules as moduleColors, pattern} from "../../theme/primaryTheme";
import {fetchHomeworkMonth} from "../journal/parentApi";
import NewsTile from "../../components/NewsTile";
import homeworkIcon from "../../assets/images/primary/homework.png";
import newsIcon from "../../assets/images/primary/news.png";
import scheduleIcon from "../../assets/images/primary/schedule.png";
import menuIcon from "../../assets/images/primary/menu.png";
import descriptionIcon from "../../assets/images/primary/description.png";
import absenceIcon from "../../assets/images/primary/absence.png";
import gradesSubjectIcon from "../../assets/images/primary/grades-subject.svg";
import gradesSummaryIcon from "../../assets/images/primary/grades-summary.svg";
import gradesAnnualIcon from "../../assets/images/primary/grades-annual.svg";
import ethicsIcon from "../../assets/images/primary/ethics.svg";
import {linkFor} from "./viewLink";
import "./primaryLanding.css";

/** Everything on the landing page except the two big tiles and the absence journal. */
const SMALL_TILES = {
    SCHEDULE: {text: "დღის რეჟიმი", link: "/schedule", icon: scheduleIcon, color: moduleColors.schedule},
    MENU: {text: "კვება", link: "/menu", icon: menuIcon, color: moduleColors.menu},
    CHARACTERIZATION: {
        text: "მოსწავლის დახასიათება", link: "/description",
        icon: descriptionIcon, color: moduleColors.description
    },
};

const thisMonth = () => new Date().toISOString().slice(0, 7);

/**
 * The primary-school landing page.
 *
 * Six boxes in the shape the school was sent: news and homework large and side
 * by side, three small tiles below, absence full-width at the bottom. Everything
 * still comes from /modules and /journals - this only changes how the same data
 * is drawn, so a module the console does not recognise is still skipped rather
 * than breaking the grid.
 *
 * **Grades, where the class has them.** The brief gave primary no grade journals;
 * the school then gave them to grades 5 and 6 (db/038), keeping every primary
 * page as well. The server decides which views a child gets, so this draws
 * every view it sent except absence's, which keeps its own tile - for grades
 * 1-4 that is none, and the page is the six boxes above:
 *
 *     news (2)                     homework (1)
 *     schedule (1)    menu (1)     description (1)
 *     by subject (1)  summary (1)  trimester+year (1)     <- grades
 *     ethics (1)      absence (2)                         <- ethics
 *
 * Absence closes the grid and takes whatever the last row left, so the rows
 * stay full whether the school has three grade views, four, or more.
 */
const PrimaryLandingGrid = ({modules, journals, views}) => {
    const present = new Set(modules || []);
    const absenceJournal = (journals || []).find(j => j.chartKey === "ABSENCE_BARS");
    const gradeViews = (views || []).filter(
        v => !absenceJournal || v.journalUuid !== absenceJournal.uuid);
    // What the last grade row leaves free - all of it when they fill their rows.
    const leftOver = gradeViews.length % 3;
    const absenceSpan = leftOver === 0 ? 3 : 3 - leftOver;

    return (
        <PatternBackground colors={pattern} className="primaryLanding">
            <div className="primaryLanding__grid">
                {present.has("NEWS") ? (
                    <NewsTile
                        className="primaryLanding__tile primaryLanding__tile--news"
                        style={{
                            background: moduleColors.news.tile,
                            color: moduleColors.news.label
                        }}
                        placeholderIcon={newsIcon}/>
                ) : null}
                {present.has("HOMEWORK") ? <HomeworkTile/> : null}

                {Object.entries(SMALL_TILES)
                    .filter(([name]) => present.has(name))
                    .map(([name, tile]) => (
                        <Link key={name} to={tile.link} className="primaryLanding__tile primaryLanding__tile--small"
                              style={{background: tile.color.tile, color: tile.color.label}}>
                            <img src={tile.icon} alt="" className="primaryLanding__icon"/>
                            <span className="primaryLanding__label">{tile.text}</span>
                        </Link>
                    ))}

                {gradeViews.map(view => {
                    const look = gradeLookFor(view);
                    return (
                        <Link key={view.journalUuid + ":" + view.label} to={linkFor(view)}
                              className="primaryLanding__tile primaryLanding__tile--small"
                              style={{background: look.color.tile, color: look.color.label}}>
                            <img src={look.icon} alt="" className="primaryLanding__icon"/>
                            <span className="primaryLanding__label">{view.label}</span>
                        </Link>
                    );
                })}

                {absenceJournal ? (
                    <Link to={`/journal/${absenceJournal.uuid}`}
                          className={"primaryLanding__tile primaryLanding__tile--absence"
                              + (absenceSpan < 3 ? ` primaryLanding__tile--span${absenceSpan}` : "")}
                          style={{background: moduleColors.absence.tile, color: moduleColors.absence.label}}>
                        <span className="primaryLanding__label">{absenceJournal.name}</span>
                        <img src={absenceIcon} alt="" className="primaryLanding__icon"/>
                    </Link>
                ) : null}
            </div>
        </PatternBackground>
    );
};

/**
 * A grade tile's picture and colours, read off what the view is rather than
 * where it sits - a view added or removed later still gets the right one.
 * Ethics is the one class-wide journal among them; of the trimester journal's
 * three, one subject is the detail, the year is the annual table, and the rest
 * is the summary.
 */
const gradeLookFor = (view) => {
    if (!view.subjectScoped) return {icon: ethicsIcon, color: moduleColors.ethics};
    if (view.subjectMode === "ONE") return {icon: gradesSubjectIcon, color: moduleColors.gradesSubject};
    if (view.periodKind === "YEAR") return {icon: gradesAnnualIcon, color: moduleColors.gradesAnnual};
    return {icon: gradesSummaryIcon, color: moduleColors.gradesSummary};
};

/** The homework tile: a graduation cap and how much of this month is unopened. */
const HomeworkTile = () => {
    const {data} = useQuery(
        ["PARENT_HOMEWORK_MONTH", thisMonth()], () => fetchHomeworkMonth(thisMonth()),
        {refetchOnWindowFocus: false});

    const unseen = (data?.days || []).reduce((sum, day) => sum + day.unseen, 0);

    return (
        <Link to="/homework" className="primaryLanding__tile primaryLanding__tile--homework"
              style={{background: moduleColors.homework.tile, color: moduleColors.homework.label}}>
            {unseen > 0 ? (
                <span className="primaryLanding__badge"
                      style={{background: badge.fill, color: badge.text}}>{unseen}</span>
            ) : null}
            <img src={homeworkIcon} alt="" className="primaryLanding__icon"/>
            <span className="primaryLanding__label">საშინაო დავალება</span>
        </Link>
    );
};

export default PrimaryLandingGrid;
