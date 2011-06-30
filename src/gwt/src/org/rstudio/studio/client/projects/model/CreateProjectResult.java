package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;

public class CreateProjectResult extends JavaScriptObject
{
   public final static int STATUS_OK = 0;
   public final static int STATUS_ALREADY_EXISTS = 2;
   public final static int STATUS_NO_WRITE_ACCESS = 3;
   
   protected CreateProjectResult()
   {
   }
   
   public native final int getStatus() /*-{
      return this.status;
   }-*/;

   public native final String getProjectFilePath() /*-{
      return this.project_file_path;
   }-*/;
}
