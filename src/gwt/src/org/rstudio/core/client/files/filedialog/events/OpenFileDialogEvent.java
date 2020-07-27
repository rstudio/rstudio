/*
 * OpenFileDialogEvent.java
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
package org.rstudio.core.client.files.filedialog.events;

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class OpenFileDialogEvent extends GwtEvent<OpenFileDialogEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native int getType()            /*-{ return this.type;     }-*/;
      public final native String getCaption()      /*-{ return this.caption;  }-*/;
      public final native String getLabel()        /*-{ return this.label;    }-*/;
      public final native FileSystemItem getFile() /*-{ return this.file;     }-*/;
      public final native String getFilter()       /*-{ return this.filter;   }-*/;
      public final native boolean selectExisting() /*-{ return this.existing; }-*/;
   }

   public OpenFileDialogEvent(Data data)
   {
      data_ = data;
   }

   public final int getType()            { return data_.getType();        }
   public final String getCaption()      { return data_.getCaption();     }
   public final String getLabel()        { return data_.getLabel();       }
   public final FileSystemItem getFile() { return data_.getFile();        }
   public final String getFilter()       { return data_.getFilter();      }
   public final boolean selectExisting() { return data_.selectExisting(); }

   private final Data data_;

   public static final int TYPE_SELECT_FILE      = 1;
   public static final int TYPE_SELECT_DIRECTORY = 2;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onOpenFileDialog(OpenFileDialogEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onOpenFileDialog(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
