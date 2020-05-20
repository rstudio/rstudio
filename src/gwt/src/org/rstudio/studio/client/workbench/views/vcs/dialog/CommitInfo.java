/*
 * CommitInfo.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import java.util.Date;

public class CommitInfo extends JavaScriptObject
{
   protected CommitInfo() {}

   public native final String getId() /*-{
      return this.id;
   }-*/;

   public native final String getAuthor() /*-{
      return this.author;
   }-*/;

   public native final String getParent() /*-{
      return this.parent || "";
   }-*/;

   public native final String getSubject() /*-{
      return this.subject;
   }-*/;

   public native final String getDescription() /*-{
      return this.description || "";
   }-*/;

   public native final String getGraph() /*-{
      return this.graph || "";
   }-*/;

   public final Date getDate()
   {
      return new Date((long) getDateRaw() * 1000);
   }

   public native final double getDateRaw() /*-{
      return this.date;
   }-*/;

   public native final JsArrayString getRefs() /*-{
      return this.refs || [];
   }-*/;

   public native final JsArrayString getTags() /*-{
      return this.tags || [];
   }-*/;
}
