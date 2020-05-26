/*
 * DirtyState.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.UndoRedoEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.UndoRedoHandler;

public class DirtyState implements ReadOnlyValue<Boolean>
{
   public DirtyState(DocDisplay editor, boolean initialState)
   {
      editor_ = editor;

      if (initialState)
         markDirty(false);
      else
         markClean();

      editor_.addUndoRedoHandler(new UndoRedoHandler()
      {
         public void onUndoRedo(UndoRedoEvent event)
         {
            if (editor_.checkCleanStateToken(cleanUndoStateToken_))
               markClean();
            else
               markDirty(true);
         }
      });
   }

   public void markDirty(boolean allowUndoBackToClean)
   {
      if (!allowUndoBackToClean)
         cleanUndoStateToken_ = JavaScriptObject.createObject();

      if (!value_)
      {
         value_ = true;
         fire(value_);
      }
   }

   public void markClean()
   {
      cleanUndoStateToken_ = editor_.getCleanStateToken();
      if (value_)
      {
         value_ = false;
         fire(value_);
      }
   }

   public Boolean getValue()
   {
      return value_;
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler)
   {
      return handlers_.addHandler(ValueChangeEvent.getType(), handler);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   private void fire(boolean value)
   {
      ValueChangeEvent.fire(
            new HasValueChangeHandlers<Boolean>()
            {
               public HandlerRegistration addValueChangeHandler(
                     ValueChangeHandler<Boolean> handler)
               {
                  throw new UnsupportedOperationException();
               }

               public void fireEvent(GwtEvent<?> event)
               {
                  DirtyState.this.fireEvent(event);
               }
            }, value);
   }

   private final HandlerManager handlers_ = new HandlerManager(this);
   private JavaScriptObject cleanUndoStateToken_;
   private boolean value_;
   private final DocDisplay editor_;
}
