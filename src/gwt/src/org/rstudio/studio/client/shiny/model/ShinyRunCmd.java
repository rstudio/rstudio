/*
 * ShinyRunCmd.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.shiny.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ShinyRunCmd extends JavaScriptObject
{
   protected ShinyRunCmd() {} 
   
   public final native String getRunCmd() /*-{
      return this.run_cmd;
   }-*/;
}
