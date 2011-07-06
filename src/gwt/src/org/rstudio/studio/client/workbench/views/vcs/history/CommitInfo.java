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
      return new Date((long) getDateRaw());
   }

   public native final double getDateRaw() /*-{
      return this.date;
   }-*/;
}
