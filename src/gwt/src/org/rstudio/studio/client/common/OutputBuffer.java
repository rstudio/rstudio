/*
 * OutputBuffer.java
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
package org.rstudio.studio.client.common;

import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;

public class OutputBuffer extends Composite
{
   public OutputBuffer()
   {
      output_ = new PreWidget();
      output_.setStylePrimaryName(
                        ConsoleResources.INSTANCE.consoleStyles().output());
      FontSizer.applyNormalFontSize(output_);
    
      scrollPanel_ = new ScrollPanel();
      scrollPanel_.setSize("100%", "100%");
      scrollPanel_.add(output_);
      
      initWidget(scrollPanel_);
   }
   
   public void append(String output)
   {
      virtualConsole_.submit(output);
      output_.setText(virtualConsole_.toString()); 
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
   private VirtualConsole virtualConsole_ = new VirtualConsole();
   private ScrollPanel scrollPanel_;
}
