import React, {useEffect, useState} from "react";
import {useMutation, useQuery, useQueryClient} from "react-query";
import {
    Autocomplete, Button, Chip, Dialog, DialogActions, DialogContent,
    DialogTitle, IconButton, TextField, Tooltip, Typography
} from "@mui/material";
import {Add, Delete, Edit} from "@mui/icons-material";
import ReactQuill from "react-quill";
import "react-quill/dist/quill.snow.css";
import {useNotification} from "../../../contexts/notification-context";
import {
    addCategory, archiveNews, fetchCategories, fetchNews, fetchNewsItem,
    publishNews, saveNews, uploadImage
} from "./contentApi";
import NewsImage from "./NewsImage";

/**
 * School news.
 *
 * The one content module with no class — so no class filter, and the only one
 * that does not go through class scoping on the server either.
 */

const TOOLBAR = [
    ["bold", "italic", "underline"],
    [{list: "ordered"}, {list: "bullet"}],
    [{header: [3, 4, false]}],
    ["blockquote", "link"],
    ["clean"]
];

/** Roughly a line and a half of the body, with the markup taken off. */
const preview = (html) => {
    if (!html) return "";
    const text = html.replace(/<[^>]*>/g, " ").replace(/\s+/g, " ").trim();
    return text.length > 160 ? text.slice(0, 160) + "…" : text;
};

const NewsPage = () => {

    const {setErrorMessage} = useNotification();
    const [filters, setFilters] = useState({categoryId: null, from: "", to: ""});
    const [editing, setEditing] = useState(null);

    const {data: categories} = useQuery("NEWS_CATEGORIES", fetchCategories,
        {onError: setErrorMessage});

    const {data: items, refetch} = useQuery(
        ["NEWS_LIST", filters.categoryId, filters.from, filters.to],
        () => fetchNews({
            categoryId: filters.categoryId,
            from: filters.from || undefined,
            to: filters.to || undefined
        }),
        {onError: setErrorMessage});

    const remove = async (item) => {
        try {
            await archiveNews({uuid: item.uuid});
            refetch();
        } catch (e) {
            setErrorMessage(e);
        }
    };

    const selectedCategory = (categories || []).find(c => c.id === filters.categoryId) || null;

    return (
        <div style={{margin: "0 15px"}}>
            <div style={{
                display: "flex", justifyContent: "space-between",
                alignItems: "center", margin: "20px 0", gap: 16
            }}>
                <div style={{display: "flex", gap: 12, alignItems: "center"}}>
                    <Typography variant="h6">სიახლეები</Typography>
                    <Autocomplete
                        size="small" style={{width: 200}}
                        options={categories || []}
                        value={selectedCategory}
                        getOptionLabel={(c) => c?.name || ""}
                        isOptionEqualToValue={(a, b) => a.id === b.id}
                        onChange={(e, value) =>
                            setFilters(f => ({...f, categoryId: value ? value.id : null}))}
                        renderInput={(params) => <TextField {...params} label="კატეგორია"/>}
                    />
                    <TextField
                        type="date" size="small" label="დან"
                        InputLabelProps={{shrink: true}}
                        value={filters.from}
                        onChange={(e) => setFilters(f => ({...f, from: e.target.value}))}
                    />
                    <TextField
                        type="date" size="small" label="მდე"
                        InputLabelProps={{shrink: true}}
                        value={filters.to}
                        onChange={(e) => setFilters(f => ({...f, to: e.target.value}))}
                    />
                </div>
                <Button variant="contained" startIcon={<Add/>}
                        onClick={() => setEditing({uuid: null})}
                        style={{textTransform: "none"}}>
                    დამატება
                </Button>
            </div>

            {(items || []).length === 0 ? (
                <Typography variant="body2" style={{color: "#888", padding: 24}}>
                    სიახლე არ არის.
                </Typography>
            ) : (items || []).map(item => (
                <div key={item.uuid}
                     style={{
                         display: "flex", alignItems: "center", gap: 16,
                         padding: 12, borderBottom: "1px solid #f0f0f0"
                     }}>
                    <span style={{width: 96, color: "#5b7c8d"}}>{item.eventDate || "—"}</span>
                    <NewsImage uuid={item.imageUuid}/>
                    <div style={{flex: 1, minWidth: 0}}>
                        {/* The school asked for the name bold and a little larger. */}
                        <div style={{fontWeight: "bold", fontSize: 14}}>{item.title || "—"}</div>
                        <div style={{
                            color: "#666", fontSize: 13, overflow: "hidden",
                            textOverflow: "ellipsis", whiteSpace: "nowrap"
                        }}>
                            {preview(item.bodyHtml)}
                        </div>
                    </div>
                    {item.categoryName ? (
                        <Chip size="small" variant="outlined" label={item.categoryName}/>
                    ) : null}
                    <StateChip item={item}/>
                    <IconButton size="small" onClick={() => setEditing({uuid: item.uuid})}>
                        <Edit fontSize="small"/>
                    </IconButton>
                    <IconButton size="small" onClick={() => remove(item)}>
                        <Delete fontSize="small"/>
                    </IconButton>
                </div>
            ))}

            <Editor
                open={Boolean(editing)}
                uuid={editing?.uuid}
                categories={categories}
                onClose={() => setEditing(null)}
                onError={setErrorMessage}
            />
        </div>
    );
};

const Editor = ({open, uuid, categories, onClose, onError}) => {

    const queryClient = useQueryClient();
    const [draft, setDraft] = useState({});
    const [uploading, setUploading] = useState(false);

    const {data: existing} = useQuery(["NEWS_ITEM", uuid], () => fetchNewsItem(uuid),
        {enabled: open && Boolean(uuid)});

    useEffect(() => {
        if (!open) return;
        if (uuid && existing) {
            setDraft({
                uuid: existing.uuid,
                eventDate: existing.eventDate || "",
                title: existing.title || "",
                bodyHtml: existing.bodyHtml || "",
                categoryUuid: existing.categoryUuid || null,
                imageUuid: existing.imageUuid || null
            });
        } else if (!uuid) {
            setDraft({
                uuid: null,
                eventDate: new Date().toISOString().slice(0, 10),
                title: "",
                bodyHtml: "",
                categoryUuid: null,
                imageUuid: null
            });
        }
    }, [open, uuid, existing]);

    const invalidate = () => {
        queryClient.invalidateQueries("NEWS_LIST");
        queryClient.invalidateQueries("NEWS_CATEGORIES");
        queryClient.invalidateQueries(["NEWS_ITEM", uuid]);
    };

    const save = useMutation(saveNews,
        {
            onSuccess: () => {
                invalidate();
                onClose();
            }, onError
        });

    const saveAndPublish = useMutation(async (d) => {
        const saved = await saveNews(d);
        return publishNews(saved.uuid);
    }, {
        onSuccess: () => {
            invalidate();
            onClose();
        }, onError
    });

    /**
     * Uploaded immediately rather than held until save, so the author sees the
     * picture that was actually stored — downscaled and re-encoded — rather than
     * the one they picked.
     */
    const pickImage = async (event) => {
        const file = event.target.files?.[0];
        if (!file) return;
        setUploading(true);
        try {
            const stored = await uploadImage(file);
            setDraft(d => ({...d, imageUuid: stored.uuid}));
        } catch (e) {
            onError(e);
        } finally {
            setUploading(false);
            // Cleared, or picking the same file twice fires no change event.
            event.target.value = "";
        }
    };

    const chooseCategory = async (value) => {
        if (typeof value === "string") {
            // Typed something new: created, or matched to an existing one.
            try {
                const created = await addCategory(value);
                queryClient.invalidateQueries("NEWS_CATEGORIES");
                setDraft(d => ({...d, categoryUuid: created.uuid}));
            } catch (e) {
                onError(e);
            }
            return;
        }
        setDraft(d => ({...d, categoryUuid: value ? value.uuid : null}));
    };

    const busy = save.isLoading || saveAndPublish.isLoading || uploading;
    const selectedCategory = (categories || []).find(c => c.uuid === draft.categoryUuid) || null;
    const published = existing && existing.status === "PUBLISHED";

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>{uuid ? "სიახლის რედაქტირება" : "ახალი სიახლე"}</DialogTitle>
            <DialogContent>
                {published ? (
                    <Typography variant="caption"
                                style={{display: "block", color: "#8a6d3b", marginBottom: 12}}>
                        უკვე გამოქვეყნებულია. ცვლილება მშობლამდე მხოლოდ ხელახალი
                        გამოქვეყნების შემდეგ მიდის.
                    </Typography>
                ) : null}

                <div style={{display: "flex", gap: 12, marginBottom: 16}}>
                    <TextField
                        type="date" size="small" label="თარიღი"
                        InputLabelProps={{shrink: true}}
                        value={draft.eventDate || ""}
                        onChange={(e) => setDraft(d => ({...d, eventDate: e.target.value}))}
                    />
                    <TextField
                        fullWidth size="small" label="სათაური"
                        value={draft.title || ""}
                        onChange={(e) => setDraft(d => ({...d, title: e.target.value}))}
                    />
                </div>

                <div style={{display: "flex", gap: 16, alignItems: "center", marginBottom: 16}}>
                    <NewsImage uuid={draft.imageUuid} size={88}/>
                    <div>
                        <Button component="label" size="small" disabled={busy}
                                style={{textTransform: "none"}}>
                            {draft.imageUuid ? "სურათის შეცვლა" : "სურათის ატვირთვა"}
                            <input hidden type="file" accept="image/*" onChange={pickImage}/>
                        </Button>
                        {draft.imageUuid ? (
                            <Button size="small" color="error" disabled={busy}
                                    onClick={() => setDraft(d => ({...d, imageUuid: null}))}
                                    style={{textTransform: "none"}}>
                                მოხსნა
                            </Button>
                        ) : null}
                        <Typography variant="caption"
                                    style={{display: "block", color: "#666"}}>
                            მაქსიმუმ 2 მბ. სურათი ავტომატურად პატარავდება.
                        </Typography>
                    </div>

                    <Autocomplete
                        freeSolo size="small" style={{width: 220, marginLeft: "auto"}}
                        options={categories || []}
                        value={selectedCategory}
                        getOptionLabel={(c) => typeof c === "string" ? c : (c?.name || "")}
                        // freeSolo hands a plain string through here the moment
                        // someone types a category that does not exist yet.
                        isOptionEqualToValue={(a, b) =>
                            typeof a === "string" || typeof b === "string"
                                ? a === b : a?.uuid === b?.uuid}
                        onChange={(e, value) => chooseCategory(value)}
                        renderInput={(params) => <TextField {...params} label="კატეგორია"/>}
                    />
                </div>

                <div style={{marginBottom: 44}}>
                    <ReactQuill
                        theme="snow"
                        value={draft.bodyHtml || ""}
                        onChange={(html) => setDraft(d => ({...d, bodyHtml: html}))}
                        modules={{toolbar: TOOLBAR}}
                        style={{height: 220}}
                    />
                </div>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={busy}>გაუქმება</Button>
                <Button disabled={busy} onClick={() => save.mutate(draft)}>შენახვა</Button>
                <Button variant="contained" disabled={busy}
                        onClick={() => saveAndPublish.mutate(draft)}>
                    გამოქვეყნება
                </Button>
            </DialogActions>
        </Dialog>
    );
};

const StateChip = ({item}) => {
    if (item.status !== "PUBLISHED") {
        return <Chip size="small" label="შენახული" style={{backgroundColor: "#eceff1"}}/>;
    }
    if (item.hasUnpublishedChanges) {
        return (
            <Tooltip title="შეიცვალა გამოქვეყნების შემდეგ">
                <Chip size="small" label="ცვლილება გამოსაქვეყნებელია"
                      style={{backgroundColor: "#fff3cd", color: "#8a6d3b"}}/>
            </Tooltip>
        );
    }
    return <Chip size="small" label="გამოქვეყნებული"
                 style={{backgroundColor: "#ddf1e5", color: "#2e6b4f"}}/>;
};

export default NewsPage;
