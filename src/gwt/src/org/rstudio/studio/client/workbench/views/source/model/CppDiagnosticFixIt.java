/*
 * CppDiagnosticFixIt.java
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
package org.rstudio.studio.client.workbench.views.source.model;


import org.rstudio.core.client.FileRange;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Fix-its are described in terms of a source range whose contents
 * should be replaced by a string. This approach generalizes over
 * three kinds of operations: removal of source code (the range covers
 * the code to be removed and the replacement string is empty),
 * replacement of source code (the range covers the code to be
 * replaced and the replacement string provides the new code), and
 * insertion (both the start and end of the range point at the
 * insertion location, and the replacement string provides the text to
 * insert).
 *
 * Note that source ranges are half-open ranges [a, b), so the source 
 * code should be replaced from a and up to (but not including) b.
 *
 */
public class CppDiagnosticFixIt extends JavaScriptObject
{
   protected CppDiagnosticFixIt()
   {
   }
   
   public static native FileRange getRange() /*-{
      return this.range;
   }-*/;
   
   public static native String getReplacement() /*-{
      return this.replacement;
   }-*/;
}
