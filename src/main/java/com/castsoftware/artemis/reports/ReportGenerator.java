/*
 * Copyright (C) 2020  Hugo JOBY
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty ofnMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNUnLesser General Public License v3 for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public v3 License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.artemis.reports;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.datasets.FrameworkNode;
import com.castsoftware.artemis.utils.Workspace;
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

  private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy_MM_dd_HHmmss");

  private static final Integer COLUMN_LENGTH = 5;
  private final List<FrameworkNode> frameworkList;
  private final List<FrameworkNode> nonFrameworkList;
  private final List<FrameworkNode> toInvestigateFrameworkList;
  private String applicationContext;
  private HSSFWorkbook workbook;
  private HSSFSheet mainSheet;
  private Integer rowNumber = 0;

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

  /**
   * Add a CAST divider
   *
   * @param text Text to be displayed in the divider
   * @param height
   * @return
   */
  private Sheet addCastDivider(String text, Integer height) {
    Integer rowNum = getAndIncrementRow();
    CellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());

    Row row = mainSheet.createRow(rowNum);
    Cell cell = row.createCell(0);
    cell.setCellValue(text);
    cell.setCellStyle(style);

    mainSheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, COLUMN_LENGTH));

    return mainSheet;
  }

  private Integer getAndIncrementRow() {
    Integer actual = rowNumber;
    rowNumber++;
    return actual;
  }

  /**
   * Add results to the Report
   *
   * @param frameworkBean
   */
  public void addFrameworkBean(FrameworkNode frameworkBean) {
    switch (frameworkBean.getFrameworkType()) {
      case TO_INVESTIGATE:
        toInvestigateFrameworkList.add(frameworkBean);
        break;
      case NOT_FRAMEWORK:
        nonFrameworkList.add(frameworkBean);
        break;
      case FRAMEWORK:
        frameworkList.add(frameworkBean);
        break;
    }
  }

  /**
   * Generate the Excel report using the timestamp
   *
   * @return
   * @throws IOException
   */
  public void generate(Neo4jAL neo4jAL) throws IOException {
    Path reportFolderPath =
        Workspace.getWorkspacePath(neo4jAL).resolve(Configuration.get("artemis.reports_generator.folder"));

    if (!Files.exists(reportFolderPath)) {
      Files.createDirectories(reportFolderPath);
    }

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    String fileLocation =
        reportFolderPath
            + "/ArtemisAnalyze_"
            + applicationContext
            + "_on"
            + SDF.format(timestamp)
            + ".xls";

    // Create style
    CellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());

    // Write detected as framework
    addCastDivider("Detected as Framework", 1);
    writeFrameworkBeanHeaders();
    for (FrameworkNode fb : frameworkList) {
      writeFrameworkBean(fb);
    }

    // Write detected as non-framework
    addCastDivider("Detected as non-framework", 1);
    writeFrameworkBeanHeaders();
    for (FrameworkNode fb : nonFrameworkList) {
      writeFrameworkBean(fb);
    }

    //  Write detected as to investigate framework
    addCastDivider("To investigate Framework", 1);
    writeFrameworkBeanHeaders();
    for (FrameworkNode fb : toInvestigateFrameworkList) {
      writeFrameworkBean(fb);
    }

    try (FileOutputStream outputStream = new FileOutputStream(fileLocation)) {
      workbook.write(outputStream);
      workbook.close();
    }
  }

  /** Write the framework bean headers to the worksheet */
  private void writeFrameworkBeanHeaders() {
    Integer rowNum = getAndIncrementRow();
    Row row = mainSheet.createRow(rowNum);

    row.createCell(0).setCellValue("Name");
    row.createCell(1).setCellValue("Discovery Date");
    row.createCell(2).setCellValue("Description");
    row.createCell(3).setCellValue("Location");
    row.createCell(4).setCellValue("Number of detection");
    row.createCell(5).setCellValue("Percentage of detection");
  }

  /**
   * Write the framework to the worksheet
   *
   * @param fb Framework Bean to write
   */
  private void writeFrameworkBean(FrameworkNode fb) {
    Integer rowNum = getAndIncrementRow();
    Row row = mainSheet.createRow(rowNum);

    row.createCell(0).setCellValue(fb.getName());
    row.createCell(1).setCellValue(fb.getDiscoveryDate());
    row.createCell(2).setCellValue(fb.getDescription());
    row.createCell(3).setCellValue(fb.getLocation());
    row.createCell(4).setCellValue(fb.getNumberOfDetection());
    row.createCell(5).setCellValue(fb.getNumberOfDetection());
  }
}
