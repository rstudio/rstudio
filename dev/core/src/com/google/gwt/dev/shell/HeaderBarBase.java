// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Base class for <code>Composites</code> that contain <code>ToolItem</code> entities.
 */
public class HeaderBarBase extends Composite implements DisposeListener {
  
  private final Color bgColor;
  private final ToolBar toolBar;
  
  public HeaderBarBase(Composite parent) {
    super(parent, SWT.NONE);

    Composite outer = new Composite(this, SWT.NONE);
    FillLayout fillLayout = new FillLayout();
    fillLayout.marginHeight = 1;
    fillLayout.marginWidth = 1;
    setLayout(fillLayout);

    bgColor = new Color(null, 239, 237, 216);
    addDisposeListener(this);

    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginLeft = 8;
    gridLayout.marginRight = 2;
    gridLayout.marginTop = 0;
    gridLayout.marginBottom = 0;
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    outer.setLayout(gridLayout);
    outer.setBackground(bgColor);

    toolBar = new ToolBar(outer, SWT.FLAT);
    toolBar.setBackground(new Color(null, 255, 0, 0));
    GridData data = new GridData();
    data.grabExcessHorizontalSpace = true;
    data.verticalAlignment = SWT.CENTER;
    data.horizontalAlignment = SWT.FILL;
    toolBar.setLayoutData(data);
    toolBar.setBackground(bgColor);

    RowLayout rowLayout = new RowLayout();
    rowLayout.fill = true;
    rowLayout.pack = false;
    rowLayout.wrap = false;
    toolBar.setLayout(rowLayout);

    Label logoLabel = new Label(outer, SWT.BORDER | SWT.SHADOW_IN);
    logoLabel.setImage(LowLevel.loadImage("logo.gif"));
    logoLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
  }

  public void widgetDisposed(DisposeEvent e) {
    bgColor.dispose();
  }
  
  public ToolBar getToolBar() {
    return toolBar;
  }
  
  public ToolItem newItem(String imageName, String label, String tooltip) {
    ToolItem item = new ToolItem(toolBar, SWT.PUSH);
    item.setImage(LowLevel.loadImage(imageName));
    item.setText(label);
    item.setSelection(false);
    item.setToolTipText(tooltip);
    item.setWidth(60);
    return item;
  }
  
  public void newSeparator() {
    new ToolItem(toolBar, SWT.SEPARATOR);
  }
}
