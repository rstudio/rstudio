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

import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
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

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.LiveValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
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
      
      Element searchBox = searchBox_.getElement();
      searchBox.setAttribute("placeholder", "Search and run commands");
      searchBox.setAttribute("spellcheck", "false");

      // Accessibility attributes: list box
      Roles.getListboxRole().set(commandList_.getElement());
      ElementIds.assignElementId(commandList_, ElementIds.COMMAND_PALETTE_LIST);

      // Accessibility attributes: search box
      ElementIds.assignElementId(searchBox_, ElementIds.COMMAND_PALETTE_SEARCH);
      Roles.getComboboxRole().setAriaOwnsProperty(searchBox, Id.of(commandList_.getElement()));
      Roles.getComboboxRole().set(searchBox);
      Roles.getComboboxRole().setAriaLabelProperty(searchBox, "Search for and run a command");
      A11y.setARIAAutocomplete(searchBox_, "list");
      
      // Accessibility attributes: announcement region; we want this to be read
      // when filter updates are complete
      A11y.setVisuallyHidden(resultsCount_);
      Roles.getAlertRole().setAriaLiveProperty(resultsCount_.getElement(), LiveValue.ASSERTIVE);

      // Populate the palette on a deferred callback so that it appears immediately
      Scheduler.get().scheduleDeferred(() ->
      {
         populate();
      });
      
      // Debounced update of the result count for screen readers
      updateResultsCount_ = new DebouncedCommand(1000)
      {
         @Override
         protected void execute()
         {
            int count = 0;
            for (CommandPaletteEntry entry: entries_)
            {
               if (entry.isVisible())
                  count++;
            }
            resultsCount_.getElement().setInnerText(count + " " +
                  "commands found, press up and down to navigate");
         }
      };
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

      
      searchBox_.addKeyUpHandler((evt) ->
      {
         if (evt.getNativeKeyCode() == KeyCode.ESC)
         {
            // Pressing ESC dismisses the host (removing the palette popup)
            host_.dismiss();
         }
         else if (evt.getNativeKeyCode() == KeyCode.ENTER)
         {
            // Enter runs the selected command
            invokeSelection();
         }
         else
         {
            // Just update the filter if the text has changed
            String searchText = searchBox_.getText();
            if (!StringUtil.equals(searchText_, searchText))
            {
               searchText_ = searchText;
               applyFilter();
            }
         }
      });

      // Up and Down arrows need to be handled on KeyDown to account for
      // repetition (a held arrow key will generate multiple KeyDown events and
      // then a single KeyUp when released)
      searchBox_.addKeyDownHandler((evt) -> 
      {
         if (evt.getNativeKeyCode() == KeyCode.UP)
         {
            moveSelection(-1);
         }
         else if (evt.getNativeKeyCode() == KeyCode.DOWN)
         {
            moveSelection(1);
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
      
      updateResultsCount_.nudge();
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
      CommandPaletteEntry selected = entries_.get(selected_);
      selected.setSelected(true);
      scroller_.ensureVisible(selected);

      // Update active descendant for accessibility
      Roles.getComboboxRole().setAriaActivedescendantProperty(
            searchBox_.getElement(), Id.of(selected.getElement()));
   }
   
   private final Host host_;
   private final ShortcutManager shortcuts_;
   private final Commands commands_;
   private final RAddins addins_;
   private final DebouncedCommand updateResultsCount_;
   private int selected_;
   private List<CommandPaletteEntry> entries_;
   private String searchText_;

   @UiField public TextBox searchBox_;
   @UiField public FlowPanel commandList_;
   @UiField HTMLPanel resultsCount_;
   @UiField ScrollPanel scroller_;
   @UiField Styles styles_;
}
