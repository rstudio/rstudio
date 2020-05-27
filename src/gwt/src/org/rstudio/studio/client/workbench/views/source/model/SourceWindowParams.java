/*
 * SourceWindowParams.java
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

import com.google.gwt.core.client.JavaScriptObject;

public class SourceWindowParams extends JavaScriptObject
{
   protected SourceWindowParams()
   {
   }

   public final static native SourceWindowParams create(int ordinal, 
         String title, String workingDir, String docId, 
         SourcePosition sourcePosition) /*-{
      return { 
         "ordinal"        : ordinal,
         "title"          : title,
         "working_dir"    : workingDir,
         "doc_id"         : docId,
         "source_position": sourcePosition,
      };
   }-*/;
   
   public final native String getTitle() /*-{
      return this.title;
   }-*/;
   
   public final native String getWorkingDir() /*-{
      return this.working_dir;
   }-*/;

   public final native int getOrdinal() /*-{
      return this.ordinal;
   }-*/;
   
   public final native String getDocId() /*-{
      return this.doc_id;
   }-*/;

   public final native SourcePosition getSourcePosition() /*-{
      return this.source_position;
   }-*/;
}
