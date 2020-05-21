/*
 * RmdYamlData.java
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
package org.rstudio.studio.client.rmarkdown.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

public class RmdYamlData extends JavaScriptObject
{
   protected RmdYamlData()
   {
   }

   public final native RmdFrontMatter getFrontMatter() /*-{
      return this.data;
   }-*/;
   
   public final native String getParseError() /*-{
      return this.parse_error;
   }-*/;

   public final native boolean parseSucceeded() /*-{
      return this.parse_succeeded;
   }-*/;
   
   // Returns the parse error, with the line number adjusted by the given 
   // offset. 
   public final String getOffsetParseError(int offsetline)
   {
      String error = getParseError();
      String lineRegex = "line (\\d+),";
      RegExp reg = RegExp.compile(lineRegex);
      MatchResult result = reg.exec(error);
      if (result == null || result.getGroupCount() < 2)
         return getParseError();
      else
      {
         Integer newLine = Integer.parseInt(result.getGroup(1)) + offsetline;
         return error.replaceAll(lineRegex, 
                                 "line " + newLine.toString() + ",");
      }
   }
}
