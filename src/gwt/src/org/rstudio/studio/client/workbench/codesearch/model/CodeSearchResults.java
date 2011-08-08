package org.rstudio.studio.client.workbench.codesearch.model;

import org.rstudio.core.client.jsonrpc.RpcObjectList;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class CodeSearchResults extends JavaScriptObject
{
   protected CodeSearchResults()
   {
      
   }

   public final native JsArrayString getFiles() /*-{
      return this.files;
   }-*/;
   
   public final native RpcObjectList<RSourceItem> getRSourceItems() /*-{
      return this.source_items;
   }-*/;


}
