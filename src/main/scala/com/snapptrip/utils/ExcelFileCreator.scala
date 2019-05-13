package com.snapptrip.utils

import java.io.FileOutputStream

import org.apache.poi.hssf.usermodel.{HSSFSheet, HSSFWorkbook}

class ExcelFileCreator {

  var sheet: HSSFSheet = _
  var file: FileOutputStream = _
  var wb: HSSFWorkbook = _

  def createFile(filePath: String)={
    file = new FileOutputStream(filePath)
  }

  def writeFile(): Unit ={
    wb.write(file)
  }

  def closeFile(): Unit ={
    file.close()
  }

  def createSheet(sheetName: String) = {
    wb = new HSSFWorkbook() //You can use XSSFWorkbook to generate .xlsx files
    sheet = wb.createSheet(sheetName)
  }

  def createExcelHeader(header: Seq[String], rowNo: Int): Unit = {
    val excelRow = sheet.createRow(rowNo)
    var cellNo = 0
    header.foreach { cell =>
      excelRow.createCell(cellNo).setCellValue(cell)
      cellNo = cellNo + 1
    }
  }

  def createExcelRow(row: Seq[String], rowNo: Int): Unit = {
    val excelRow = sheet.createRow(rowNo)
    var cellNo = 0
    row.foreach { cell =>
      excelRow.createCell(cellNo).setCellValue(cell)
      cellNo = cellNo + 1
    }
  }

}
