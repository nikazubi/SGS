import axios from "../../../utils/axios";

export const fetchGradesSemester = async (filters, checked) => {
    const params = {
        classId: filters?.academyClass.id,
        studentId: filters?.student.id,
        yearRange: filters.yearRange,
        component: filters.semesterN?.value,
        isDecimalSystem: !!checked
    }
    axios.get("/test/exportToExcel/semester", {responseType: 'blob', params: params})
        .then((response) => {
            const url = window.URL.createObjectURL(new Blob([response.data], {type: 'application/octet-stream'}));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', "IB_Mthiebi_" + filters.academyClass.className + ".xlsx");
            document.body.appendChild(link);
            link.click();
        })
}

export const fetchGradeMonthly = async (filters, checked) => {
    const params = {
        classId: filters?.academyClass.id,
        studentId: filters?.student.id,
        createDate: Date.parse(filters.date),
        component: "monthly",
        isDecimalSystem: checked
    }
    axios.get("/test/exportToExcel", {responseType: 'blob', params: params})
        .then((response) => {
            const url = window.URL.createObjectURL(new Blob([response.data], {type: 'application/octet-stream'}));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', "IB_Mthiebi_" + filters.academyClass.className + ".xlsx");
            document.body.appendChild(link);
            link.click();
        })
}

export const fetchGradesAnual = async (filters, checked) => {
    const params = {
        classId: filters?.academyClass.id,
        studentId: filters?.student.id,
        yearRange: filters.yearRange,
        component: "anual",
        isDecimalSystem: !!checked
    }
    axios.get("/test/exportToExcel/anual", {responseType: 'blob', params: params})
        .then((response) => {
            const url = window.URL.createObjectURL(new Blob([response.data], {type: 'application/octet-stream'}));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', "IB_Mthiebi_" + filters.academyClass.className + ".xlsx");
            document.body.appendChild(link);
            link.click();
        })
}