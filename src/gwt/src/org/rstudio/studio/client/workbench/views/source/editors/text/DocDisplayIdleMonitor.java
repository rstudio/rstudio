/*
 * DocDisplayIdleMonitor.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorModeChangedEvent;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public class DocDisplayIdleMonitor
{
   interface IdleCommand
   {
      public void execute(DocDisplay docDisplay, IdleState state);
   }
   
   public DocDisplayIdleMonitor(DocDisplay docDisplay)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      docDisplay_ = docDisplay;
      handlers_ = new ArrayList<HandlerRegistration>();
      monitors_ = new ArrayList<HandlerRegistration>();
      commands_ = new HashMap<HandlerRegistration, IdleCommand>();
      timer_ = new Timer()
      {
         @Override
         public void run()
         {
            int type = cursorMovedLast_
                  ? IdleState.STATE_CURSOR_IDLE
                  : IdleState.STATE_MOUSE_IDLE;
            
            IdleState state = new IdleState(type, mouseX_, mouseY_, modifiers_);
            for (Map.Entry<HandlerRegistration, IdleCommand> entry : commands_.entrySet())
            {
               IdleCommand command = entry.getValue();
               command.execute(docDisplay_, state);
            }
         }
      };
      
      attachCommands();
      attachHandlers();
   }
   
   public HandlerRegistration registerCommand(final IdleCommand command)
   {
      HandlerRegistration registration = new HandlerRegistration()
      {
         @Override
         public void removeHandler()
         {
            commands_.remove(this);
         }
      };
      commands_.put(registration, command);
      return registration;
   }
   
   // Private Methods ----
   
   @Inject
   private void initialize(DocDisplayIdleCommands idleCommands)
   {
      idleCommands_ = idleCommands;
   }
   
   private void attachHandlers()
   {
      detachHandlers();
      
      handlers_.add(docDisplay_.addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            beginMonitoring();
         }
      }));
      
      handlers_.add(docDisplay_.addBlurHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            endMonitoring();
         }
      }));
      
      handlers_.add(docDisplay_.addEditorModeChangedHandler(new EditorModeChangedEvent.Handler()
      {
         @Override
         public void onEditorModeChanged(EditorModeChangedEvent event)
         {
            beginMonitoring();
         }
      }));
      
      handlers_.add(docDisplay_.addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (!event.isAttached())
               onDetach();
         }
      }));
   }
   
   private void detachHandlers()
   {
      detach(handlers_);
   }
   
   private void beginMonitoring()
   {
      endMonitoring();
      
      if (!docDisplay_.isFocused())
         return;
      
      monitors_.add(Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent preview)
         {
            if (preview.getTypeInt() != Event.ONMOUSEMOVE)
               return;
            
            NativeEvent event = preview.getNativeEvent();
            cursorMovedLast_ = false;
            modifiers_ = KeyboardShortcut.getModifierValue(event);
            mouseX_ = event.getClientX();
            mouseY_ = event.getClientY();
            timer_.schedule(DELAY_MS);
         }
      }));
      
      monitors_.add(docDisplay_.addCursorChangedHandler(new CursorChangedHandler()
      {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            cursorMovedLast_ = true;
            timer_.schedule(DELAY_MS);
         }
      }));
   }
   
   private void endMonitoring()
   {
      detach(monitors_);
   }
   
   private void detach(List<HandlerRegistration> handlers)
   {
      for (HandlerRegistration handler : handlers)
         handler.removeHandler();
      handlers.clear();
   }
   
   private void onDetach()
   {
      detach(handlers_);
      detach(monitors_);
      commands_.clear();
   }
   
   private void attachCommands()
   {
      registerCommand(idleCommands_.PREVIEW_LINK);
      registerCommand(idleCommands_.PREVIEW_LATEX);
   }
   
   public static class IdleState
   {
      public IdleState(int type, int mouseX, int mouseY, int modifiers)
      {
         type_ = type;
         mouseX_ = mouseX;
         mouseY_ = mouseY;
         modifiers_ = modifiers;
      }
      
      public int getType()      { return type_; }
      public int getMouseX()    { return mouseX_; }
      public int getMouseY()    { return mouseY_; }
      public int getModifiers() { return modifiers_; }
      
      public static final int STATE_CURSOR_IDLE = 0;
      public static final int STATE_MOUSE_IDLE  = 1;
      
      private final int type_;
      private final int mouseX_;
      private final int mouseY_;
      private final int modifiers_;
   }
   
   private final DocDisplay docDisplay_;
   private final List<HandlerRegistration> handlers_;
   private final List<HandlerRegistration> monitors_;
   private final Map<HandlerRegistration, IdleCommand> commands_;
   private final Timer timer_;
   
   private boolean cursorMovedLast_;
   private int modifiers_;
   private int mouseX_;
   private int mouseY_;
   
   private static final int DELAY_MS = 700;
   
   // Injected ----
   private DocDisplayIdleCommands idleCommands_;
}
