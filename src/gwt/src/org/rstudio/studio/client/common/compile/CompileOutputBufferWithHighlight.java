/*
 * CompileOutputBufferWithHighlight.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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


package org.rstudio.studio.client.common.compile;

import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.widget.BottomScrollPanel;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;

import com.google.gwt.user.client.ui.Composite;

public class CompileOutputBufferWithHighlight extends Composite 
                                implements CompileOutputDisplay
{
   public CompileOutputBufferWithHighlight()
   {
      styles_ = ConsoleResources.INSTANCE.consoleStyles();
      
      output_ = new PreWidget();
      output_.setStylePrimaryName(styles_.output());
      output_.addStyleName("ace_text-layer");
      output_.addStyleName("ace_line");
      output_.addStyleName(styles_.paddedOutput());
      FontSizer.applyNormalFontSize(output_);
      console_ = new VirtualConsole(output_.getElement());
    
      scrollPanel_ = new BottomScrollPanel();
      scrollPanel_.setSize("100%", "100%");
      scrollPanel_.addStyleName("ace_editor");
      scrollPanel_.addStyleName("ace_scroller");
      scrollPanel_.setWidget(output_);
      
      initWidget(scrollPanel_);
   }
   
   
   @Override
   public void writeCommand(String command)
   {
      write(command, styles_.command() + ConsoleResources.KEYWORD_CLASS_NAME);
   }
   
   @Override
   public void writeOutput(String output)
   {
      write(output, styles_.output());
   }

   @Override
   public void writeError(String error)
   {
      write(error, getErrorClass());
   }
   
   @Override
   public void scrollToBottom()
   {
      scrollPanel_.scrollToBottom();
   }

   @Override
   public void clear()
   {
      console_.clear();
      output_.setText("");
   }
   
   private void write(String output, String className)
   {
      console_.submit(output, className);
      scrollPanel_.onContentSizeChanged();
   }
   
   private String getErrorClass()
   {
      return styles_.output() + " " + 
             RStudioGinjector.INSTANCE.getUIPrefs().getThemeErrorClass();
   }
 
   PreWidget output_;
   VirtualConsole console_;
   private BottomScrollPanel scrollPanel_;
   private ConsoleResources.ConsoleStyles styles_;
}
