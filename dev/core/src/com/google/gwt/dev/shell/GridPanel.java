// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class GridPanel extends Composite {

    public GridPanel(Composite parent, int style, int numCols, boolean equalWidthCols, int marginWidth, int marginHeight) {
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

    public GridPanel(Composite parent, int style, int numCols, boolean equalWidthCols) {
        this(parent, style, numCols, equalWidthCols, 5, 5);
    }

    protected GridData getGridData(Control control) {
        GridData gridData = (GridData)control.getLayoutData();
        if (gridData == null) {
            gridData = new GridData();
            control.setLayoutData(gridData);
        }
        return gridData;
    }

    protected GridData setGridData(Control control, int rowSpan, int colSpan, int hAlign, int vAlign, boolean hGrab, boolean vGrab) {
        return setGridData(control, rowSpan, colSpan, hAlign, vAlign, hGrab, vGrab, SWT.DEFAULT, SWT.DEFAULT);  
    }

    protected GridData setGridData(Control control, int rowSpan, int colSpan, int hAlign, int vAlign, boolean hGrab, boolean vGrab, int widthHint, int heightHint) {
        GridData gridData = getGridData(control);
        gridData.horizontalSpan = colSpan;
        gridData.verticalSpan = rowSpan;
        gridData.horizontalAlignment = hAlign;
        gridData.verticalAlignment = vAlign;
        gridData.grabExcessHorizontalSpace = hGrab;
        gridData.grabExcessVerticalSpace = vGrab;
        if (heightHint != SWT.DEFAULT)
            gridData.heightHint = heightHint;
        if (widthHint != SWT.DEFAULT)
            gridData.widthHint = widthHint;
        control.setLayoutData(gridData);
        return gridData; 
    }

    protected static final int FILL = GridData.FILL;
    protected static final int CENTER = GridData.CENTER;
    protected static final int MIDDLE = GridData.CENTER;
    protected static final int LEFT = GridData.BEGINNING;
    protected static final int RIGHT = GridData.END;
    protected static final int TOP = GridData.BEGINNING;
    protected static final int BOTTOM = GridData.END;
}
