package mthiebi.sgs.controllers;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.*;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.GradeType;
import mthiebi.sgs.service.AcademyClassService;
import mthiebi.sgs.utils.AuthConstants;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static mthiebi.sgs.utils.ExcelUtils.sortGradeArrayForExcel;

@RestController
@RequestMapping("/test")
public class ExcelExportController {

    @Autowired
    private GradeController gradeController;

    @Autowired
    private AcademyClassService academyClassService;

    @GetMapping("/exportToExcel")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void exportToExcel(@RequestParam Long classId,
                              @RequestParam String createDate,
                              @RequestParam String component,
                              @RequestParam boolean isDecimalSystem,
                              HttpServletResponse response) throws IOException, SGSException {
        // Create a new workbook and sheet
        List<GradeComponentWrapper> list = gradeController.getGradesByComponent(classId, null, null,  createDate, component);
        sortGradeArrayForExcel(list);
        AcademyClass academyClass = academyClassService.findAcademyClassById(classId);
        Date date = new Date();
        if (createDate != null) {
            date.setTime(Long.parseLong(createDate));
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(academyClass.getClassName());

        // Create a header row
        Row mainRow = sheet.createRow(0);
        Cell headerCell = mainRow.createCell(0);
        headerCell.setCellValue("სკოლა პანსიონ იბ მთიები - კლასი " + academyClass.getClassName() + " - " + adjustMonth(date.getMonth() + 1));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));


        Row headerRow = sheet.createRow(1);
        GradeComponentWrapper first = list.get(0);
        List<SubjectComponentWrapper> subjectComponentWrappersList = first.getGradeList();
        Cell firstCell = headerRow.createCell(0);
        firstCell.setCellValue("მოსწავლის გვარი, სახელი");
        for (int i = 0; i < subjectComponentWrappersList.size() ; i++) {
            Cell dataCell = headerRow.createCell(i + 1);
            dataCell.setCellValue(adjustName(subjectComponentWrappersList.get(i).getSubject().getName()));
        }
        for (int i = 0; i < list.size(); i++) {
            Row studentRow = sheet.createRow(i + 2);
            for (int j = 0; j < list.get(i).getGradeList().size() + 1; j++) {
                if (j == 0) {
                    Cell dataCell = studentRow.createCell(j);
                    dataCell.setCellValue((i+1) + ". " +  list.get(i).getStudent().getLastName() + " " + list.get(i).getStudent().getFirstName());
                } else {
                    Cell dataCell = studentRow.createCell(j);
                    dataCell.setCellValue(adjustGradeValue(list.get(i).getGradeList().get(j-1), isDecimalSystem));
                }
            }
        }

        int teacherIndex = list.size() + 2;
        Row teacherRow = sheet.createRow(teacherIndex);
        Cell teacgerCell = teacherRow.createCell(0);
        teacgerCell.setCellValue("პედაგოგი");

        for (int i = 0; i < subjectComponentWrappersList.size() ; i++) {
            Cell dataCell = teacherRow.createCell(i + 1);
            dataCell.setCellValue(subjectComponentWrappersList.get(i).getSubject().getTeacher());
        }

        for (int i = 0; i <= sheet.getRow(1).getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
        }

        CellStyle thickBorderStyle = workbook.createCellStyle();
        thickBorderStyle.setBorderTop(BorderStyle.THIN);
        thickBorderStyle.setBorderBottom(BorderStyle.THIN);
        thickBorderStyle.setBorderLeft(BorderStyle.THIN);
        thickBorderStyle.setBorderRight(BorderStyle.THIN);
        thickBorderStyle.setAlignment(HorizontalAlignment.CENTER);

        // Apply the thick border style to all cells in the sheet
        for (Row row : sheet) {
            for (Cell cell : row) {
                cell.setCellStyle(thickBorderStyle);
            }
        }

        // Set the response headers for Excel download
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=exported_data.xlsx");

        // Write the workbook to the response stream
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/exportToExcel/semester")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void exportToExcelSemester(@RequestParam Long classId,
                              @RequestParam String yearRange,
                              @RequestParam String component,
                              @RequestParam boolean isDecimalSystem,
                              HttpServletResponse response) throws IOException, SGSException {
        // Create a new workbook and sheet
        List<GradeComponentWrapper> list = gradeController.getGradesByComponent(classId, null, yearRange,  null, component);
        sortGradeArrayForExcel(list);
        AcademyClass academyClass = academyClassService.findAcademyClassById(classId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(academyClass.getClassName());

        // Create a header row
        Row mainRow = sheet.createRow(0);
        Cell headerCell = mainRow.createCell(0);
        headerCell.setCellValue("სკოლა პანსიონ იბ მთიები - კლასი " + academyClass.getClassName() + " - " + yearRange);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        Row headerRow = sheet.createRow(1);
        GradeComponentWrapper first = list.get(0);
        List<SubjectComponentWrapper> subjectComponentWrappersList = first.getGradeList();
        Cell firstCell = headerRow.createCell(0);
        firstCell.setCellValue("მოსწავლის გვარი, სახელი");
        Integer[] grades = component.equals("firstSemester") ? new Integer[]{9, 11, 12, -3, -4, -2, -1} : new Integer[]{1, 3, 4, 5, -5, -6, -2, -1};

        int subjectCount = 0;
        int cc = 0;
        for (int i = 0; i < subjectComponentWrappersList.size() ; i++) {
            String subjectName = subjectComponentWrappersList.get(i).getSubject().getName();
            if (subjectName.equals("absence1") || subjectName.equals("absence2")) {
                cc++;
            }
            Cell dataCell = headerRow.createCell(cc + 1);
            dataCell.setCellValue(adjustName(subjectName));
            subjectCount++;
            if (!subjectName.equals("behaviour1") && !subjectName.equals("behaviour2") &&
                    !subjectName.equals("absence1") && !subjectName.equals("absence2")) {
                sheet.addMergedRegion(new CellRangeAddress(1, 1, (cc + 1) , cc + grades.length));
                cc += grades.length;
            }
        }

        Row monthRows = sheet.createRow(2);
        Cell monthRowsFirstCell = monthRows.createCell(0);

        subjectCount--;
        int dd = 1;
        for (int i = 1; i < subjectCount ; i++) {
            for (int j = 0; j < grades.length ; j++) {
                Cell dataCell = monthRows.createCell(dd + j);
                dataCell.setCellValue(adjustMonthNamesForSemesters(grades[j]));
            }
            dd+= grades.length;
        }

        for (int i = 0; i < list.size(); i++) {
            Row studentRow = sheet.createRow(i + 3);
            for (int j = 0; j < list.get(i).getGradeList().size() + 1; j++) {
                if (j == 0) {
                    Cell dataCell = studentRow.createCell(j);
                    dataCell.setCellValue((i+1) + ". " +  list.get(i).getStudent().getLastName() + " " + list.get(i).getStudent().getFirstName());
                } else {
                    if (list.get(i).getGradeList().get(j-1).getSubject().getName().equals("behaviour1") ||
                            list.get(i).getGradeList().get(j-1).getSubject().getName().equals("behaviour2")    ) {
                        Cell dataCell = studentRow.createCell( ((j-1) * grades.length) + 1);
                        dataCell.setCellValue(adjustSemesterGradeValue(list.get(i).getGradeList().get(j-1), isDecimalSystem, grades, 0));
                        continue;
                    }
                    if (list.get(i).getGradeList().get(j-1).getSubject().getName().equals("absence1") ||
                            list.get(i).getGradeList().get(j-1).getSubject().getName().equals("absence2")    ) {
                        Cell dataCell = studentRow.createCell( ((j-1) * grades.length) - (grades.length - 2) );
                        dataCell.setCellValue(adjustSemesterGradeValue(list.get(i).getGradeList().get(j-1), isDecimalSystem, grades, 0));
                        continue;
                    }
                    for (int l = 0; l < grades.length; l++) {
                        Cell dataCell = studentRow.createCell(l + ((j-1) * grades.length) + 1);
                        dataCell.setCellValue(adjustSemesterGradeValue(list.get(i).getGradeList().get(j-1), isDecimalSystem, grades, l));
                    }
                }
            }
        }

        for (int i = 0; i <= sheet.getRow(1).getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
        }
        for (int i = 0; i <= sheet.getRow(2).getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
        }

        CellStyle thickBorderStyle = workbook.createCellStyle();
        thickBorderStyle.setBorderTop(BorderStyle.THIN);
        thickBorderStyle.setBorderBottom(BorderStyle.THIN);
        thickBorderStyle.setBorderLeft(BorderStyle.THIN);
        thickBorderStyle.setBorderRight(BorderStyle.THIN);
        thickBorderStyle.setAlignment(HorizontalAlignment.CENTER);

        // Apply the thick border style to all cells in the sheet
        for (Row row : sheet) {
            for (Cell cell : row) {
                cell.setCellStyle(thickBorderStyle);
            }
        }

        // Set the response headers for Excel download
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=exported_data.xlsx");

        // Write the workbook to the response stream
        workbook.write(response.getOutputStream());
        workbook.close();
    }


    @GetMapping("/exportToExcel/anual")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void exportToExcelAnual(@RequestParam Long classId,
                                      @RequestParam String yearRange,
                                      @RequestParam String component,
                                      @RequestParam boolean isDecimalSystem,
                                      HttpServletResponse response) throws IOException, SGSException {
        // Create a new workbook and sheet
        List<GradeComponentWrapper> list = gradeController.getGradesByComponent(classId, null, yearRange,  null, component);
        sortGradeArrayForExcel(list);
        AcademyClass academyClass = academyClassService.findAcademyClassById(classId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(academyClass.getClassName());

        // Create a header row
        Row mainRow = sheet.createRow(0);
        Cell headerCell = mainRow.createCell(0);
        headerCell.setCellValue("სკოლა პანსიონ იბ მთიები - კლასი " + academyClass.getClassName() + " - " + yearRange);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        Row headerRow = sheet.createRow(1);
        GradeComponentWrapper first = list.get(0);
        List<SubjectComponentWrapper> subjectComponentWrappersList = first.getGradeList();
        Cell firstCell = headerRow.createCell(0);
        firstCell.setCellValue("მოსწავლის გვარი, სახელი");
        Integer[] grades = new Integer[]{1, 2, 5, 3, 4};

        int subjectCount = 0;
        int cc = 0;
        for (int i = 0; i < subjectComponentWrappersList.size() ; i++) {
            String subjectName = subjectComponentWrappersList.get(i).getSubject().getName();
            if (subjectName.equals("absence1")) {
                cc++;
            }
            Cell dataCell = headerRow.createCell(cc + 1);
            dataCell.setCellValue(adjustName(subjectName));
            subjectCount++;
            if (!subjectName.equals("behaviour1") && !subjectName.equals("absence1") ) {
                sheet.addMergedRegion(new CellRangeAddress(1, 1, (cc + 1) , cc + grades.length));
                cc += grades.length;
            }
        }

        Row monthRows = sheet.createRow(2);
        Cell monthRowsFirstCell = monthRows.createCell(0);
        subjectCount--;

        int dd = 1;
        for (int i = 1; i < subjectCount ; i++) {
            for (int j = 0; j < grades.length ; j++) {
                Cell dataCell = monthRows.createCell(dd + j);
                dataCell.setCellValue(adjustMonthNamesForAnual(grades[j]));
            }
            dd+= grades.length;
        }

        for (int i = 0; i < list.size(); i++) {
            Row studentRow = sheet.createRow(i + 3);
            for (int j = 0; j < list.get(i).getGradeList().size() + 1; j++) {
                if (j == 0) {
                    Cell dataCell = studentRow.createCell(j);
                    dataCell.setCellValue((i+1) + ". " +  list.get(i).getStudent().getLastName() + " " + list.get(i).getStudent().getFirstName());
                } else {
                    if (list.get(i).getGradeList().get(j-1).getSubject().getName().equals("behaviour1")) {
                        Cell dataCell = studentRow.createCell( ((j-1) * grades.length) + 1);
                        dataCell.setCellValue(adjustAnualGradeValue(list.get(i).getGradeList().get(j-1), isDecimalSystem, grades, 0));
                        continue;
                    }
                    if (list.get(i).getGradeList().get(j-1).getSubject().getName().equals("absence1")) {
                        Cell dataCell = studentRow.createCell( ((j-1) * grades.length) - 3);
                        dataCell.setCellValue(adjustAnualGradeValue(list.get(i).getGradeList().get(j-1), isDecimalSystem, grades, 0));
                        continue;
                    }
                    for (int l = 0; l < grades.length; l++) {
                        Cell dataCell = studentRow.createCell(l + ((j-1) * grades.length) + 1);
                        dataCell.setCellValue(adjustAnualGradeValue(list.get(i).getGradeList().get(j-1), isDecimalSystem, grades, l));
                    }
                }
            }
        }

        for (int i = 0; i <= sheet.getRow(1).getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
        }
        for (int i = 0; i <= sheet.getRow(2).getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
        }

        CellStyle thickBorderStyle = workbook.createCellStyle();
        thickBorderStyle.setBorderTop(BorderStyle.THIN);
        thickBorderStyle.setBorderBottom(BorderStyle.THIN);
        thickBorderStyle.setBorderLeft(BorderStyle.THIN);
        thickBorderStyle.setBorderRight(BorderStyle.THIN);
        thickBorderStyle.setAlignment(HorizontalAlignment.CENTER);

        // Apply the thick border style to all cells in the sheet
        for (Row row : sheet) {
            for (Cell cell : row) {
                cell.setCellStyle(thickBorderStyle);
            }
        }

        // Set the response headers for Excel download
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=exported_data.xlsx");

        // Write the workbook to the response stream
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/exportToExcel/dashbord")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void exportToExcelDashboard(@RequestParam Long classId,
                              @RequestParam Long subjectId,
                              @RequestParam String filterDate,
                              @RequestParam String gradeTypePrefix,
                              HttpServletResponse response) throws IOException, SGSException {
        // Create a new workbook and sheet
        List<GradeWrapperDto> list = gradeController.getGradeGrouped(classId, subjectId, null,  filterDate, GradeGroupByClause.STUDENT, gradeTypePrefix);
//        sortGradeArrayForExcel(list);
        AcademyClass academyClass = academyClassService.findAcademyClassById(classId);
        Date date = new Date();
        if (filterDate != null) {
            date.setTime(Long.parseLong(filterDate));
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(academyClass.getClassName());

        // Create a header row
        Row mainRow = sheet.createRow(0);
        Cell headerCell = mainRow.createCell(0);
        headerCell.setCellValue("სკოლა პანსიონ იბ მთიები - კლასი " + academyClass.getClassName() + " - " + adjustMonth(date.getMonth() + 1));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        boolean isTransit = gradeTypePrefix.equals("TRANSIT");
        if (isTransit) {

            // First header row
            Row headerRow1 = sheet.createRow(1);

            // Creating and merging the first header cell
            Cell firstCell1 = headerRow1.createCell(0);
            firstCell1.setCellValue("მოსწავლის გვარი, სახელი");

            // Creating and merging cells for "შემაჯამებელი დავალება I"
            Cell summaryCell1 = headerRow1.createCell(1);
            summaryCell1.setCellValue("შემაჯამებელი დავალება I");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 5));

            // Creating and merging cells for "სკოლის სამუშაო II"
            Cell schoolWorkCell = headerRow1.createCell(6);
            schoolWorkCell.setCellValue("სკოლის სამუშაო II");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 6, 15));

            // Creating the second header row
            Row headerRow2 = sheet.createRow(2);

            // Sub-headers for "მოსწავლის გვარი, სახელი"
            Cell firstCell2 = headerRow2.createCell(0);
            firstCell2.setCellValue("მოსწავლის გვარი, სახელი");

            // Sub-headers for "შემაჯამებელი დავალება I"
            headerRow2.createCell(1).setCellValue("1");
            headerRow2.createCell(2).setCellValue("2");
            headerRow2.createCell(3).setCellValue("აღდგენა");
            headerRow2.createCell(4).setCellValue("თვის ნიშანი");
            headerRow2.createCell(5).setCellValue("%");

            // Sub-headers for "სკოლის სამუშაო II"
            headerRow2.createCell(6).setCellValue("1");
            headerRow2.createCell(7).setCellValue("2");
            headerRow2.createCell(8).setCellValue("3");
            headerRow2.createCell(9).setCellValue("4");
            headerRow2.createCell(10).setCellValue("5");
            headerRow2.createCell(11).setCellValue("6");
            headerRow2.createCell(12).setCellValue("7");
            headerRow2.createCell(13).setCellValue("8");
            headerRow2.createCell(14).setCellValue("თვის ნიშანი");
            headerRow2.createCell(15).setCellValue("%");
            headerRow2.createCell(16).setCellValue("საერთო ქულა");
        } else {
            Row headerRow = sheet.createRow(1);
            Cell firstCell = headerRow.createCell(0);
            firstCell.setCellValue("მოსწავლის გვარი, სახელი");

            Cell summaryCell = headerRow.createCell(1);
            summaryCell.setCellValue("შემაჯამებელი დავალება I");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 5));

            Cell shemokmedobitoba = headerRow.createCell(6);
            shemokmedobitoba.setCellValue("შემოქმედობითობა II");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 6, 12));

            Cell homework = headerRow.createCell(13);
            homework.setCellValue("საშინაო დავალება III");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 13, 18));

            Row subHeaderRow = sheet.createRow(2);
// Sub-headers for "შემაჯამებელი დავალება I"
            subHeaderRow.createCell(1).setCellValue("1");
            subHeaderRow.createCell(2).setCellValue("2");
            subHeaderRow.createCell(3).setCellValue("აღდგენა");
            subHeaderRow.createCell(4).setCellValue("თვის ნიშანი");
            subHeaderRow.createCell(5).setCellValue("50%");

// Sub-headers for "შემოქმედებითობა II"
            subHeaderRow.createCell(6).setCellValue("1");
            subHeaderRow.createCell(7).setCellValue("2");
            subHeaderRow.createCell(8).setCellValue("3");
            subHeaderRow.createCell(9).setCellValue("4");
            subHeaderRow.createCell(10).setCellValue("5");
            subHeaderRow.createCell(11).setCellValue("თვის ნიშანი");
            subHeaderRow.createCell(12).setCellValue("25%");

// Sub-headers for "საშინაო დავალება III"
            subHeaderRow.createCell(13).setCellValue("1");
            subHeaderRow.createCell(14).setCellValue("2");
            subHeaderRow.createCell(15).setCellValue("3");
            subHeaderRow.createCell(16).setCellValue("4");
            subHeaderRow.createCell(17).setCellValue("თვის ნიშანი");
            subHeaderRow.createCell(18).setCellValue("25%");

            subHeaderRow.createCell(19).setCellValue("თვის ქულა");

        }

        GradeType[] gradeTypeArray = isTransit ?
            new GradeType[]{
                    GradeType.TRANSIT_SUMMARY_ASSIGMENT_1,
                    GradeType.TRANSIT_SUMMARY_ASSIGMENT_2,
                    GradeType.TRANSIT_SUMMARY_ASSIGMENT_RESTORATION,
                    GradeType.TRANSIT_SUMMARY_ASSIGMENT_MONTH,
                    GradeType.TRANSIT_SUMMARY_ASSIGMENT_PERCENT,
                    GradeType.TRANSIT_SCHOOL_WORK_1,
                    GradeType.TRANSIT_SCHOOL_WORK_2,
                    GradeType.TRANSIT_SCHOOL_WORK_3,
                    GradeType.TRANSIT_SCHOOL_WORK_4,
                    GradeType.TRANSIT_SCHOOL_WORK_5,
                    GradeType.TRANSIT_SCHOOL_WORK_6,
                    GradeType.TRANSIT_SCHOOL_WORK_7,
                    GradeType.TRANSIT_SCHOOL_WORK_8,
                    GradeType.TRANSIT_SCHOOL_WORK_MONTH,
                    GradeType.TRANSIT_SCHOOL_WORK_MONTH_PERCENT,
                    GradeType.TRANSIT_SCHOOL_COMPLETE_MONTHLY
                }
        :
        new GradeType[]{
                GradeType.GENERAL_SUMMARY_ASSIGMENT_1,
                GradeType.GENERAL_SUMMARY_ASSIGMENT_2,
                GradeType.GENERAL_SUMMARY_ASSIGMENT_RESTORATION,
                GradeType.GENERAL_SUMMARY_ASSIGMENT_MONTH,
                GradeType.GENERAL_SUMMARY_ASSIGMENT_PERCENT,
                GradeType.GENERAL_SCHOOL_WORK_1,
                GradeType.GENERAL_SCHOOL_WORK_2,
                GradeType.GENERAL_SCHOOL_WORK_3,
                GradeType.GENERAL_SCHOOL_WORK_4,
                GradeType.GENERAL_SCHOOL_WORK_5,
                GradeType.GENERAL_SCHOOL_WORK_MONTH,
                GradeType.GENERAL_SCHOOL_WORK_PERCENT,
                GradeType.GENERAL_HOMEWORK_WRITE_ASSIGMENT_1,
                GradeType.GENERAL_HOMEWORK_WRITE_ASSIGMENT_2,
                GradeType.GENERAL_HOMEWORK_WRITE_ASSIGMENT_3,
                GradeType.GENERAL_HOMEWORK_WRITE_ASSIGMENT_4,
                GradeType.GENERAL_HOMEWORK_MONTHLY,
                GradeType.GENERAL_HOMEWORK_PERCENT,
                GradeType.GENERAL_COMPLETE_MONTHLY
        };

        for (int i = 0; i < list.size(); i++) {
            Row studentRow = sheet.createRow(i + 3);
            var student = ((GradeWrapperByStudent) list.get(i)).getStudent();
            var grades = list.get(i).getGrades().stream()
                    .collect(Collectors.toMap(
                            GradeDTO::getGradeType,
                            obj -> obj.getValue() == null ? BigDecimal.ZERO : obj.getValue(),
                            (existing, replacement) -> existing
                    ));;
            for (int j = 0; j < gradeTypeArray.length + 1; j++) {
                if (j == 0) {
                    Cell dataCell = studentRow.createCell(j);
                    dataCell.setCellValue((i+1) + ". " +  student.getLastName() + " " + student.getFirstName());
                } else {
                    Cell dataCell = studentRow.createCell(j);
                    dataCell.setCellValue(adjustGradeValueDashboard(grades.get(gradeTypeArray[j-1].toString())));
                }
            }
        }

//        int teacherIndex = list.size() + 2;
//        Row teacherRow = sheet.createRow(teacherIndex);
//        Cell teacgerCell = teacherRow.createCell(0);
//        teacgerCell.setCellValue("პედაგოგი");
//
//        for (int i = 0; i < subjectComponentWrappersList.size() ; i++) {
//            Cell dataCell = teacherRow.createCell(i + 1);
//            dataCell.setCellValue(subjectComponentWrappersList.get(i).getSubject().getTeacher());
//        }

        for (int i = 0; i <= sheet.getRow(1).getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
        }

        for (int i = 0; i <= sheet.getRow(2).getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
        }

        CellStyle thickBorderStyle = workbook.createCellStyle();
        thickBorderStyle.setBorderTop(BorderStyle.THIN);
        thickBorderStyle.setBorderBottom(BorderStyle.THIN);
        thickBorderStyle.setBorderLeft(BorderStyle.THIN);
        thickBorderStyle.setBorderRight(BorderStyle.THIN);
        thickBorderStyle.setAlignment(HorizontalAlignment.CENTER);

        // Apply the thick border style to all cells in the sheet
        for (Row row : sheet) {
            for (Cell cell : row) {
                cell.setCellStyle(thickBorderStyle);
            }
        }

        // Set the response headers for Excel download
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=exported_data.xlsx");

        // Write the workbook to the response stream
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private String adjustGradeValueDashboard(BigDecimal value) {
        if (value == null || Objects.equals(value.toString(), "0")) {
            return " ";
        }
        if (value.toString().startsWith("-50")) {
            return "ჩთ";
        }
        BigDecimal val = new BigDecimal(value.toString());
        return String.valueOf(val.longValue());
    }

    private String adjustGradeValue(SubjectComponentWrapper obj, Boolean isDecimalSystem) {
        Object value = obj.getValue();
        if (value == null || Objects.equals(value.toString(), "0")) {
            return " ";
        }
        if (value.toString().startsWith("-50")) {
            return "ჩთ";
        }
        BigDecimal val = new BigDecimal(value.toString());
        String subjName = obj.getSubject().getName();
        return isDecimalSystem
                && !subjName.equalsIgnoreCase("rating")
                && !subjName.equalsIgnoreCase("behaviour")
                && !subjName.equalsIgnoreCase("absence") ? String.valueOf(val.add(new BigDecimal(3)).longValue()) : String.valueOf(val.longValue());
    }

    private String adjustSemesterGradeValue(SubjectComponentWrapper obj, Boolean isDecimalSystem,
                                            Integer[] grades, int index) {
        Map<Integer, BigDecimal> map = (Map<Integer, BigDecimal>) obj.getValue();
        String subjectName = obj.getSubject().getName();
        if (map == null) {
            return " ";
        }
        BigDecimal value = null;
        if (subjectName.equals("behaviour1") || subjectName.equals("behaviour2")) {
            value = map.get(-7);
        }

        if (subjectName.equals("absence1") || subjectName.equals("absence2")) {
            value = map.get(-9);
        }

        if (value == null) value = map.get(grades[index]);

        if (value == null || value.toString().equals("0")) {
            return " ";
        }

        if (value.toString().startsWith("-50")) {
            return "ჩთ";
        }
        return isDecimalSystem ? String.valueOf(value.add(new BigDecimal(3)).longValue()) : String.valueOf(value.longValue());
    }

    private String adjustAnualGradeValue(SubjectComponentWrapper obj, Boolean isDecimalSystem,
                                            Integer[] grades, int index) {
        Map<Integer, BigDecimal> map = (Map<Integer, BigDecimal>) obj.getValue();
        String subjectName = obj.getSubject().getName();
        if (map == null) {
            return " ";
        }
        BigDecimal value = null;
        if (subjectName.equals("behaviour1")) {
            BigDecimal first = map.get(-7);
            BigDecimal second = map.get(-8);
            value = first != null && !first.equals(BigDecimal.ZERO) ?
                    second != null && !second.equals(BigDecimal.ZERO) ? first.add(second).divide(new BigDecimal(2), RoundingMode.HALF_UP) : first
                    :
                    second != null && !second.equals(BigDecimal.ZERO) ? second : BigDecimal.ZERO;
        }

        if (subjectName.equals("absence1")) {
            BigDecimal first = map.get(-9);
            BigDecimal second = map.get(-10);
            value = first != null && !first.equals(BigDecimal.ZERO) ?
                    second != null && !second.equals(BigDecimal.ZERO) ? first.add(second).divide(new BigDecimal(2), RoundingMode.HALF_UP) : first
                    :
                    second != null && !second.equals(BigDecimal.ZERO) ? second : BigDecimal.ZERO;
        }

        if (grades[index] == 5) {
            BigDecimal first = map.get(1);
            BigDecimal second = map.get(2);
            value = first != null && !first.equals(BigDecimal.ZERO) ?
                    second != null && !second.equals(BigDecimal.ZERO) ? first.add(second).divide(new BigDecimal(2), RoundingMode.HALF_UP) : first
                    :
                    second != null && !second.equals(BigDecimal.ZERO) ? second : BigDecimal.ZERO;
        }

        if (value == null) {
            value = map.get(grades[index]);
        }

        if (value == null || value.toString().equals("0")) {
            return " ";
        }

        if (value.toString().startsWith("-50")) {
            return "ჩთ";
        }
        return isDecimalSystem ? String.valueOf(value.add(new BigDecimal(3)).longValue()) : String.valueOf(value.longValue());
    }


    private String adjustName(String name) {
        switch (name) {
            case "rating":
                return "რეიტინგი";
            case "behaviour":
            case "behaviour1":
            case "behaviour2":
                return "ეთიკური ნორმა";
            case "absence":
            case "absence1":
            case "absence2":
                return "გაცდენილი საათები";
            default:
                return name;
        }
    }

    private String adjustMonthNamesForSemesters(Integer key) {
        switch (key) {
            case -1:
                return "სემესტრული";
            case -2:
                return "შემოქმედობითობა";
            case -3:
            case -5:
                return "დიაგნოსტიკური 1";
            case -4:
            case -6:
                return "დიაგნოსტიკური 2";
            case 9:
                return "სექტემბერი-ოქტომბერი";
            case 11:
                return "ნოემბერი";
            case 12:
                return "დეკემბერი";
            case 1:
                return "იანვარი-თებერვალი";
            case 3:
                return "მარტი";
            case 4:
                return "აპრილი";
            case 5:
                return "მაისი";
            default:
                return "თვე";
        }
    }


    private String adjustMonthNamesForAnual(Integer key) {
        switch (key) {
            case 1:
                return "I სემესტრი";
            case 2:
                return "II სემესტრი";
            case 3:
                return "გამოცდა";
            case 4:
                return "საბოლოო ქულა";
            case 5:
                return "წლიური";
            default:
                return "თვე";
        }
    }

    private String adjustMonth(int i) {
        switch (i) {
            case 1:
            case 2:
                return "იანვარი-თებერვალი";
            case 3:
                return "მარტი";
            case 4:
                return "აპრილი";
            case 5:
                return "მაისი";
            case 6:
                return "ივნისი";
            case 9:
            case 10:
                return "სექტემბერი-ოქტომბერი";
            case 11:
                return "ნოემბერი";
            case 12:
                return "დეკემბერი";
            default:
                return "თვე";
        }
    }
}