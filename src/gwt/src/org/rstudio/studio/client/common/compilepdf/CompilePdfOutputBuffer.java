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

import org.rstudio.studio.client.common.shell.ShellWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

import com.google.gwt.user.client.ui.Composite;

public class CompilePdfOutputBuffer extends Composite
{
   public CompilePdfOutputBuffer()
   {
      outputWidget_ = new ShellWidget(new AceEditor());
      outputWidget_.setSize("100%", "100%");
      outputWidget_.setMaxOutputLines(1000);
      outputWidget_.setReadOnly(true);
      outputWidget_.setSuppressPendingInput(true);
      
      initWidget(outputWidget_);
   }
   
   public void append(String output)
   {
      outputWidget_.consoleWriteOutput(output);    
   }
   
   public void scrollToBottom()
   {
      outputWidget_.scrollToBottom();
   }

   public void clear()
   {
      outputWidget_.clearOutput(); 
   }
 
   
   private ShellWidget outputWidget_;
}
