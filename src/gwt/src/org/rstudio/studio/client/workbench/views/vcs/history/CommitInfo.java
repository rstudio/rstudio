/*
 * CommitInfo.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.history;

import com.google.gwt.core.client.JavaScriptObject;

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

   public native final String getSubject() /*-{
      return this.subject;
   }-*/;

   public final Date getDate()
   {
      return new Date((long) getDateRaw() * 1000);
   }

   public native final double getDateRaw() /*-{
      return this.date;
   }-*/;
}
