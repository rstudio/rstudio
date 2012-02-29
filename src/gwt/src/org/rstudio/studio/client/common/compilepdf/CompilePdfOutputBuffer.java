/*
 * CompilePdfOutputBuffer.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.compilepdf;

import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources.ConsoleStyles;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;

public class CompilePdfOutputBuffer extends Composite
{
   public CompilePdfOutputBuffer()
   {
      ConsoleStyles styles = ConsoleResources.INSTANCE.consoleStyles();

      output_ = new PreWidget();
      output_.setStylePrimaryName(styles.output());
      FontSizer.applyNormalFontSize(output_);
    
      scrollPanel_ = new ScrollPanel();
      scrollPanel_.addStyleName(
                CompilePdfResources.INSTANCE.styles().outputScrollPanel());
      scrollPanel_.setSize("100%", "100%");
      scrollPanel_.add(output_);
      
      initWidget(scrollPanel_);
   }
   
   public void append(String output)
   {
      output_.appendText(output); 
      scrollPanel_.scrollToBottom();
   }
   
   public void scrollToBottom()
   {
     scrollPanel_.scrollToBottom();
   }

   public void clear()
   {
      output_.setText("");
   }
 
   private PreWidget output_;
   private ScrollPanel scrollPanel_;
}
