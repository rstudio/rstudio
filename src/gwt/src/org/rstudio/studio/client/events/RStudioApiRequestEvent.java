/*
 * RStudioApiRequestEvent.java
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
package org.rstudio.studio.client.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RStudioApiRequestEvent extends GwtEvent<RStudioApiRequestEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }
      
      public static final native Data create()
      /*-{
         return {
            type    : 0,
            sync    : false,
            target  : 0,
            payload : {}
         };
      }-*/;
      
      public final native int getType()
      /*-{
         return this.type || 0;
      }-*/;
      
      public final native boolean isSynchronous()
      /*-{
         return this.sync || false;
      }-*/;
      
      public final native int getTarget()
      /*-{
         return this.target || 0;
      }-*/;
      
      public final native JavaScriptObject getPayload()
      /*-{
         return this.payload || {};
      }-*/;
      
   }
   
   public RStudioApiRequestEvent(Data data)
   {
      data_ = data;
   }
   
   public Data getData()
   {
      return data_;
   }
   
   public JavaScriptObject getPayload()
   {
      return data_.getPayload();
   }
   
   private final Data data_;
   
   

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onRStudioApiRequest(RStudioApiRequestEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRStudioApiRequest(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
   
   
   // Event Data ----
   
   // list of events (keep in sync with Api.R)
   public static final int TYPE_UNKNOWN              = 0;
   public static final int TYPE_GET_EDITOR_SELECTION = 1;
   public static final int TYPE_SET_EDITOR_SELECTION = 2;
   public static final int TYPE_DOCUMENT_ID          = 3;
   public static final int TYPE_DOCUMENT_OPEN        = 4;
   public static final int TYPE_DOCUMENT_NEW         = 5;
   
   // list of potential event targets (keep in sync with Api.R)
   public static final int TARGET_UNKNOWN       = 0;
   public static final int TARGET_ACTIVE_WINDOW = 1;
   public static final int TARGET_ALL_WINDOWS   = 2;
   
   public static class GetEditorSelectionData extends JavaScriptObject
   {
      protected GetEditorSelectionData()
      {
      }

      public final native String getDocId() /*-{ return this["doc_id"]; }-*/;
   }
   
   public static class SetEditorSelectionData extends JavaScriptObject
   {
      protected SetEditorSelectionData()
      {
      }

      public final native String getValue() /*-{ return this["value"]; }-*/;
      public final native String getDocId() /*-{ return this["doc_id"]; }-*/;
   }
   
   public static class DocumentIdData extends JavaScriptObject
   {
      protected DocumentIdData()
      {
      }
      
      public final native boolean getAllowConsole() /*-{ return this["allow_console"]; }-*/;
   }
   
   public static class DocumentOpenData extends JavaScriptObject
   {
      protected DocumentOpenData()
      {
      }
      
      public final native String getPath() /*-{ return this["path"]; }-*/;
   }
   
   public static class DocumentNewData extends JavaScriptObject
   {
      protected DocumentNewData()
      {
      }
      
      public final native String  getType()       /*-{ return this["type"];    }-*/;
      public final native String  getCode()       /*-{ return this["code"];    }-*/;
      public final native int     getRow()        /*-{ return this["row"];     }-*/;
      public final native int     getColumn()     /*-{ return this["column"];  }-*/;
      public final native boolean getExecute()    /*-{ return this["execute"]; }-*/;
   }
   
   
   
}

