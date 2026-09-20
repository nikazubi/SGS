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
 * Only the absence journal (chartKey ABSENCE_BARS) gets a tile here - the brief
 * gives primary no grade journals at all, so any other journal a primary child
 * is offered (assignment today pins a version rather than restricting who sees
 * it - TEST-RESULTS.md 3.A) is a school-side question, not something this page
 * should surface as a seventh box.
 */
const PrimaryLandingGrid = ({modules, journals}) => {
    const present = new Set(modules || []);
    const absenceJournal = (journals || []).find(j => j.chartKey === "ABSENCE_BARS");

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

                {absenceJournal ? (
                    <Link to={`/journal/${absenceJournal.uuid}`}
                          className="primaryLanding__tile primaryLanding__tile--absence"
                          style={{background: moduleColors.absence.tile, color: moduleColors.absence.label}}>
                        <span className="primaryLanding__label">{absenceJournal.name}</span>
                        <img src={absenceIcon} alt="" className="primaryLanding__icon"/>
                    </Link>
                ) : null}
            </div>
        </PatternBackground>
    );
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
