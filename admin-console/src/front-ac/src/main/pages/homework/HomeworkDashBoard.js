import React, {useMemo, useState} from "react";
import {useQuery} from "react-query";
import {
    Accordion, AccordionDetails, AccordionSummary, Button, Chip, Dialog,
    DialogContent, DialogTitle, IconButton, TextField, Tooltip,
    Typography
} from "@mui/material";
import {Add, Delete, Edit, ExpandMore} from "@mui/icons-material";
import {Formik} from "formik";
import FlexBox from "../../../components/FlexBox";
import FormikAutocomplete from "../../components/formik/FormikAutocomplete";
import {getFiltersOfPage, setFiltersOfPage} from "../../../utils/filters";
import {useNotification} from "../../../contexts/notification-context";
import {fetchClasses, fetchSubjects} from "../gradebook/gradebookApi";
import {archiveHomework, fetchHomework} from "./homeworkApi";
import HomeworkEditor from "./HomeworkEditor";

/**
 * Homework, by subject.
 *
 * The class's subjects are the accordions — confirmed with the school, who
 * scope by class rather than by what a teacher personally teaches. A class
 * filter is here for anyone holding more than one; a coordinator holding one
 * sees it pre-selected and never thinks about it.
 *
 * Staff side only. The parent side of every content module lands in phase 11.
 */

const PAGE_ID = "HOMEWORK";

/**
 * How many rows an accordion shows before "see more".
 *
 * A named constant on purpose: the school asked to settle this by looking at it.
 */
const TOP_N = 5;

const HomeworkDashBoard = () => {

    const {setErrorMessage} = useNotification();
    const [filters, setFilters] = useState({...getFiltersOfPage(PAGE_ID)});
    const [editing, setEditing] = useState(null);
    const [seeMoreSubject, setSeeMoreSubject] = useState(null);

    const classGroupId = filters?.classGroup?.id;

    const commit = (values) => {
        setFilters(values);
        setFiltersOfPage(PAGE_ID, values);
    };

    const {data: subjects} = useQuery(
        ["HOMEWORK_SUBJECTS", classGroupId],
        () => fetchSubjects(classGroupId),
        {enabled: Boolean(classGroupId)});

    return (
        <div style={{margin: "0 15px"}}>
            <FlexBox style={{
                justifyContent: "space-between", alignItems: "center",
                margin: "20px 0"
            }}>
                <Formik initialValues={{classGroup: filters?.classGroup || ""}} enableReinitialize onSubmit={() => {
                }}>
                    {({values, setFieldValue}) => (
                        <div style={{display: "flex", gap: 16, alignItems: "center"}}>
                            <FormikAutocomplete
                                name="classGroup"
                                label="კლასი"
                                style={{width: 220}}
                                onFetch={fetchClasses}
                                multiple={false}
                                getOptionSelected={(o, v) => o?.id === v?.id}
                                getOptionLabel={(o) => o?.name || ""}
                                onChange={(event, value) => {
                                    setFieldValue("classGroup", value);
                                    commit({...values, classGroup: value});
                                }}
                            />
                            <TextField
                                type="date" size="small" label="დან"
                                InputLabelProps={{shrink: true}}
                                value={filters?.from || ""}
                                onChange={(e) => commit({...filters, from: e.target.value})}
                            />
                            <TextField
                                type="date" size="small" label="მდე"
                                InputLabelProps={{shrink: true}}
                                value={filters?.to || ""}
                                onChange={(e) => commit({...filters, to: e.target.value})}
                            />
                        </div>
                    )}
                </Formik>
            </FlexBox>

            {!classGroupId ? (
                <Typography variant="body2" style={{color: "#888", padding: 24}}>
                    აირჩიეთ კლასი.
                </Typography>
            ) : (subjects || []).map(subject => (
                <SubjectAccordion
                    key={subject.id}
                    subject={subject}
                    classGroupId={classGroupId}
                    from={filters?.from}
                    to={filters?.to}
                    onAdd={() => setEditing({uuid: null, subject})}
                    onEdit={(item) => setEditing({uuid: item.uuid, subject})}
                    onSeeMore={() => setSeeMoreSubject(subject)}
                    onError={setErrorMessage}
                />
            ))}

            <HomeworkEditor
                open={Boolean(editing)}
                uuid={editing?.uuid}
                classGroupId={classGroupId}
                subjectId={editing?.subject?.id}
                subjectName={editing?.subject?.name}
                onClose={() => setEditing(null)}
                onSaved={() => {
                }}
                onError={setErrorMessage}
            />

            <Dialog open={Boolean(seeMoreSubject)} onClose={() => setSeeMoreSubject(null)}
                    maxWidth="md" fullWidth>
                <DialogTitle>{seeMoreSubject?.name}</DialogTitle>
                <DialogContent>
                    {seeMoreSubject ? (
                        <HomeworkList
                            classGroupId={classGroupId}
                            subjectId={seeMoreSubject.id}
                            from={filters?.from}
                            to={filters?.to}
                            limit={null}
                            onEdit={(item) => {
                                setSeeMoreSubject(null);
                                setEditing({uuid: item.uuid, subject: seeMoreSubject});
                            }}
                            onError={setErrorMessage}
                        />
                    ) : null}
                </DialogContent>
            </Dialog>
        </div>
    );
};

const SubjectAccordion = ({
                              subject, classGroupId, from, to, onAdd, onEdit,
                              onSeeMore, onError
                          }) => (
    <Accordion TransitionProps={{unmountOnExit: true}}>
        <AccordionSummary expandIcon={<ExpandMore/>}>
            <Typography>{subject.name}</Typography>
        </AccordionSummary>
        <AccordionDetails>
            <Button size="small" startIcon={<Add/>} onClick={onAdd}
                    style={{textTransform: "none", marginBottom: 8}}>
                დამატება
            </Button>

            <HomeworkList
                classGroupId={classGroupId}
                subjectId={subject.id}
                from={from}
                to={to}
                limit={TOP_N}
                onEdit={onEdit}
                onError={onError}
            />

            <Button size="small" onClick={onSeeMore} style={{textTransform: "none"}}>
                ყველას ნახვა
            </Button>
        </AccordionDetails>
    </Accordion>
);

/**
 * The rows themselves, shared by the accordion and the "see more" dialog so the
 * two cannot drift apart.
 */
const HomeworkList = ({classGroupId, subjectId, from, to, limit, onEdit, onError}) => {

    const {data: items, isLoading, refetch} = useQuery(
        ["HOMEWORK_LIST", classGroupId, subjectId, from, to, limit],
        () => fetchHomework({classGroupId, subjectId, from, to, limit}),
        {enabled: Boolean(classGroupId), onError});

    const rows = useMemo(() => items || [], [items]);

    const remove = async (item) => {
        try {
            await archiveHomework({uuid: item.uuid});
            refetch();
        } catch (e) {
            onError(e);
        }
    };

    if (isLoading) {
        return <Typography variant="body2" style={{color: "#888"}}>...</Typography>;
    }
    if (rows.length === 0) {
        return (
            <Typography variant="body2" style={{color: "#888", padding: 8}}>
                ჯერ დავალება არ არის.
            </Typography>
        );
    }

    return (
        <div>
            {rows.map(item => (
                <div key={item.uuid}
                     style={{
                         display: "flex", alignItems: "center", gap: 12,
                         padding: "6px 4px", borderBottom: "1px solid #f0f0f0"
                     }}>
                    <span style={{width: 96, color: "#5b7c8d"}}>{item.eventDate || "—"}</span>
                    <span style={{flex: 1}}>{item.title || "—"}</span>

                    {/* Three states, not two. The brief asks for saved-versus-sent;
                        frozen publication needs a third, or a teacher edits, is
                        satisfied, and the change never reaches anyone. */}
                    <StateChip item={item}/>

                    {item.targetEnrollmentIds?.length > 0 ? (
                        <Tooltip title={(item.targetNames || []).join(", ")}>
                            <Chip size="small" variant="outlined"
                                  label={`${item.targetEnrollmentIds.length} მოსწავლე`}/>
                        </Tooltip>
                    ) : (
                        <Chip size="small" variant="outlined" label="მთელი კლასი"/>
                    )}

                    <IconButton size="small" onClick={() => onEdit(item)}>
                        <Edit fontSize="small"/>
                    </IconButton>
                    <IconButton size="small" onClick={() => remove(item)}>
                        <Delete fontSize="small"/>
                    </IconButton>
                </div>
            ))}
        </div>
    );
};

const StateChip = ({item}) => {
    if (item.status !== "PUBLISHED") {
        return <Chip size="small" label="შენახული" style={{backgroundColor: "#eceff1"}}/>;
    }
    if (item.hasUnpublishedChanges) {
        return (
            <Tooltip title="შეიცვალა გამოქვეყნების შემდეგ — მშობელი ჯერ ძველ ტექსტს ხედავს">
                <Chip size="small" label="ცვლილება გამოსაქვეყნებელია"
                      style={{backgroundColor: "#fff3cd", color: "#8a6d3b"}}/>
            </Tooltip>
        );
    }
    return <Chip size="small" label="გამოქვეყნებული"
                 style={{backgroundColor: "#ddf1e5", color: "#2e6b4f"}}/>;
};

export default HomeworkDashBoard;
