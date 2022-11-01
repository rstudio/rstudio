/*
 * TestsResult.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.tests.model;

import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class TestsResult extends JavaScriptObject
{
   protected TestsResult() 
   {
   }

   public native final boolean getSucceeded() /*-{
      return this.succeeded;
   }-*/;

   public native final String getTargetFile() /*-{
      return this.target_file;
   }-*/;

   public final native JsArray<SourceMarker> getTestsErrors() /*-{
      return this.tests_errors;
   }-*/;
   
   public final boolean equals(TestsResult other)
   {
      return getTargetFile() == other.getTargetFile();
   }
}
