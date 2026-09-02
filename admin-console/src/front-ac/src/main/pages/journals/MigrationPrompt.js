import React, {useState} from "react";
import {useQuery} from "react-query";
import {
    Button, Dialog, DialogActions, DialogContent, DialogTitle,
    List, ListItem, ListItemText, Typography
} from "@mui/material";
import {migrate, previewMigration} from "./journalApi";

/**
 * "Do you want to recalculate?"
 *
 * Activating a version reaches future periods only. Every period that already
 * holds marks stays on whatever those marks were entered under, and bringing
 * one forward is this, deliberately.
 *
 * Answering no leaves the period where it is. It does not move it and keep the
 * old numbers — those were produced by the old rules, and the new version may
 * not even have the same columns, so the result would be data no rule explains.
 */
const MigrationPrompt = ({journal, onClose, onError, onNotify}) => {

    const [busy, setBusy] = useState(false);

    const {data: plan, isLoading} = useQuery(
        ["MIGRATION_PREVIEW", journal?.uuid],
        () => previewMigration({uuid: journal.uuid}),
        {enabled: Boolean(journal), refetchOnWindowFocus: false}
    );

    const nothingToDo = plan && plan.scopes.length === 0;

    const run = async (scope) => {
        setBusy(true);
        try {
            const result = await migrate({
                uuid: journal.uuid,
                classGroupId: scope?.classGroupId,
                periodId: scope?.periodId
            });
            onNotify({
                message: `გადათვლილია ${result.cellsToRecalculate} შეფასება`,
                severity: "success"
            });
            if (scope) return;
            onClose();
        } catch (e) {
            onError(e);
        } finally {
            setBusy(false);
        }
    };

    return (
        <Dialog open={Boolean(journal)} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>არსებული პერიოდების გადატანა</DialogTitle>
            <DialogContent>

                {isLoading ? <Typography variant="body2">მოწმდება…</Typography> : null}

                {nothingToDo ? (
                    <Typography variant="body2">
                        ყველა პერიოდი უკვე ახალ ვერსიაზეა — გადასატანი არაფერია.
                    </Typography>
                ) : null}

                {plan && !nothingToDo ? (
                    <>
                        <Typography variant="body2" style={{marginBottom: 12}}>
                            ქვემოთ ჩამოთვლილი პერიოდები ჯერ კიდევ ძველ ვერსიაზეა. მათი
                            გადატანა ყოველთვის იწვევს ხელახლა გამოთვლას.
                        </Typography>

                        <div style={{
                            background: "#f4f8fa", borderRadius: 4,
                            padding: 12, marginBottom: 12
                        }}>
                            <Typography variant="body2">
                                <strong>{plan.cellsToRecalculate}</strong> შეფასება
                                გადაითვლება.
                            </Typography>
                            {/* The sentence that should make someone stop. */}
                            {plan.marksToDelete > 0 ? (
                                <Typography variant="body2" style={{color: "#a94442"}}>
                                    <strong>{plan.marksToDelete}</strong> შეფასება წაიშლება —
                                    ეს სვეტები ახალ ვერსიაში აღარ არსებობს:{" "}
                                    {plan.removedColumns.join(", ")}.
                                </Typography>
                            ) : null}
                        </div>

                        <List dense>
                            {plan.scopes.map((scope, i) => (
                                <ListItem
                                    key={i}
                                    secondaryAction={
                                        <Button size="small" disabled={busy}
                                                onClick={() => run(scope)}
                                                style={{textTransform: "none"}}>
                                            გადატანა
                                        </Button>
                                    }
                                >
                                    <ListItemText
                                        primary={`${scope.className} — ${scope.periodLabel}`}
                                        secondary={`${scope.cellCount} შეფასება, `
                                            + `${scope.subjectCount} საგანი`}
                                    />
                                </ListItem>
                            ))}
                        </List>

                        <Typography variant="caption" style={{color: "#666"}}>
                            ხელით შესწორებული უჯრები შენარჩუნდება — ისინი გადათვლისას
                            არ ბრუნდება ფორმულის მნიშვნელობაზე.
                        </Typography>
                    </>
                ) : null}
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose}>
                    {nothingToDo ? "დახურვა" : "არა, დარჩეს ძველ ვერსიაზე"}
                </Button>
                {plan && !nothingToDo ? (
                    <Button variant="contained" color="warning" disabled={busy}
                            onClick={() => run(null)}>
                        ყველას გადატანა
                    </Button>
                ) : null}
            </DialogActions>
        </Dialog>
    );
};

export default MigrationPrompt;
