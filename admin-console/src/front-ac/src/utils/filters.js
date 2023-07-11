
const FILTER_ID = "filters";
export const getFiltersOfPage = (pageId) =>{
    const filters = JSON.parse(localStorage.getItem(FILTER_ID));
    if (!filters)
        return {}
    return filters[pageId];
}

export const setFiltersOfPage = (pageId, filters) =>{
    let json = JSON.parse(localStorage.getItem(FILTER_ID))
    if(!json){
        json = {}
    }
    json[pageId] = filters;
    localStorage.setItem(FILTER_ID, JSON.stringify(json));
}