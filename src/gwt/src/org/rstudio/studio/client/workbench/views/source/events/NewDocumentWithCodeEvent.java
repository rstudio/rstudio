
/*
 * NewDocumentWithCodeEvent.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.events;

import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class NewDocumentWithCodeEvent 
   extends GwtEvent<NewDocumentWithCodeEvent.Handler>
{
   public final static String R_SCRIPT = "r_script";
   public final static String R_NOTEBOOK = "r_notebook";
   
   public interface Handler extends EventHandler
   {
      void onNewDocumentWithCode(NewDocumentWithCodeEvent e);
   }
   
   public NewDocumentWithCodeEvent(String type,
                                   String code,
                                   SourcePosition cursorPosition,
                                   boolean execute)
   {
      type_ = type;
      code_ = code;
      cursorPosition_ = cursorPosition;
      execute_ = execute;
   }

   public String getType()
   {
      return type_;
   }
   
   public String getCode()
   {
      return code_;
   }
   
   public SourcePosition getCursorPosition()
   {
      return cursorPosition_;
   }
   
   public boolean getExecute()
   {
      return execute_;
   }
  
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onNewDocumentWithCode(this);
   }

   private final String type_;
   private final String code_;
   private final SourcePosition cursorPosition_;
   private final boolean execute_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
   
   public static final int STATE_NONE = 0;
   public static final int STATE_DRAGGING = 1;
}