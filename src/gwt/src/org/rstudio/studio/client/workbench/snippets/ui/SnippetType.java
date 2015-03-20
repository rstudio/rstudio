/*
 * SnippetType.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.snippets.ui;

import org.rstudio.studio.client.common.filetypes.TextFileType;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class SnippetType extends Composite
{
   public SnippetType(TextFileType fileType)
   {
      this(fileType.getLabel(), fileType);
   }
   
   public SnippetType(String name, TextFileType fileType)
   {
      fileType_ = fileType;
      HorizontalPanel panel = new HorizontalPanel();
      Image icon = new Image(fileType.getDefaultIcon());
      icon.getElement().getStyle().setMarginRight(2, Unit.PX);
      panel.add(icon);
      Label label = new Label(name);    
      label.getElement().getStyle().setMarginLeft(5, Unit.PX);
      panel.add(label);
      initWidget(panel);
   }
   
   public TextFileType getFileType()
   {
      return fileType_;
   }
   
   private final TextFileType fileType_;
}

