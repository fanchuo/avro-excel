package org.fanchuo.avroexcel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelWorkbookDescriptor {
    public static List<String> dump(File file, String sheetName) throws IOException {
        List<String> out = new ArrayList<>();
        try (
                InputStream is = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(is)
        ) {
            Sheet sheet = workbook.getSheet(sheetName);
            int rowNum = 0;
            Iterator<Row> rowIt = sheet.rowIterator();
            while (rowIt.hasNext()) {
                Row row = rowIt.next();
                Iterator<Cell> cellIt = row.cellIterator();
                int cellNum=0;
                while (cellIt.hasNext()) {
                    Cell cell = cellIt.next();
                    out.add(String.format("Cell: %d - %d = %s", rowNum, cellNum, cell));
                    cellNum++;
                }
                rowNum++;
            }
            for (CellRangeAddress cellRangeAddress : sheet.getMergedRegions()) {
                out.add(cellRangeAddress.formatAsString());
            }
        }
        return out;
    }
}
