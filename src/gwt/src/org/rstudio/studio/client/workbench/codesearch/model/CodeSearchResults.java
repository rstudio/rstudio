package org.rstudio.studio.client.workbench.codesearch.model;

import org.rstudio.core.client.jsonrpc.RpcObjectList;

import com.google.gwt.core.client.JavaScriptObject;

public class CodeSearchResults extends JavaScriptObject
{
   protected CodeSearchResults()
   {
      
   }

   public final native RpcObjectList<RFileItem> getRFileItems() /*-{
      return this.file_items;
   }-*/;
   
   public final native RpcObjectList<RSourceItem> getRSourceItems() /*-{
      return this.source_items;
   }-*/;

   public final native boolean getMoreAvailable() /*-{
      return this.more_available;
   }-*/;

}
