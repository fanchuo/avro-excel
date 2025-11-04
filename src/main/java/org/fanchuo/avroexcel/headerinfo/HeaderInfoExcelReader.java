package org.fanchuo.avroexcel.headerinfo;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class HeaderInfoExcelReader {
    private static class Point {
        final int col;
        final int row;

        Point(int col, int row) {
            this.col = col;
            this.row = row;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return col == point.col && row == point.row;
        }

        @Override
        public int hashCode() {
            return Objects.hash(col, row);
        }
    }

    private static Cell getCell(Sheet sheet, int col, int row) {
        Row r = sheet.getRow(row);
        if (r==null) return null;
        return r.getCell(col);
    }

    public static HeaderInfo visitInputStream(InputStream is, String sheetName, int col, int row) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(sheetName);
            return visitSheet(sheet, col, row);
        }
    }

    public static HeaderInfo visitSheet(Sheet sheet, int col, int row) {
        Map<Point, CellRangeAddress> idxRange = makeIdxRange(sheet.getMergedRegions());
        List<HeaderInfo> subHeaders = new ArrayList<>();
        int x = col;
        int colSpan = 0;
        int rowSpan = 0;
        while (true) {
            HeaderInfo subHeader = visitSheet(sheet, x, row, idxRange);
            if (subHeader==null) break;
            x += subHeader.colSpan;
            subHeaders.add(subHeader);
            colSpan += subHeader.colSpan;
            rowSpan = Math.max(rowSpan, subHeader.rowSpan);
        }
        return new HeaderInfo(null, subHeaders, colSpan, rowSpan+1, false);
    }

    private static HeaderInfo visitSheet(Sheet sheet, int col, int row, Map<Point, CellRangeAddress> idxRange) {
        Cell cell = getCell(sheet, col, row);
        if (cell==null) return null;
        if (cell.getCellType()== CellType.BLANK) return null;
        CellRangeAddress range = idxRange.get(new Point(col, row));
        if (range!=null && (range.getFirstColumn()!=range.getLastColumn())) {
            List<HeaderInfo> subHeaders = new ArrayList<>();
            int colSpan = range.getLastColumn() - range.getFirstColumn() + 1;
            int rowSpan = 0;
            int x = col;
            int end = x + colSpan;
            while (x<end) {
                HeaderInfo subHeader = visitSheet(sheet, x, row+1, idxRange);
                if (subHeader==null) break;
                subHeaders.add(subHeader);
                x += subHeader.colSpan;
                rowSpan = Math.max(rowSpan, subHeader.rowSpan);
            }
            return new HeaderInfo(cell.getStringCellValue(), subHeaders, colSpan, rowSpan+1, false);
        }
        return new HeaderInfo(cell.getStringCellValue(), null, 1, 1, false);
    }

    private static Map<Point, CellRangeAddress> makeIdxRange(List<CellRangeAddress> mergedRegions) {
        Map<Point, CellRangeAddress> idxRange = new HashMap<>();
        for (CellRangeAddress range : mergedRegions) {
            Point point = new Point(range.getFirstColumn(), range.getFirstRow());
            idxRange.put(point, range);
        }
        return idxRange;
    }
}
