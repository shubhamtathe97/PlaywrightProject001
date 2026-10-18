package Utils;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;


//Reads test data from an Excel file — single cells, full rows, or whole columns.
public class ExcelUtils {

    private static final Logger log = LogManager.getLogger(ExcelUtils.class);

    private ExcelUtils() { /* utility class */ }

    // ── Core data reader ──────────────────────────


    //Reads all rows of a sheet into a list, using the first row as column headers
    public static List<Map<String, String>> getSheetData(String sheetName) {
        return getSheetData(FrameworkConstants.EXCEL_PATH, sheetName);
    }


    //Same, but lets you specify a different Excel file too.
    public static List<Map<String, String>> getSheetData(String filePath, String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();

        try (FileInputStream fis    = new FileInputStream(filePath);
             Workbook        wb     = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("Sheet not found: '" + sheetName + "' in " + filePath);
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                log.warn("Sheet '{}' is empty or has no header row = "+ sheetName);
                return data;
            }

            // Build header list
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell));
            }

            // Build data rows
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                    Cell cell  = row.getCell(colIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowMap.put(headers.get(colIdx), getCellValue(cell));
                }
                data.add(rowMap);
            }

            log.info("Read {} data rows from sheet : " + data.size()+" : "+ sheetName+ " : "+ filePath);

        } catch (IOException e) {
            log.error("Failed to read Excel file :"+ filePath+" : "+ e.getMessage(), e);
            throw new RuntimeException("Excel read failed: " + e.getMessage(), e);
        }

        return data;
    }


    //Reads one specific cell by sheet, row number, and column name.
    public static String getCellData(String sheetName, int rowIndex, String columnHeader) {
        List<Map<String, String>> sheetData = getSheetData(sheetName);
        if (rowIndex >= sheetData.size()) {
            throw new IndexOutOfBoundsException(
                    "Row index " + rowIndex + " exceeds available rows (" + sheetData.size() + ") in sheet: " + sheetName);
        }
        Map<String, String> row = sheetData.get(rowIndex);
        if (!row.containsKey(columnHeader)) {
            throw new RuntimeException("Column not found: '" + columnHeader + "' in sheet: " + sheetName);
        }
        return row.get(columnHeader);
    }


    //Returns every value from one column in a sheet.
    public static List<String> getColumnData(String sheetName, String columnHeader) {
        List<String> values = new ArrayList<>();
        for (Map<String, String> row : getSheetData(sheetName)) {
            values.add(row.getOrDefault(columnHeader, ""));
        }
        return values;
    }

    //Converts sheet data into the format TestNG needs for a @DataProvider.
    public static Object[][] getDataProvider(String sheetName) {
        List<Map<String, String>> rows = getSheetData(sheetName);
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            data[i][0] = rows.get(i);
        }
        return data;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {

            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }

                double val = cell.getNumericCellValue();

                // Return as int if it's a whole number (e.g. 1.0 -> "1")
                if (val == Math.floor(val)) {
                    return String.valueOf((long) val);
                }

                return String.valueOf(val);

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                return cell.getCellFormula();

            case BLANK:
                return "";

            default:
                return "";
        }
    }
}
