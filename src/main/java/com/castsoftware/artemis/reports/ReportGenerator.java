package com.castsoftware.artemis.reports;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.datasets.FrameworkBean;
import com.castsoftware.artemis.datasets.FrameworkType;
import com.castsoftware.artemis.nlp.model.NLPCategory;
import com.castsoftware.artemis.nlp.model.NLPConfidence;
import com.castsoftware.artemis.nlp.model.NLPResults;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ReportGenerator {

    public static final String ARTEMIS_WORKSPACE = Configuration.get("artemis.workspace.folder");
    public static final String REPORT_FOLDER = ARTEMIS_WORKSPACE + Configuration.get("nlp.reports_generator.folder");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy_MM_dd_HHmmss");

    private static final Integer COLUMN_LENGTH = 5;

    private String applicationContext;
    private HSSFWorkbook workbook;
    private HSSFSheet mainSheet;

    private final List<FrameworkBean> frameworkList;
    private final List<FrameworkBean> nonFrameworkList;
    private final List<FrameworkBean> toInvestigateFrameworkList;

    private Integer rowNumber = 0;

    private Integer getAndIncrementRow() {
        Integer actual = rowNumber;
        rowNumber++;
        return actual;
    }


    /**
     * Add a CAST divider
     * @param text Text to be displayed in the divider
     * @param style
     * @param height
     * @return
     */
    private Sheet addCastDivider(String text, Integer height) {
        Integer rowNum = getAndIncrementRow();
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());

        Row row = mainSheet.createRow(rowNum);
        Cell cell = row.createCell(rowNum);
        cell.setCellValue(text);
        cell.setCellStyle(style);

        mainSheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, COLUMN_LENGTH));

        return mainSheet;
    }

    /**
     * Add results to the Report
     * @param type
     * @param frameworkBean
     */
    public void addFrameworkBean(FrameworkType type, FrameworkBean frameworkBean) {
        if(type == FrameworkType.TO_INVESTIGATE) {
            toInvestigateFrameworkList.add(frameworkBean);
        } else if(type == FrameworkType.FRAMEWORK) {
            frameworkList.add(frameworkBean);
        } else if(type == FrameworkType.NOT_FRAMEWORK) {
            nonFrameworkList.add(frameworkBean);
        }
    }

    /**
     * Write the framework to the worksheet
     * @param fb Framework Bean to write
     */
    private void writeFrameworkBean(FrameworkBean fb){
        Integer rowNum = getAndIncrementRow();
        Row row = mainSheet.createRow(rowNum);

        row.createCell(0).setCellValue(fb.getName());
        row.createCell(1).setCellValue(fb.getDiscoveryDate());
        row.createCell(2).setCellValue(fb.getDescription());
        row.createCell(3).setCellValue(fb.getLocation());
        row.createCell(4).setCellValue(fb.getNumberOfDetection());

    }

    /**
     * Write the framework bean headers to the worksheet
     */
    private void writeFrameworkBeanHeaders(){
        Integer rowNum = getAndIncrementRow();
        Row row = mainSheet.createRow(rowNum);

        row.createCell(0).setCellValue("Name");
        row.createCell(1).setCellValue("Discovery Date");
        row.createCell(2).setCellValue("Description");
        row.createCell(3).setCellValue("Location");
        row.createCell(4).setCellValue("Number of detection");
    }

    /**
     * Generate the Excel report using the timestamp
     * @return
     * @throws IOException
     */
    public void generate() throws IOException {

        Path reportFolderPath = Path.of(REPORT_FOLDER);
        if(!Files.exists(reportFolderPath)) {
            Files.createDirectories(reportFolderPath);
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String fileLocation = reportFolderPath + "/ArtemisAnalyze_" + applicationContext+ "_on" + SDF.format(timestamp) + ".xls";

        // Create style
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());

        // Write detected as framework
        this.addCastDivider("Detected as Framework", 1);
        writeFrameworkBeanHeaders();
        for(FrameworkBean fb : frameworkList) {
            writeFrameworkBean(fb);
        }

        // Write detected as non-framework
        this.addCastDivider("Detected as non-framework", 1);
        writeFrameworkBeanHeaders();
        for(FrameworkBean fb : nonFrameworkList) {
            writeFrameworkBean(fb);
        }

        //  Write detected as to investigate framework
        this.addCastDivider("To investigate Framework", 1);
        writeFrameworkBeanHeaders();
        for(FrameworkBean fb : toInvestigateFrameworkList) {
            writeFrameworkBean(fb);
        }


        try(FileOutputStream outputStream = new FileOutputStream(fileLocation)) {
            workbook.write(outputStream);
            workbook.close();
        }
    }

    public ReportGenerator(String applicationContext) {
        this.applicationContext = applicationContext;
        this.workbook = new HSSFWorkbook();

        // Init table
        this.frameworkList = new ArrayList<>();
        this.nonFrameworkList = new ArrayList<>();
        this.toInvestigateFrameworkList = new ArrayList<>();

        // Create style
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());

        // Create sheet in workbook
        this.mainSheet = workbook.createSheet("Frame work report");

        this.addCastDivider("ARTEMIS Framework Detector", 4);
    }

}
