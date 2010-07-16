/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.user.client.ui.Grid;

import java.util.Vector;

/**
 * A parser for Grid rows and cells.
 */
public class GridParser implements ElementParser {

  private static class CellContent {
    private String tagName;
    private String content;

    public CellContent(String tagName, String content) {
      this.tagName = tagName;
      this.content = content;
    }

    public String getConent() {
      return this.content;
    }

    public String getTagName() {
      return this.tagName;
    }
  }

  private static class Size {
    private int rows;
    private int columns;

    public Size() {
      this.rows = 0;
      this.columns = 0;
    }

    public int getColumns() {
      return this.columns;
    }

    public int getRows() {
      return this.rows;
    }

    public void setColumns(int cols) {
      this.columns = cols;
    }

    public void setRows(int rows) {
      this.rows = rows;
    }
  }

  private static final String ROW_TAG = "row";

  private static final String CELL_TAG = "cell";

  private static final String CUSTOMCELL_TAG = "customCell";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    Vector<Vector<CellContent>> matrix = new Vector<Vector<CellContent>>();

    parseRows(elem, fieldName, writer, matrix);

    Size size = getMatrixSize(matrix);

    writer.setFieldInitializerAsConstructor(fieldName,
        writer.getOracle().findType(Grid.class.getName()),
        Integer.toString(size.getRows()), Integer.toString(size.getColumns()));
    for (Vector<CellContent> row : matrix) {
      for (CellContent column : row) {
        if (column.getTagName().equals(CELL_TAG)) {
          writer.addStatement("%s.setHTML(%s, %s, \"%s\");", fieldName,
              Integer.toString(matrix.indexOf(row)),
              Integer.toString(row.indexOf(column)), column.getConent());
        }
        if (column.getTagName().equals(CUSTOMCELL_TAG)) {
          writer.addStatement("%s.setWidget(%s, %s, %s);", fieldName,
              Integer.toString(matrix.indexOf(row)),
              Integer.toString(row.indexOf(column)), column.getConent());
        }
      }
    }
  }

  private Size getMatrixSize(Vector<Vector<CellContent>> matrix) {
    Size size = new Size();

    size.setRows(matrix.size());

    int maxColumns = 0;
    for (Vector<CellContent> column : matrix) {
      maxColumns = (column.size() > maxColumns) ? column.size() : maxColumns;
    }
    size.setColumns(maxColumns);

    return size;
  }

  private void parseColumns(String fieldName, UiBinderWriter writer,
      Vector<Vector<CellContent>> matrix, XMLElement child)
      throws UnableToCompleteException {

    String tagName;
    for (XMLElement cell : child.consumeChildElements()) {
      tagName = cell.getLocalName();
      if (!tagName.equals(CELL_TAG) && !tagName.equals(CUSTOMCELL_TAG)
          || !cell.getPrefix().equals(child.getPrefix())) {
        writer.die("Grid's row tag in %s may only contain %s or %s element.",
            fieldName, CELL_TAG, CUSTOMCELL_TAG);
      }
      CellContent newColumn = null;
      if (tagName.equals(CELL_TAG)) {
        HtmlInterpreter htmlInt = HtmlInterpreter.newInterpreterForUiObject(
            writer, fieldName);
        String html = cell.consumeInnerHtml(htmlInt);
        newColumn = new CellContent(tagName, html);
      }
      if (tagName.equals(CUSTOMCELL_TAG)) {
        newColumn = new CellContent(tagName,
            writer.parseElementToField(cell.consumeSingleChildElement()));
      }
      matrix.get(matrix.size() - 1).add(newColumn);
    }
  }

  private void parseRows(XMLElement elem, String fieldName,
      UiBinderWriter writer, Vector<Vector<CellContent>> matrix)
      throws UnableToCompleteException {

    for (XMLElement child : elem.consumeChildElements()) {
      String tagName = child.getLocalName();
      if (!tagName.equals(ROW_TAG)
          || !elem.getPrefix().equals(child.getPrefix())) {
        writer.die(
            "%1$s:Grid elements must contain only %1$s:%2$s children, found %3$s:%4$s",
            elem.getPrefix(), ROW_TAG, child.getPrefix(), tagName);
      }
      Vector<CellContent> newRow = new Vector<CellContent>();
      matrix.add(newRow);
      parseColumns(fieldName, writer, matrix, child);
    }
  }
}
