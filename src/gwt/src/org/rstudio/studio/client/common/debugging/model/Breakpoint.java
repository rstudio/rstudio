/*
 * Breakpoint.java
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

package org.rstudio.studio.client.common.debugging.model;

import com.google.gwt.core.client.JsArrayNumber;

public class Breakpoint
{
   public Breakpoint(
         String fileName, 
         FunctionSteps steps)
   {
      fileName_ = fileName;
      functionName_ = steps.getName();
      lineNumber_ = steps.getLineNumber();
      functionSteps_ = steps.getSteps();
   }
   
   public String getFileName()
   {
      return fileName_;
   }
   
   public int getLineNumber()
   {
      return lineNumber_;
   }
   
   public String getFunctionName()
   {
      return functionName_;
   }
   
   public JsArrayNumber getFunctionSteps()
   {
      return functionSteps_;
   }
   
   private String fileName_;
   private int lineNumber_;
   private String functionName_;
   private JsArrayNumber functionSteps_;
}
