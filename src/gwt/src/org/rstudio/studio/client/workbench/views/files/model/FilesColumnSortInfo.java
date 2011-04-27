package org.rstudio.studio.client.workbench.views.files.model;

import com.google.gwt.core.client.JavaScriptObject;

public class FilesColumnSortInfo extends JavaScriptObject
{
   protected FilesColumnSortInfo()
   {  
   }
   
   public static final native FilesColumnSortInfo create(int columnIndex,
                                                         boolean ascending) /*-{
      var sortInfo = new Object();
      sortInfo.columnIndex = columnIndex;
      sortInfo.ascending = ascending;
      return sortInfo;
   }-*/;
   
   public final native int getColumnIndex() /*-{
      return this.columnIndex;
   }-*/;
   
   public final native boolean getAscending() /*-{
      return this.ascending;
   }-*/;
}
