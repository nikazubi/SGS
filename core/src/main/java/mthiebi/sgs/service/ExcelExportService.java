package mthiebi.sgs.service;

import mthiebi.sgs.dto.GradeComponentWrapper;
import mthiebi.sgs.dto.GradeWrapperDto;
import mthiebi.sgs.models.AcademyClass;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Date;
import java.util.List;

/**
 * Builds the various grade-export spreadsheets. The controller is responsible for fetching the grade data
 * and for writing the workbook to the HTTP response; this service only turns already-fetched data into a
 * POI {@link Workbook}. The cell layout / index magic is intentional and matches what the school expects in
 * the exported files.
 */
public interface ExcelExportService {

    Workbook buildMonthlyExcel(List<GradeComponentWrapper> list, AcademyClass academyClass, Date date, boolean isDecimalSystem);

    Workbook buildSemesterExcel(List<GradeComponentWrapper> list, AcademyClass academyClass, String yearRange, String component, boolean isDecimalSystem);

    Workbook buildAnnualExcel(List<GradeComponentWrapper> list, AcademyClass academyClass, String yearRange, boolean isDecimalSystem);

    Workbook buildDashboardExcel(List<GradeWrapperDto> list, AcademyClass academyClass, Date date, String gradeTypePrefix);
}
