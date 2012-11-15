/*
 * CompileOutputBufferWithHighlight.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


package org.rstudio.studio.client.common.compile;

import org.rstudio.core.client.widget.BottomScrollPanel;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
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
      output_.setWidth("100%");
      FontSizer.applyNormalFontSize(output_);
    
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
      output_.setText("");
   }
   
   private void write(String output, String className)
   {
      SpanElement span = Document.get().createSpanElement();
      span.setClassName(className);
      span.setInnerText(output);
      output_.getElement().appendChild(span);

      scrollPanel_.onContentSizeChanged();
   }
   
   private String getErrorClass()
   {
      return styles_.output() + " " + 
             RStudioGinjector.INSTANCE.getUIPrefs().getThemeErrorClass();
   }
 
   PreWidget output_;
   private BottomScrollPanel scrollPanel_;
   private ConsoleResources.ConsoleStyles styles_;
}
