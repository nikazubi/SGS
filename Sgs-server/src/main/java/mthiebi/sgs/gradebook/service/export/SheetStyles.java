package mthiebi.sgs.gradebook.service.export;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * The look of an exported sheet.
 * <p>
 * Created once per workbook. The legacy exports built a CellStyle and then
 * assigned it in a loop over every cell in the sheet, which is both slow and a
 * POI trap: styles are a limited workbook-level resource, and creating them per
 * cell exhausts them on a large sheet.
 */
class SheetStyles {

    final CellStyle title;
    final CellStyle header;
    final CellStyle cell;
    final CellStyle studentName;
    final CellStyle teacher;

    SheetStyles(Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setAlignment(HorizontalAlignment.CENTER);

        header = bordered(workbook);
        header.setFont(headerFont);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setWrapText(true);

        cell = bordered(workbook);
        cell.setAlignment(HorizontalAlignment.CENTER);

        studentName = bordered(workbook);
        studentName.setAlignment(HorizontalAlignment.LEFT);

        teacher = bordered(workbook);
        teacher.setFont(headerFont);
        teacher.setAlignment(HorizontalAlignment.CENTER);
        teacher.setWrapText(true);
    }

    private static CellStyle bordered(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
