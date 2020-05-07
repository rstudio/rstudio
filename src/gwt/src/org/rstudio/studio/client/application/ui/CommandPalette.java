/*
 * CommandPalette.java
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

package org.rstudio.studio.client.application.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import elemental.events.KeyboardEvent.KeyCode;

public class CommandPalette extends Composite
{
   private static CommandPaletteUiBinder uiBinder = GWT.create(CommandPaletteUiBinder.class);

   interface CommandPaletteUiBinder extends UiBinder<Widget, CommandPalette>
   {
   }
   
   public interface Host
   {
      public void dismiss();
   }
   
   public interface Styles extends CssResource
   {
      String popup();
      String searchBox();
      String commandList();
      String commandPanel();
   }

   public CommandPalette(Commands commands, RAddins addins, ShortcutManager shortcuts, Host host)
   {
      initWidget(uiBinder.createAndBindUi(this));

      entries_ = new ArrayList<CommandPaletteEntry>();
      host_ = host;
      shortcuts_ = shortcuts;
      selected_ = -1;
      addins_ = addins;
      commands_ = commands;
      styles_.ensureInjected();
      
      // Populate the palette on a deferred callback so that it appears immediately
      Scheduler.get().scheduleDeferred(() ->
      {
         populate();
      });
   }
   
   private void populate()
   {
      // Add all of the application commands
      KeyMap map = shortcuts_.getKeyMap(KeyMapType.APPLICATION);
      Map<String, AppCommand> allCommands = commands_.getCommands();
      for (String command: allCommands.keySet())
      {
         if (command.contains("Mru") || command.contains("Dummy"))
         {
            // MRU entries and dummy commands should not appear in the palette
            continue;
         }

         // Look up the key binding for this command
         AppCommand appCommand = allCommands.get(command);
         List<KeySequence> keys = map.getBindings(command);
         
         // Create an application command entry
         CommandPaletteEntry entry = new AppCommandPaletteEntry(appCommand, keys);
         if (StringUtil.isNullOrEmpty(entry.getLabel()))
         {
            // Ignore app commands which have no label
            continue;
         }
         entries_.add(entry);
      }
      
      // Add all of the R addin commands
      map = shortcuts_.getKeyMap(KeyMapType.ADDIN);
      AddinExecutor executor = new AddinExecutor();
      for (String addin: JsUtil.asIterable(addins_.keys()))
      {
         RAddin rAddin = addins_.get(addin);
         
         // Look up the key binding for this addin
         List<KeySequence> keys = map.getBindings(rAddin.getId());
         CommandPaletteEntry entry = new RAddinCommandPaletteEntry(rAddin, executor, keys);
         if (StringUtil.isNullOrEmpty(entry.getLabel()))
         {
            // Ignore addin commands which have no label
            continue;
         }
         entries_.add(entry);
      }
      
      // Invoke commands when they're clicked on
      for (CommandPaletteEntry entry: entries_)
      {
         entry.sinkEvents(Event.ONCLICK);
         entry.addHandler((evt) -> {
            host_.dismiss();
            entry.invoke();
         }, ClickEvent.getType());
         commandList_.add(entry);
      }

      searchBox_.getElement().setAttribute("placeholder", "Search and run commands");
      searchBox_.getElement().setAttribute("spellcheck", "false");
      
      searchBox_.addKeyUpHandler((evt) -> 
      {
         if (evt.getNativeKeyCode() == KeyCode.UP)
         {
            moveSelection(-1);
         }
         else if (evt.getNativeKeyCode() == KeyCode.DOWN)
         {
            moveSelection(1);
         }
         else if (evt.getNativeKeyCode() == KeyCode.ESC)
         {
            host_.dismiss();
         }
         else if (evt.getNativeKeyCode() == KeyCode.ENTER)
         {
            invokeSelection();
         }
         else
         {
            // just update the filter
            applyFilter();
         }
      });
   }
   
   public void applyFilter()
   {
      String needle = searchBox_.getText().toLowerCase();
      for (CommandPaletteEntry entry: entries_)
      {
         String hay = entry.getLabel().toLowerCase();
         if (hay.contains(needle))
         {
            entry.setSearchHighlight(needle);
            entry.setVisible(true);
         }
         else
         {
            entry.setVisible(false);
         }
      }

      updateSelection();
   }
   
   /**
    * Selects the topmost search result in the palette
    */
   public void updateSelection()
   {
      for (int i = 0; i < entries_.size(); i++)
      {
         if (!entries_.get(i).isVisible())
         {
            continue;
         }
         
         selectNewCommand(i);
         break;
      }
   }
   
   private void moveSelection(int units)
   {
      // Select the first visible command in the given direction
      CommandPaletteEntry candidate = null;
      int pass = 1;
      int target = 0;
      do
      {
         target = selected_ + (units * pass++);
         if (target < 0 || target >= entries_.size())
         {
            // Request to navigate outside the boundaries of the palette
            return;
         }
         candidate = entries_.get(target);
      }
      while (!candidate.isVisible());
      
      selectNewCommand(target);
   }
   
   public void focus()
   {
      searchBox_.setFocus(true);
      updateSelection();
   }
   
   private void invokeSelection()
   {
      if (selected_ >= 0)
      {
         host_.dismiss();
         entries_.get(selected_).invoke();
      }
   }
   
   private void selectNewCommand(int target)
   {
      // Clear previous selection, if any
      if (selected_ >= 0)
      {
         entries_.get(selected_).setSelected(false);
      }
            
      // Set new selection
      selected_ = target;
      entries_.get(selected_).setSelected(true);
      scroller_.ensureVisible(entries_.get(selected_));
   }
   
   private final Host host_;
   private final ShortcutManager shortcuts_;
   private final Commands commands_;
   private final RAddins addins_;
   private int selected_;
   private List<CommandPaletteEntry> entries_;

   @UiField public TextBox searchBox_;
   @UiField public VerticalPanel commandList_;
   @UiField ScrollPanel scroller_;
   @UiField Styles styles_;
}
