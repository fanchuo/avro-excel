package org.fanchuo.avroexcel.excelutil;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExcelSheetReader {
    private final Sheet sheet;
    private final Map<Point, CellRangeAddress> idxRange = new HashMap<>();

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

    public ExcelSheetReader(Sheet sheet) {
        this.sheet = sheet;
        makeIdxRange(this.sheet.getMergedRegions());
    }

    public Cell getCell(int col, int row) {
        Row r = this.sheet.getRow(row);
        if (r==null) return null;
        return r.getCell(col);
    }

    public CellRangeAddress getRangeAt(int col, int row) {
        return this.idxRange.get(new Point(col, row));
    }

    private void makeIdxRange(List<CellRangeAddress> mergedRegions) {
        for (CellRangeAddress range : mergedRegions) {
            Point point = new Point(range.getFirstColumn(), range.getFirstRow());
            idxRange.put(point, range);
        }
    }

    public static ExcelSheetReader loadSheet(InputStream is, String sheetName) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(sheetName);
            return new ExcelSheetReader(sheet);
        }
    }
}
