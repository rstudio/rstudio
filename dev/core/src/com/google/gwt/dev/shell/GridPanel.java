/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class GridPanel extends Composite {

  protected static final int FILL = GridData.FILL;

  protected static final int CENTER = GridData.CENTER;

  protected static final int MIDDLE = GridData.CENTER;

  protected static final int LEFT = GridData.BEGINNING;

  protected static final int RIGHT = GridData.END;

  protected static final int TOP = GridData.BEGINNING;
  protected static final int BOTTOM = GridData.END;

  public GridPanel(Composite parent, int style, int numCols,
      boolean equalWidthCols) {
    this(parent, style, numCols, equalWidthCols, 5, 5);
  }

  public GridPanel(Composite parent, int style, int numCols,
      boolean equalWidthCols, int marginWidth, int marginHeight) {
    super(parent, style);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = numCols;
    gridLayout.makeColumnsEqualWidth = equalWidthCols;
    gridLayout.marginWidth = marginWidth;
    gridLayout.marginHeight = marginHeight;
    gridLayout.horizontalSpacing = 1;
    gridLayout.verticalSpacing = 1;
    setLayout(gridLayout);
  }

  protected GridData getGridData(Control control) {
    GridData gridData = (GridData) control.getLayoutData();
    if (gridData == null) {
      gridData = new GridData();
      control.setLayoutData(gridData);
    }
    return gridData;
  }

  protected GridData setGridData(Control control, int rowSpan, int colSpan,
      int hAlign, int vAlign, boolean hGrab, boolean vGrab) {
    return setGridData(control, rowSpan, colSpan, hAlign, vAlign, hGrab, vGrab,
        SWT.DEFAULT, SWT.DEFAULT);
  }

  protected GridData setGridData(Control control, int rowSpan, int colSpan,
      int hAlign, int vAlign, boolean hGrab, boolean vGrab, int widthHint,
      int heightHint) {
    GridData gridData = getGridData(control);
    gridData.horizontalSpan = colSpan;
    gridData.verticalSpan = rowSpan;
    gridData.horizontalAlignment = hAlign;
    gridData.verticalAlignment = vAlign;
    gridData.grabExcessHorizontalSpace = hGrab;
    gridData.grabExcessVerticalSpace = vGrab;
    if (heightHint != SWT.DEFAULT) {
      gridData.heightHint = heightHint;
    }

    if (widthHint != SWT.DEFAULT) {
      gridData.widthHint = widthHint;
    }

    control.setLayoutData(gridData);
    return gridData;
  }
}
