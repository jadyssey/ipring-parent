package org.ipring.excel;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 辅助 excel 导入和导出的工具类.
 *
 * @author liuguangjin
 * @date 2022/9/30
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcelOperateUtils {

    /**
     * 匹配页名不能包含的字符
     */
    private static final Pattern SHEET_NAME_ILLEGAL_PATTERN = Pattern.compile("[：:？/?*\\[\\]\\\\]+");

    /**
     * 页名最大长度
     */
    private static final int SHEET_NAME_LIMIT = 31;

    /**
     * 将 excel 第一页除首行外每一行解析为对象并返回列表
     *
     * @param file  excel 文件, 列号需要与对象的注解对应
     * @param clazz 目标对象的类
     * @param <T>   excel 每一行对应的目标对象, 对应的属性需要注解 {@link ExcelColumn}
     * @return 解析得到的对象列表
     */
    public static <T> List<T> importToList(InputStreamSource file, Class<T> clazz) {
        XSSFSheet sheet;
        try (InputStream in = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(in);
            sheet = workbook.getSheetAt(0);
        } catch (IOException e) {
            throw new ValidationException("excel 文件解析失败");
        }

        // 得到列名属性列表
        List<Field> columns = getColumns(clazz);
        int totalColNum = columns.size();
        int totalRowNum = sheet.getLastRowNum();
        List<T> list = new ArrayList<>();
        List<Integer> cellEmptyCountList = new ArrayList<>();
        for (int i = 1; i <= totalRowNum; i++) {
            T bean = PojoUtils.newInstance(clazz);
            Row row = sheet.getRow(i);
            cellEmptyCountList.clear();
            for (int j = 0; j < totalColNum; j++) {
                setCellValueToBean(row.getCell(j), columns.get(j), bean, cellEmptyCountList);
            }
            if (cellEmptyCountList.size() < totalColNum) {
                list.add(bean);
            }
        }
        return list;
    }

    /**
     * 将列表导出成 excel, 写入第一页
     *
     * @param list 列表数据
     * @param <T>  数据类型, 需要注解 {@link ExcelColumn} 表示列号, {@link ApiModelProperty} 表示列名
     *
     * @return excel 对象
     */
    public static <T> XSSFWorkbook exportToFile(List<T> list) {
        // 不确定是否要关闭...
        XSSFWorkbook workbook = new XSSFWorkbook();
        // 创建第一页
        XSSFSheet sheet = workbook.createSheet();
        addListToSheet(list, sheet, workbook);
        return workbook;
    }

    /**
     * 将列表导出成 excel, 写入第一页
     *
     * @param list 列表数据
     * @param <T>  数据类型, 需要注解 {@link ExcelColumn} 表示列号, {@link ApiModelProperty} 表示列名
     * @return excel 对象
     */
    public static <T> SXSSFWorkbook exportToBigDataFile(List<T> list) {
        // 不确定是否要关闭...
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        // 创建第一页
        SXSSFSheet sheet = workbook.createSheet();
        addListToSheet(list, sheet, workbook);
        return workbook;
    }

    /**
     * 添加列表的数据到 excel 页中
     *
     * @param list 列表数据
     * @param <T>  数据类型, 需要注解 {@link ExcelColumn} 表示列号, {@link ApiModelProperty} 表示列名
     */
    @SneakyThrows
    public static <T> void addListToSheet(List<T> list, Sheet sheet, Workbook workbook) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        // 获取列信息
        List<Field> columns = getColumns(list.get(0).getClass());
        List<JsonFormat.Shape> shapes = getShapes(columns);
        List<String> headerNames = columns.stream().map(e -> e.getAnnotation(ApiModelProperty.class))
                .map(ApiModelProperty::value).collect(Collectors.toList());
        createHeader(headerNames, sheet, workbook);
        // 循环写入每一行
        CellStyle cellStyle = getCellStyle(workbook, false);
        for (int i = 0; i < list.size(); i++) {
            Row row = sheet.createRow(i + 1);
            T obj = list.get(i);
            for (int j = 0; j < columns.size(); j++) {
                // 循环写入每一个单元格
                Object data = columns.get(j).get(obj);
                if (org.springframework.util.StringUtils.isEmpty(data)) {
                    continue;
                }
                Cell cell = row.createCell(j);
                setCellValue(cell, data, shapes.get(j));
                cell.setCellStyle(cellStyle);
            }
        }
    }

    /**
     * 设置单元格数据
     *
     * @param cell
     * @param value
     */
    private static void setCellValue(Cell cell, Object value, JsonFormat.Shape shape) {
        if (value instanceof Number) {
            if (JsonFormat.Shape.STRING.equals(shape)) {
                //数字类型 转String
                cell.setCellType(CellType.STRING);
                cell.setCellValue(value.toString());
            } else {
                //数字类型
                cell.setCellType(CellType.NUMERIC);
                cell.setCellValue(((Number) value).doubleValue());
            }
        } else if (value instanceof Boolean) {
            //布尔类型
            cell.setCellType(CellType.BOOLEAN);
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            //日期类型
            cell.setCellValue(((Date) value).getTime());
        } else {
            //其他类型都为字符串，包括表达式，字符串，错误类型以及blank
            cell.setCellType(CellType.STRING);
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 根据属性从数据对象中获取数据, 注解了 {@link EnumValue} 的字段会解析成枚举的描述
     */
    /*@SneakyThrows
    private static Object getDataByField(Object obj, Field field) {
        Object data = field.get(obj);
        if (data == null) {
            return null;
        }
        if (field.isAnnotationPresent(EnumValue.class)) {
            return EnumValueUtils.getDescByValue(field.getAnnotation(EnumValue.class).type(), (Integer) data);
        } else {
            return data;
        }
    }*/

    /**
     * 解析类得到列名属性列表并禁用访问权限检查
     * <p>
     * 属性顺序按照 {@link ExcelColumn} 的值排列
     *
     * @return 索引与列号对应的属性列表
     */
    private static <T> List<Field> getColumns(Class<T> clazz) {
        return Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(ExcelColumn.class))
                .sorted(Comparator.comparing(field -> field.getAnnotation(ExcelColumn.class).value()))
                .peek(field -> field.setAccessible(true)).collect(Collectors.toList());
    }

    private static List<JsonFormat.Shape> getShapes(List<Field> columns) {
        // @JsonFormat(shape= JsonFormat.Shape.STRING)
        return columns.stream().map(field -> Optional.ofNullable(field.getAnnotation(JsonFormat.class))
                .map(JsonFormat::shape).orElse(null)).collect(Collectors.toList());
    }

    /**
     * 根据属性将 excel 单元格的值设置进 bean 中
     */
    private static void setCellValueToBean(Cell cell, Field field, Object bean, List<Integer> cellEmptyCountList) {
        if (Objects.isNull(cell)) {
            cellEmptyCountList.add(0);
            // 跳过空单元格
            return;
        }
        Class<?> type = field.getType();
        CellType cellType = cell.getCellTypeEnum();
        if (cellType == CellType.BLANK) {
            cellEmptyCountList.add(0);
            // 跳过空白单元格
            return;
        }
        String cellValue;
        if (cellType == CellType.STRING) {
            cellValue = cell.getStringCellValue().trim();
        } else if (cellType == CellType.NUMERIC) {
            cellValue = cell.getNumericCellValue() + "";
        } else {
            throw new ValidationException("excel 单元格格式错误");
        }
        try {
            if (type.equals(String.class)) {
                field.set(bean, cellValue);
            } else if (type.equals(Double.class)) {
                field.set(bean, Double.valueOf(cellValue));
            } else if (type.equals(Float.class)) {
                field.set(bean, Double.valueOf(cellValue).floatValue());
            } else if (type.equals(Long.class)) {
                field.set(bean, Double.valueOf(cellValue).longValue());
            } else if (type.equals(Integer.class)) {
                field.set(bean, Double.valueOf(cellValue).intValue());
            } else {
                throw new IllegalStateException("the column field should be string, double, float, long or int");

            }
        } catch (IllegalAccessException ignored) {
            // 已禁止访问权限检查, 不会有此异常
        } catch (NumberFormatException e) {
            throw new ValidationException("excel 单元格格式错误");
        }
    }

    /**
     * 获取单元格格式
     */
    private static CellStyle getCellStyle(Workbook workbook, boolean bold) {
        // 设置字体
        Font font = workbook.createFont();
        // 设置字体大小
        font.setFontHeightInPoints((short) 11);
        // 字体加粗
        font.setBold(bold);
        // 设置样式
        CellStyle style = workbook.createCellStyle();
        // 在样式中应用设置的字体
        style.setFont(font);
        // 设置自动换行
        style.setWrapText(false);
        // 设置对齐样式
//        style.setAlignment(HorizontalAlignment.CENTER);
//        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 创建第一行的列头
     *
     * @param headerNames 列名列表
     */
    private static void createHeader(List<String> headerNames, Sheet sheet, Workbook workbook) {
        CellStyle headerStyle = getCellStyle(workbook, true);
        Row headerRow = sheet.createRow(0);
        for (int n = 0; n < headerNames.size(); n++) {
            Cell headerCell = headerRow.createCell(n);
            headerCell.setCellType(CellType.STRING);
            headerCell.setCellValue(new XSSFRichTextString(headerNames.get(n)));
            headerCell.setCellStyle(headerStyle);
            sheet.setColumnWidth(n, 20 * 256);
        }
    }

    /**
     * 获取有效的页名
     */
    public static String getValidSheetTitle(String title) {
        return SHEET_NAME_ILLEGAL_PATTERN
                .matcher(StringUtils.substring(title, 0, SHEET_NAME_LIMIT)).replaceAll(" ");
    }

    /**
     * 构建 excel 导入时的错误信息, 会将错误的序号分行显示
     *
     * @param errorMsgMap 错误信息字典, 键为序号, 值为错误信息
     * @return 根据错误信息字典构建的错误信息
     */
    public static String buildImportErrorMsg(SortedMap<Integer, String> errorMsgMap) {
        StringBuilder builder = new StringBuilder("excel 中以下序号的行信息导入失败:\n");
        errorMsgMap.forEach((k, v) -> builder.append(k).append(": ").append(v).append("\n"));
        return builder.toString();
    }

    /**
     * 根据日期将映射导出成 excel
     *
     * @param map 要导出的映射, 键为列名, 值为以行为顺序的数据数组, 导出时会按顺序排列
     * @return excel 对象
     */
    public static XSSFWorkbook exportMap(Map<String, String[]> map) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        // 创建第一页
        XSSFSheet sheet = workbook.createSheet();
        addMapToSheet(map, sheet, workbook);
        return workbook;
    }

    private static void addMapToSheet(Map<String, String[]> map, XSSFSheet sheet, XSSFWorkbook workbook) {
        if (map.isEmpty()) {
            return;
        }
        // 获取列信息
        List<String> columns = new ArrayList<>(map.keySet());
        createHeader(columns, sheet, workbook);
        // 循环写入每一行
        CellStyle cellStyle = getCellStyle(workbook, false);
        int length = map.get(columns.get(0)).length;
        for (int i = 0; i < length; i++) {
            XSSFRow row = sheet.createRow(i + 1);
            for (int j = 0; j < columns.size(); j++) {
                // 循环写入每一个单元格
                XSSFCell cell = row.createCell(j);
                cell.setCellType(CellType.STRING);
                cell.setCellValue(new XSSFRichTextString(map.get(columns.get(j))[i]));
                cell.setCellStyle(cellStyle);
            }
        }
    }

    public static void downFile(HttpServletResponse response, InputStream inputStream, String fileName) throws IOException {
        ServletOutputStream servletOutputStream = null;
        try {
            //清空response
            response.reset();
            // 设置response的Header,设置浏览器
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            response.setContentType("application/vnd.ms-excel");

            servletOutputStream = response.getOutputStream();
            IOUtils.copy(inputStream, servletOutputStream);
            response.flushBuffer();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (servletOutputStream != null) {
                    servletOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void downData(SXSSFWorkbook xswb, HttpServletResponse response, String fileName) {
        try {
           // 生成文件
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
            OutputStream outputStream;
            outputStream = response.getOutputStream();
            xswb.write(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
