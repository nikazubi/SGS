export const MONTHS = [

    {
        key: 0,
        value: 'იანვარი-თებერვალი'
    },
    {
        key: 1,
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
    {
        key: 6,
        value: 'ივლისი'
    },
    {
        key: 7,
        value: 'აგვისტო'
    },
    {
        key: 8,
        value: 'სექტემბერი-ოქტომბერი'
    },

    {
        key: 9,
        value: 'სექტემბერი-ოქტომბერი'
    },

    {
        key: 10,
        value: 'ნოემბერი'
    },
    {
        key: 11,
        value: 'დეკემბერი'
    }
]

export const MONTHS_SCHOOL = [

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
    {
        key: 8,
        value: 'სექტემბერი-ოქტომბერი'
    },

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

export const getSemesterOfMonth = (month) => {
    if (month === 8 || month === 9 || month === 10 || month === 11) {
        return {
            value: 'firstSemester',
            label: 'I სემესტრი'
        }
    }

    return {
        value: 'secondSemester',
        label: 'II სემესტრი'
    }
}

export const getRomanByInt = (num) => {
    if (isNaN(num))
        return NaN;
    var digits = String(+num).split(""),
        key = ["", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM",
            "", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC",
            "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"],
        roman = "",
        i = 3;
    while (i--)
        roman = (key[+digits.pop() + (i * 10)] || "") + roman;
    return Array(+digits.join("") + 1).join("M") + roman;
}