/*
 * XTermDimensions.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.terminal.xterm;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Size of xterm in rows and columns of text. A dimension will be returned
 * as -1 if it could not be computed.
 */
public class XTermDimensions extends JavaScriptObject
{
   protected XTermDimensions() {}
   
   public final native int getCols() /*-{
      if (this.cols !== this.cols) { return -1; } // isNaN
      return this.cols;
   }-*/;

   public final native int getRows() /*-{
      if (this.rows !== this.rows) { return -1; } // isNaN
      return this.rows;
   }-*/;
}
