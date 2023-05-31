import { differenceInSeconds, format, getUnixTime, isAfter } from 'date-fns';
// import { v4 as generateUUID } from 'uuid';
import imageCompression from 'browser-image-compression';


export const handleModalFormSuccess = ({
                                         setNotification,
                                         onClose,
                                         resetForm,
                                         onFormSuccess,
                                         closeOnSuccess,
                                       }) => {
  if (!!setNotification && typeof setNotification === 'function') {
    setNotification({
      message: "ოპერაცია წარმატებით განხორციელდა",
      severity: 'success'
    });
  }

  if (typeof resetForm === 'function' && closeOnSuccess) {
    resetForm({});
  }

  if (typeof onClose === 'function' && closeOnSuccess) {
    onClose();
  }

  if (onFormSuccess) {
    onFormSuccess();
  }
};

export const clearObject = obj => {
  if (Array.isArray(obj)) {
    return obj;
  }
  const copyOfObj = {...obj};
  for (let prop in copyOfObj) {
    if (copyOfObj.hasOwnProperty(prop) && !obj[prop]) {
      delete copyOfObj[prop];
    }
  }
  return copyOfObj;
};

export function detectTabWithError(fieldsMap, errors) {
  let tabWithError;
  fieldsMap.forEach((fields, tabIndex) => {
    for (const field of fields) {
      if (errors.hasOwnProperty(field)) {
        tabWithError = tabIndex;
        break;
      }
    }
  });
  return tabWithError;
}

export const booleanCellProps = {
  cellStyle: {
    textAlign: 'center',
  },
  headerStyle: {
    textAlign: 'center',
  },
  width: 40
};

export const enumToCombo = enumObj => {
  return Object.values(enumObj).map(el => ({value: el}));
};

export const getImageMime = data => {
  if (data.charAt(0) === '/') {
    return 'image/jpeg';
  } else if (data.charAt(0) === 'R') {
    return 'image/gif';
  } else if (data.charAt(0) === 'i') {
    return 'image/png';
  }
};

export const formatDateTimeCustom = (date, formatString = 'yyyy-MM-dd HH:mm:ss') => {
  if (!date) {
    return null;
  }

  return format(new Date(date), formatString);
};


export const formatDateTime = (date, seconds = true) => {
  if (!date) {
    return null;
  }

  let formatString = 'yyyy-MM-dd HH:mm:ss';
  if (!seconds) {
    formatString = 'yyyy-MM-dd HH:mm';
  }

  return format(new Date(date), formatString);
};

export const dateTimeByFormat = (date, formatStr = 'dd-MM-yyyy') => {
  if (!date) {
    return null;
  }
  return format(new Date(date), formatStr);
};

export const isDateAfter = (dateLeft, dateRight) => isAfter(dateLeft, dateRight);

export const diffDatesInSeconds = (dateLeft, dateRight) => differenceInSeconds(dateLeft, dateRight);

export const toUnixTime = date => getUnixTime(date);

export const numberToCash = number => {
  if (!number && number !== 0) {
    return;
  }
  return Number(number).toFixed(2).toLocaleString('en', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

export const numberToNonRoundedCash = number => {
  if (!number && number !== 0) {
    return;
  }
  return Number(number).toLocaleString('en', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2 //TODO check if 10 was necessary
  });
};

export const capitalizeFirstLetter = str => {
  if (!str || typeof (str) !== 'string') {
    return str;
  }
  return str.charAt(0).toUpperCase() + str.slice(1);
};

// export function fileToBase64(file) {
//   return new Promise((resolve, reject) => {
//     let reader = new FileReader();
//     reader.readAsDataURL(file);
//     reader.onload = function () {
//       resolve({
//         id: generateUniqueId(),
//         name: file.name,
//         content: reader.result,
//         action: 'CREATE'
//       });
//     };
//     reader.onerror = function (error) {
//       reject(error);
//     };
//   });
// }
//
// export function generateUniqueId() {
//   return generateUUID();
// }

export const groupBy = (arr, criteria) => {
  return arr.reduce(function (obj, item) {

    const key = typeof criteria === 'function' ? criteria(item) : item[criteria];

    if (!obj.hasOwnProperty(key)) {
      obj[key] = [];
    }

    obj[key].push(item);

    return obj;
  }, {});
};


const imgZipOpts = {
  maxSizeMB: 1,
  maxWidthOrHeight: 1920,
  useWebWorker: true
};

export function compressImage(file, options = imgZipOpts) {
  return imageCompression(file, options);
}

export const getImageSrcFromBase64 = (base64) => {
  return 'data:image/png;base64,' + base64;
};

export const addToPath = (parent, child) => {
  parent = removeTrailingSlashes(parent);
  child = removeStartingSlashes(child);
  return `${parent}/${child}`;
};

export const removeSlashes = (str) => {
  if (!str) {
    return str;
  }
  str = removeTrailingSlashes(str);
  return removeStartingSlashes(str);
};

export const removeTrailingSlashes = (str) => {
  if (!str) {
    return str;
  }
  str = str.toString();
  return str.replace(/[/]+$/, '');
};

export const removeStartingSlashes = (str) => {
  if (!str) {
    return str;
  }
  str = str.toString();
  return str.replace(/^[/]+/, '');
};

export const createHandleFormSubmit = (onSubmit) => {
  return (event) => {
    event.preventDefault();
    onSubmit();
  };
};

export const combineStrings = (arr) => {
  return arr.filter(str => !!str).join(', ');
};

export const formatDate = (date) => {
  if (!date) {
    return null;
  }

  let formatString = "yyyy-MM-dd";
  return format(new Date(date), formatString);
};
