/*
 * AceEditorIdleMonitor.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.container.SafeMap;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;

public class AceEditorIdleMonitor
{
   interface IdleCommand
   {
      public void execute(AceEditor editor, IdleState state);
   }
   
   @Inject
   private void initialize(AceEditorIdleCommands idleCommands)
   {
      idleCommands_ = idleCommands;
   }
   
   public AceEditorIdleMonitor(AceEditor editor)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      editor_ = editor;
      monitors_ = new ArrayList<HandlerRegistration>();
      commands_ = new HashMap<HandlerRegistration, IdleCommand>();
      timer_ = new Timer()
      {
         @Override
         public void run()
         {
            executeIdleCommands(editor_, IdleState.STATE_CURSOR_IDLE);
         }
      };
      
      COMMAND_MAP.put(editor, commands_);
      
      registerCommands();
      beginMonitoring();
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
   
   private static void executeIdleCommands(AceEditor editor, int type)
   {
      IdleState state = new IdleState(type, mouseX_, mouseY_, modifiers_);
      Map<HandlerRegistration, IdleCommand> commandMap = COMMAND_MAP.get(editor);
      for (Map.Entry<HandlerRegistration, IdleCommand> entry : commandMap.entrySet())
      {
         IdleCommand command = entry.getValue();
         command.execute(editor, state);
      }
   }
   
   private void beginMonitoring()
   {
      monitors_.add(editor_.addCursorChangedHandler(new CursorChangedHandler()
      {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            mouseMovedLast_ = false;
            timer_.schedule(DELAY_MS);
         }
      }));
      
      monitors_.add(editor_.addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (!event.isAttached())
               onDetach();
         }
      }));
   }
   
   private void onDetach()
   {
      for (HandlerRegistration monitor : monitors_)
         monitor.removeHandler();
      monitors_.clear();
      COMMAND_MAP.remove(editor_);
      commands_.clear();
   }
   
   private void registerCommands()
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
   
   private final AceEditor editor_;
   private final List<HandlerRegistration> monitors_;
   private final Map<HandlerRegistration, IdleCommand> commands_;
   private final Timer timer_;
   
   private static boolean mouseMovedLast_;
   private static int modifiers_;
   private static int mouseX_;
   private static int mouseY_;
   
   private static final Timer MOUSE_MOVE_TIMER;
   private static final HandlerRegistration MOUSE_MOVE_HANDLER;
   private static final SafeMap<AceEditor, Map<HandlerRegistration, IdleCommand>> COMMAND_MAP;
   
   static {
      MOUSE_MOVE_TIMER = new Timer()
      {
         @Override
         public void run()
         {
            if (!mouseMovedLast_)
               return;
            
            Element el = DomUtils.elementFromPoint(mouseX_, mouseY_);
            AceEditor editor = AceEditor.getEditor(el);
            if (editor == null)
               return;
            
            executeIdleCommands(editor, IdleState.STATE_MOUSE_IDLE);
         }
      };
      
      MOUSE_MOVE_HANDLER = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent preview)
         {
            mouseMovedLast_ = preview.getTypeInt() == Event.ONMOUSEMOVE;
            if (!mouseMovedLast_)
               return;
            
            NativeEvent event = preview.getNativeEvent();
            modifiers_ = KeyboardShortcut.getModifierValue(event);
            mouseX_ = event.getClientX();
            mouseY_ = event.getClientY();
            MOUSE_MOVE_TIMER.schedule(DELAY_MS);
         }
      });
      
      COMMAND_MAP = new SafeMap<AceEditor, Map<HandlerRegistration, IdleCommand>>();
   }
   
   private static final int DELAY_MS = 700;
   
   // Injected ----
   private AceEditorIdleCommands idleCommands_;
}