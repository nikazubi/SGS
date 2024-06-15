package mthiebi.sgs.controllers;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.GradeComponentWrapper;
import mthiebi.sgs.dto.SubjectComponentWrapper;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.models.Subject;
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

import static mthiebi.sgs.utils.ExcelUtils.sortGradeArrayForExcel;
import static mthiebi.sgs.utils.ExcelUtils.subjectPattern;

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
            Cell dataCell = headerRow.createCell(cc + 1);
            String subjectName = subjectComponentWrappersList.get(i).getSubject().getName();
            dataCell.setCellValue(adjustName(subjectName));
            subjectCount++;
            if (!subjectName.equals("behaviour1") && !subjectName.equals("behaviour2")) {
                sheet.addMergedRegion(new CellRangeAddress(1, 1, (cc + 1) , cc + grades.length));
                cc += grades.length;
            }
        }

        Row monthRows = sheet.createRow(2);
        Cell monthRowsFirstCell = monthRows.createCell(0);


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
            Cell dataCell = headerRow.createCell(cc + 1);
            String subjectName = subjectComponentWrappersList.get(i).getSubject().getName();
            dataCell.setCellValue(adjustName(subjectName));
            subjectCount++;
            if (!subjectName.equals("behaviour1")) {
                sheet.addMergedRegion(new CellRangeAddress(1, 1, (cc + 1) , cc + grades.length));
                cc += grades.length;
            }
        }

        Row monthRows = sheet.createRow(2);
        Cell monthRowsFirstCell = monthRows.createCell(0);


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

        BigDecimal value = subjectName.equals("behaviour1") || subjectName.equals("behaviour2") ? map.get(-7) : map.get(grades[index]);


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