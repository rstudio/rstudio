/*
 * ConsoleAction.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ConsoleAction extends JavaScriptObject
{
   protected ConsoleAction() {}

   public static final int PROMPT = 0;
   public static final int INPUT = 1;
   public static final int OUTPUT = 2;
   public static final int ERROR = 3;

   public native final int getType() /*-{
      return this.type;
   }-*/;

   public native final String getData() /*-{
      return this.data;
   }-*/;
}
