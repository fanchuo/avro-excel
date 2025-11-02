package org.example;

import org.apache.avro.generic.GenericRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkbookWriter implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkbookWriter.class);

    private final OutputStream outputStream;
    private final Workbook workbook = new XSSFWorkbook();
    private final Sheet sheet = workbook.createSheet("Avro Data");
    private final CellStyle defaultMergeStyle = this.workbook.createCellStyle();

    public WorkbookWriter(File excelFile) throws IOException {
        this(new FileOutputStream(excelFile));
    }

    public WorkbookWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.defaultMergeStyle.setVerticalAlignment(VerticalAlignment.TOP);
    }

    private Row getRow(int row) {
        Row r = this.sheet.getRow(row);
        if (r==null) return this.sheet.createRow(row);
        return r;
    }

    private Cell getCell(int row, int col) {
        Row r = getRow(row);
        Cell c = r.getCell(col);
        if (c==null) return r.createCell(col);
        return c;
    }

    public void writeHeaders(int col, int row, org.example.HeaderInfo headerInfo, int maxDepth) {
        Cell c = getCell(row, col);
        c.setCellValue(headerInfo.text);
        int lastCol = col + headerInfo.colSpan - 1;
        int lastRow = row;
        if (headerInfo.subHeaders != null) {
            int offset = col;
            for (org.example.HeaderInfo subHeader : headerInfo.subHeaders) {
                writeHeaders(offset, row+(headerInfo.text==null?0:1), subHeader, maxDepth);
                offset += subHeader.colSpan;
            }
        } else {
            lastRow = maxDepth-1;
        }
        if (headerInfo.text != null && (col<lastCol || row<lastRow)) {
            CellRangeAddress range = new CellRangeAddress(row, lastRow, col, lastCol);
            sheet.addMergedRegion(range);
            c.setCellStyle(this.defaultMergeStyle);
        }
    }

    public void writeRecord(GenericRecord record, org.example.HeaderInfo headerInfo, org.example.RecordGeometry recordGeometry, int col, int row, int maxDepth) {
        LOGGER.info("record: {}", record);
        LOGGER.info("recordGeometry: {}", recordGeometry);
        int offset = col;
        for (org.example.HeaderInfo subHeader : headerInfo.subHeaders) {
            if (record.hasField(subHeader.text)) {
                writeObject(record.get(subHeader.text), subHeader, recordGeometry.subRecords.get(subHeader.text), offset, row, maxDepth);
            }
            offset += subHeader.colSpan;
        }
    }

    private void writeIterable(Iterable<?> lst, org.example.HeaderInfo headerInfo, org.example.RecordGeometry recordGeometry, int col, int row) {
        int i=0;
        int offsetRow=row;
        for (Object o : lst) {
            org.example.RecordGeometry subList = recordGeometry.subLists.get(i++);
            int end = offsetRow + subList.rowSpan;
            writeObject(o, headerInfo, subList, col, offsetRow, end);
            offsetRow = end;
        }
    }

    public void writeList(List<?> lst, org.example.HeaderInfo headerInfo, org.example.RecordGeometry recordGeometry, int col, int row, int maxDepth) {
        int offset = col;
        for (org.example.HeaderInfo subHeader : headerInfo.subHeaders) {
            if ("*size".equals(subHeader.text)) {
                writeObject(lst.size(), subHeader, org.example.RecordGeometry.ATOM, offset, row, maxDepth);
            } else if ("*".equals(subHeader.text)) {
                writeIterable(lst, subHeader, recordGeometry, offset, row);
            }
            offset += subHeader.colSpan;
        }
    }

    public void writeMap(Map<?, ?> map, org.example.HeaderInfo headerInfo, org.example.RecordGeometry recordGeometry, int col, int row, int maxDepth) {
        int offset = col;
        List<Object> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }
        for (org.example.HeaderInfo subHeader : headerInfo.subHeaders) {
            if ("#size".equals(subHeader.text)) {
                writeObject(map.size(), subHeader, org.example.RecordGeometry.ATOM, offset, row, maxDepth);
            } else if ("#k".equals(subHeader.text)) {
                writeIterable(keys, subHeader, recordGeometry, offset, row);
            } else if ("#v".equals(subHeader.text)) {
                writeIterable(values, subHeader, recordGeometry, offset, row);
            }
            offset += subHeader.colSpan;
        }
    }

    public void writeObject(Object value, org.example.HeaderInfo headerInfo, org.example.RecordGeometry recordGeometry, int col, int row, int maxDepth) {
        if (value instanceof GenericRecord) {
            writeRecord((GenericRecord) value, headerInfo, recordGeometry, col, row, maxDepth);
            return;
        }
        if (value instanceof List) {
            writeList((List<?>) value, headerInfo, recordGeometry, col, row, maxDepth);
            return;
        }
        if (value instanceof Map) {
            writeMap((Map<?, ?>) value, headerInfo, recordGeometry, col, row, maxDepth);
            return;
        }
        if (value == null) {
            int lastRow = maxDepth-1;
            int lastCol = col-1+headerInfo.colSpan;
            if (headerInfo.text!=null && (col!=lastCol || row!=lastRow)) {
                CellRangeAddress range = new CellRangeAddress(row, lastRow, col, lastCol);
                sheet.addMergedRegion(range);
            }
            return;
        }
        // case of a scalar value
        int offset = col;
        if (headerInfo.subHeaders != null) {
            for (org.example.HeaderInfo subHeader : headerInfo.subHeaders) {
                if (".value".equals(subHeader.text)) break;
                offset += subHeader.colSpan;
            }
        }
        Cell c = getCell(row, offset);
        if (value instanceof Number) {
            c.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            c.setCellValue((Boolean) value);
        } else if (value instanceof LocalDate) {
            c.setCellValue((LocalDate) value);
        } else if (value instanceof LocalDateTime) {
            c.setCellValue((LocalDateTime) value);
        } else {
            c.setCellValue(String.valueOf(value));
        }
        if (row+1 != maxDepth) {
            CellRangeAddress range = new CellRangeAddress(row, maxDepth-1, offset, offset);
            this.sheet.addMergedRegion(range);
            c.setCellStyle(this.defaultMergeStyle);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            workbook.write(this.outputStream);
        } finally {
            this.outputStream.close();
        }
    }
}
