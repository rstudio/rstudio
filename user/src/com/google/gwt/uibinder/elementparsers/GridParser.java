/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;

import java.util.ArrayList;
import java.util.List;

/**
 * A parser for Grid rows and cells.
 */
public class GridParser implements ElementParser {

  private static class CellContent {
    private String tagName;
    private String content;
    private String styleName;

    public CellContent(String tagName, String content, String styleName) {
      this.tagName = tagName;
      this.content = content;
      this.styleName = styleName;
    }

    public String getContent() {
      return this.content;
    }

    public String getStyleName() {
      return styleName;
    }

    public String getTagName() {
      return this.tagName;
    }
  }

  private static class RowContent {
    private List<CellContent> columns = new ArrayList<CellContent>();
    private String styleName;

    private void addColumn(CellContent column) {
      columns.add(column);
    }

    public List<CellContent> getColumns() {
      return columns;
    }

    public String getStyleName() {
      return styleName;
    }

    public void setStyleName(String styleName) {
      this.styleName = styleName;
    }
  }

  private static class Size {
    private int rows;
    private int columns;

    public Size(int rows, int columns) {
      this.rows = rows;
      this.columns = columns;
    }

    public int getColumns() {
      return this.columns;
    }

    public int getRows() {
      return this.rows;
    }
  }

  private static final String ROW_TAG = "row";

  private static final String CELL_TAG = "cell";

  private static final String CUSTOMCELL_TAG = "customCell";

  private static final String STYLE_NAME_ATTRIBUTE = "styleName";

  public void parse(XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {

    List<RowContent> matrix = new ArrayList<RowContent>();

    parseRows(elem, fieldName, writer, matrix);
    Size size = getMatrixSize(matrix);

    if (size.getRows() > 0 || size.getColumns() > 0) {
      writer.addStatement("%s.resize(%s, %s);", fieldName,
          Integer.toString(size.getRows()), Integer.toString(size.getColumns()));

      for (RowContent row : matrix) {
        if ((row.getStyleName() != null) && (!row.getStyleName().isEmpty())) {
          writer.addStatement("%s.getRowFormatter().setStyleName(%s, %s);",
              fieldName,
              matrix.indexOf(row),
              row.getStyleName());
        }

        for (CellContent column : row.getColumns()) {
          if (column.getTagName().equals(CELL_TAG)) {
            writer.addStatement("%s.setHTML(%s, %s, %s);", fieldName,
                Integer.toString(matrix.indexOf(row)),
                Integer.toString(row.getColumns().indexOf(column)),
                writer.declareTemplateCall(column.getContent(), fieldName));
          }
          if (column.getTagName().equals(CUSTOMCELL_TAG)) {
            writer.addStatement("%s.setWidget(%s, %s, %s);", fieldName,
                Integer.toString(matrix.indexOf(row)),
                Integer.toString(row.getColumns().indexOf(column)), column.getContent());
          }
          if ((column.getStyleName() != null) && (!column.getStyleName().isEmpty())) {
            writer.addStatement("%s.getCellFormatter().setStyleName(%s, %s, %s);",
                fieldName,
                matrix.indexOf(row),
                row.getColumns().indexOf(column),
                column.getStyleName());
          }
        }
      }
    }
  }

  private Size getMatrixSize(List<RowContent> matrix) {
    int maxColumns = 0;
    for (RowContent row : matrix) {
      maxColumns = (row.getColumns().size() > maxColumns) ? row.getColumns().size() : maxColumns;
    }
    return new Size(matrix.size(), maxColumns);
  }

  private void parseColumns(String fieldName, UiBinderWriter writer,
      RowContent row, XMLElement child)
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
      String styleName = cell.consumeStringAttribute(STYLE_NAME_ATTRIBUTE, null);
      if (tagName.equals(CELL_TAG)) {
        HtmlInterpreter htmlInt = HtmlInterpreter.newInterpreterForUiObject(
            writer, fieldName);
        String html = cell.consumeInnerHtml(htmlInt);
        newColumn = new CellContent(tagName, html, styleName);
      }
      if (tagName.equals(CUSTOMCELL_TAG)) {
        FieldWriter field = writer.parseElementToField(cell.consumeSingleChildElement());
        newColumn = new CellContent(tagName, field.getNextReference(),
            styleName);
      }
      row.addColumn(newColumn);
    }
  }

  private void parseRows(XMLElement elem, String fieldName,
      UiBinderWriter writer, List<RowContent> matrix)
      throws UnableToCompleteException {

    for (XMLElement child : elem.consumeChildElements()) {
      String tagName = child.getLocalName();
      if (!tagName.equals(ROW_TAG)
          || !elem.getPrefix().equals(child.getPrefix())) {
        writer.die(
            "%1$s:Grid elements must contain only %1$s:%2$s children, found %3$s:%4$s",
            elem.getPrefix(), ROW_TAG, child.getPrefix(), tagName);
      }

      RowContent newRow = new RowContent();
      newRow.setStyleName(child.consumeStringAttribute(STYLE_NAME_ATTRIBUTE, null));
      matrix.add(newRow);
      parseColumns(fieldName, writer, newRow, child);
    }
  }
}
