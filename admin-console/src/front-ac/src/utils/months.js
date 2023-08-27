export const MONTHS = [

    {
     key: 0,
     value: 'იანვარი-თებერვალი'
    },
    {
     key: 2,
     value: 'მარტი'
    },
    {
     key: 3,
     value: 'აპრილი'
    },
    {
     key: 4,
     value: 'მაისი'
    },
    {
     key: 5,
     value: 'ივნისი'
    },
    // {
    //  key: 6,
    //  value: 'ივლისი'
    // },
    // {
    //  key: 7,
    //  value: 'აგვისტო'
    // },
    {
     key: 8,
     value: 'სექტემბერი-ოქტომბერი'
    },
    // {
    //  key: 9,
    //  value: 'ოქტომბერი'
    // },
    {
     key: 10,
     value: 'ნოემბერი'
    },
    {
     key: 11,
     value: 'დეკემბერი'
    }
]

export const getFirstSemestMonths = () => {
    return [MONTHS[5], MONTHS[6], MONTHS[7]]
}
export const getSecondSemestMonths = () => {
    return [MONTHS[0], MONTHS[1], MONTHS[2], MONTHS[3], MONTHS[4]]
}