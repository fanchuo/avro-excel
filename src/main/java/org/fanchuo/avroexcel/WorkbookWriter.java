package org.fanchuo.avroexcel;

import org.apache.avro.generic.GenericRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class WorkbookWriter implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkbookWriter.class);

    private final OutputStream outputStream;
    private final Workbook workbook = new XSSFWorkbook();
    private final Sheet sheet;
    private final CellStyle defaultMergeStyle = this.workbook.createCellStyle();
    private final CellStyle defaultDateStyle = this.workbook.createCellStyle();
    private final CellStyle defaultDatetimeStyle = this.workbook.createCellStyle();

    public WorkbookWriter(File excelFile, String sheetName) throws IOException {
        this(new FileOutputStream(excelFile), sheetName);
    }

    public WorkbookWriter(OutputStream outputStream, String sheetName) {
        this.sheet = workbook.createSheet(sheetName);
        this.outputStream = outputStream;
        this.defaultMergeStyle.setVerticalAlignment(VerticalAlignment.TOP);
        this.defaultDateStyle.setVerticalAlignment(VerticalAlignment.TOP);
        this.defaultDateStyle.setDataFormat((short) 14);
        this.defaultDatetimeStyle.setVerticalAlignment(VerticalAlignment.TOP);
        this.defaultDatetimeStyle.setDataFormat((short) 22);
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

    public void writeHeaders(int col, int row, HeaderInfo headerInfo, int maxDepth) {
        Cell c = getCell(row, col);
        c.setCellValue(headerInfo.text);
        int lastCol = col + headerInfo.colSpan - 1;
        int lastRow = row;
        if (headerInfo.subHeaders != null) {
            int offset = col;
            for (HeaderInfo subHeader : headerInfo.subHeaders) {
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

    public void writeRecord(GenericRecord record, HeaderInfo headerInfo, RecordGeometry recordGeometry, int col, int row, int maxDepth) {
        LOGGER.info("record: {}", record);
        LOGGER.info("recordGeometry: {}", recordGeometry);
        int offset = col;
        for (HeaderInfo subHeader : headerInfo.subHeaders) {
            if (record.hasField(subHeader.text)) {
                writeObject(record.get(subHeader.text), subHeader, recordGeometry.subRecords.get(subHeader.text), offset, row, maxDepth);
            }
            offset += subHeader.colSpan;
        }
    }

    private void writeIterable(Iterable<?> lst, HeaderInfo headerInfo, RecordGeometry recordGeometry, int col, int row) {
        int i=0;
        int offsetRow=row;
        for (Object o : lst) {
            RecordGeometry subList = recordGeometry.subLists.get(i++);
            int end = offsetRow + subList.rowSpan;
            writeObject(o, headerInfo, subList, col, offsetRow, end);
            offsetRow = end;
        }
    }

    public void writeList(List<?> lst, HeaderInfo headerInfo, RecordGeometry recordGeometry, int col, int row, int maxDepth) {
        int offset = col;
        for (HeaderInfo subHeader : headerInfo.subHeaders) {
            if ("*size".equals(subHeader.text)) {
                writeObject(lst.size(), subHeader, RecordGeometry.ATOM, offset, row, maxDepth);
            } else if ("*".equals(subHeader.text)) {
                writeIterable(lst, subHeader, recordGeometry, offset, row);
            }
            offset += subHeader.colSpan;
        }
    }

    public void writeMap(Map<?, ?> map, HeaderInfo headerInfo, RecordGeometry recordGeometry, int col, int row, int maxDepth) {
        int offset = col;
        List<Object> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        SortedMap<?, ?> sorted = new TreeMap<>(map);
        for (Map.Entry<?, ?> entry : sorted.entrySet()) {
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }
        for (HeaderInfo subHeader : headerInfo.subHeaders) {
            if ("#size".equals(subHeader.text)) {
                writeObject(map.size(), subHeader, RecordGeometry.ATOM, offset, row, maxDepth);
            } else if ("#k".equals(subHeader.text)) {
                writeIterable(keys, subHeader, recordGeometry, offset, row);
            } else if ("#v".equals(subHeader.text)) {
                writeIterable(values, subHeader, recordGeometry, offset, row);
            }
            offset += subHeader.colSpan;
        }
    }

    public void writeObject(Object value, HeaderInfo headerInfo, RecordGeometry recordGeometry, int col, int row, int maxDepth) {
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
            for (HeaderInfo subHeader : headerInfo.subHeaders) {
                if (".value".equals(subHeader.text)) break;
                offset += subHeader.colSpan;
            }
        }
        Cell c = getCell(row, offset);
        if (value instanceof Number) {
            c.setCellValue(((Number) value).doubleValue());
            c.setCellStyle(this.defaultMergeStyle);
        } else if (value instanceof Boolean) {
            c.setCellValue((Boolean) value);
            c.setCellStyle(this.defaultMergeStyle);
        } else if (value instanceof LocalDate) {
            c.setCellValue((LocalDate) value);
            c.setCellStyle(this.defaultDateStyle);
        } else if (value instanceof LocalDateTime) {
            c.setCellValue((LocalDateTime) value);
            c.setCellStyle(this.defaultDatetimeStyle);
        } else {
            c.setCellValue(String.valueOf(value));
            c.setCellStyle(this.defaultMergeStyle);
        }
        if (row+1 != maxDepth) {
            CellRangeAddress range = new CellRangeAddress(row, maxDepth-1, offset, offset);
            this.sheet.addMergedRegion(range);
        }
    }

    public void finalize(int col, int width) {
        for (int i=col; i<col+width; i++) {
            this.sheet.autoSizeColumn(i, true);
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
