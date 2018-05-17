/*
 * PlumberViewerType.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.studio.client.plumber.model;

import com.google.gwt.core.client.JavaScriptObject;

public class PlumberViewerType extends JavaScriptObject
{
   protected PlumberViewerType() {}
   
   public final static int PLUMBER_VIEWER_USER = 0;
   public final static int PLUMBER_VIEWER_NONE = 1;
   public final static int PLUMBER_VIEWER_PANE = 2;
   public final static int PLUMBER_VIEWER_WINDOW = 3;
   public final static int PLUMBER_VIEWER_BROWSER = 4;

   public final native int getViewerType() /*-{ 
      return this.viewerType;
   }-*/;
}
