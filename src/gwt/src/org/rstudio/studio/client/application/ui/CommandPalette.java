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

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyMap;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.command.KeyMap.KeyMapType;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.AriaLiveStatusWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.gwt.aria.client.ExpandedValue;
import com.google.gwt.aria.client.Id;
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

/**
 * CommandPalette is a widget that displays all available RStudio commands in a
 * searchable list.
 */
public class CommandPalette extends Composite
{
   private static CommandPaletteUiBinder uiBinder = GWT.create(CommandPaletteUiBinder.class);

   interface CommandPaletteUiBinder extends UiBinder<Widget, CommandPalette>
   {
   }
   
   /**
    * The host interface represents the class hosting the widget (not a widget
    * itself), which is currently the CommandPaletteLauncher.
    */
   public interface Host
   {
      void dismiss();
   }
   
   public interface Styles extends CssResource
   {
      String popup();
      String searchBox();
      String commandList();
      String commandPanel();
   }

   public CommandPalette(Commands commands, 
                         RAddins addins, 
                         List<CommandPaletteEntrySource> extraSources,
                         ShortcutManager shortcuts, 
                         Host host)
   {
      initWidget(uiBinder.createAndBindUi(this));

      entries_ = new ArrayList<>();
      host_ = host;
      shortcuts_ = shortcuts;
      selected_ = -1;
      addins_ = addins;
      commands_ = commands;
      extraEntriesSource_ = CommandPaletteEntrySource.join(extraSources);
      attached_ = false;
      pageSize_ = 0;
      styles_.ensureInjected();
      
      Element searchBox = searchBox_.getElement();
      searchBox.setAttribute("placeholder", "Search and run commands");
      searchBox.setAttribute("spellcheck", "false");

      // Accessibility attributes: list box
      Element commandList = commandList_.getElement();
      ElementIds.assignElementId(commandList, ElementIds.COMMAND_PALETTE_LIST);
      Roles.getListboxRole().set(commandList);
      Roles.getListboxRole().setAriaLabelProperty(commandList, "Matching commands");

      // Accessibility attributes: search box
      ElementIds.assignElementId(searchBox_, ElementIds.COMMAND_PALETTE_SEARCH);
      Roles.getComboboxRole().setAriaOwnsProperty(searchBox, Id.of(commandList_.getElement()));
      Roles.getComboboxRole().set(searchBox);
      Roles.getComboboxRole().setAriaLabelProperty(searchBox, "Search for and run a command");
      Roles.getComboboxRole().setAriaExpandedState(searchBox, ExpandedValue.TRUE);
      A11y.setARIAAutocomplete(searchBox_, "list");
      
      // Populate the palette on a deferred callback so that it appears immediately
      Scheduler.get().scheduleDeferred(() ->
      {
         populate();
      });
      
   }
   
   @Override
   public void onAttach()
   {
      super.onAttach();

      attached_ = true;

      // If we have already populated, compute the page size. Do this deferred
      // so that a render pass occurs (otherwise the page size computations will
      // take place with unrendered elements)
      if (entries_.size() > 0)
      {
         Scheduler.get().scheduleDeferred(() ->
         {
            computePageSize();
         });
      }
   }

   /**
    * Performs a one-time population of the palette with all available commands.
    */
   private void populate()
   {
      // Add all of the application commands
      KeyMap map = shortcuts_.getKeyMap(KeyMapType.APPLICATION);
      Map<String, AppCommand> allCommands = commands_.getCommands();
      for (String command: allCommands.keySet())
      {
         if (command.contains("Mru") || command.startsWith("mru") || 
               command.contains("Dummy"))
         {
            // MRU entries and dummy commands should not appear in the palette
            continue;
         }
         
         // Ensure the command is visible. It'd be nice to show all commands in
         // the palette for the purposes of examining key bindings, discovery,
         // etc., but invisible commands are generally meaningless in the 
         // current context.
         AppCommand appCommand = allCommands.get(command);
         if (!appCommand.isVisible())
         {
            continue;
         }

         // Look up the key binding for this command
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
      
      // add commands from additional sources
      List<CommandPaletteEntry> extraEntries = extraEntriesSource_.getCommandPaletteEntries();
      if (extraEntries != null)
         entries_.addAll(extraEntries);
      
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

      
      // Handle most keystrokes on KeyUp so that the contents of the text box
      // have already been changed
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
         // Ignore the Tab key so we don't lose focus accidentally (there is
         // only one focusable element in the palette and we don't want Tab to
         // dismiss it)
         if (evt.getNativeKeyCode() == KeyCode.TAB)
         {
            evt.stopPropagation();
            evt.preventDefault();
            return;
         }

         // Ignore modified arrows so that e.g. Shift Up/Down to select the
         // contents of the textbox work as expected
         if (evt.isAnyModifierKeyDown())
            return;
         
         if (evt.getNativeKeyCode() == KeyCode.UP)
         {
            // Directional keys often trigger behavior in textboxes (e.g. moving
            // the cursor to the beginning/end of text) but we're hijacking them
            // to do navigation in the results list, so disable that.
            evt.stopPropagation();
            evt.preventDefault();
            moveSelection(-1);
         }
         else if (evt.getNativeKeyCode() == KeyCode.DOWN)
         {
            evt.stopPropagation();
            evt.preventDefault();
            moveSelection(1);
         }
         else if (evt.getNativeKeyCode() == KeyCode.PAGE_UP)
         {
            // Page Up moves up by the page size (computed based on the size of
            // entries in the DOM)
            moveSelection(-1 * pageSize_);
         }
         else if (evt.getNativeKeyCode() == KeyCode.PAGE_DOWN)
         {
            moveSelection(pageSize_);
         }
      });
      
      // If we are already attached to the DOM at this point, compute the page
      // size for scrolling by pages. 
      if (attached_)
      {
         computePageSize();
      }
   }
   
   /**
    * Compute the size of a "page" of results (for Page Up / Page Down). We do
    * this dynamically based on measuring DOM elements since the number of items
    * that fit in a page can vary based on platform, browser, and available
    * fonts.
    */
   private void computePageSize()
   {
      // Find the first visible entry (we can't measure an invisible one)
      for (CommandPaletteEntry entry: entries_)
      {
         if (entry.isVisible())
         {
            // Compute the page size: the total size of the scrolling area
            // divided by the size of a visible entry
            pageSize_ = Math.floorDiv(scroller_.getOffsetHeight(), 
                  entry.getOffsetHeight());
            break;
         }
      }
      
      if (pageSize_ > 1)
      {
         // We want the virtual page to be very slightly smaller than the
         // physical page
         pageSize_--;
      }
      else
      {
         // Something went wrong and we got a tiny or meaningless page size. Use
         // 10 items as a default.
         pageSize_ = 10;
      }
   }
   
   /**
    * Filter the commands by the current contents of the search box
    */
   private void applyFilter()
   {
      int matches = 0;

      // Split the search text into a series of lowercase words. This provides a
      // kind of partial fuzzy matching, so that e.g., "new py" matches the command
      // "Create a new Python script".
      String[] needles = searchBox_.getText().toLowerCase().split("\\s+");
      
      for (CommandPaletteEntry entry: entries_)
      {
         String hay = entry.getLabel().toLowerCase();
         boolean matched = true;
         for (String needle: needles)
         {
            // The haystack doesn't have this needle, so this entry does not match.
            if (!hay.contains(needle))
            {
               entry.setVisible(false);
               matched = false;
               break;
            }
         }

         // We matched all needles, so highlight and show the entry.
         if (matched)
         {
            entry.setSearchHighlight(needles);
            entry.setVisible(true);
            matches++;
         }
      }
      
      // If not searching for anything, then searching for everything.
      if (needles.length == 0)
      {
         matches = entries_.size();
      }
      
      updateSelection();
      
      // Show "no results" message if appropriate
      if (matches == 0 && !noResults_.isVisible())
      {
         scroller_.setVisible(false);
         noResults_.setVisible(true);
      }
      else if (matches > 0 && noResults_.isVisible())
      {
         scroller_.setVisible(true);
         noResults_.setVisible(false);
      }

      // Report results count to screen reader
      resultsCount_.reportStatus(matches + " " +
            "commands found, press up and down to navigate",
            RStudioGinjector.INSTANCE.getUserPrefs().typingStatusDelayMs().getValue(),
            Severity.STATUS);
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
   
   /**
    * Changes the selected palette entry in response to user input.
    * 
    * @param units The number of units to move selection (negative to go
    *   backwards)
    */
   private void moveSelection(int units)
   {
      // Select the first visible command in the given direction
      CommandPaletteEntry candidate = null;

      int direction = units / Math.abs(units);  // -1 (backwards) or 1 (forwards)
      int consumed = 0; // Number of units consumed to goal
      int target = 0;   // Target element to select
      int pass = 1;     // Number of entries passed so far
      int viable = -1;  // The last visited viable (selectable) entry
      do
      {
         target = selected_ + (direction * pass++);
         if (target < 0 || target >= entries_.size())
         {
            // Request to navigate outside the boundaries of the palette
            break;
         }
         candidate = entries_.get(target);

         if (candidate.isVisible())
         {
            // This entry is visible, so it counts against our goal.
            consumed += direction;
            viable = target;
         }
      }
      while (consumed != units);
      
      // Select a viable entry if we found one; this may not be the desired
      // element but will be as far as we could move (e.g. requested to move
      // 20 units but had to stop at 15).
      if (viable >= 0)
      {
         selectNewCommand(viable);
      }
   }
   
   /**
    * Focuses the palette's search box in preparation for user input.
    */
   public void focus()
   {
      searchBox_.setFocus(true);
      updateSelection();
   }
   
   /**
    * Invoke the currently selected command.
    */
   private void invokeSelection()
   {
      if (selected_ >= 0)
      {
         host_.dismiss();
         entries_.get(selected_).invoke();
      }
   }
   
   /**
    * Change the selected command.
    * 
    * @param target The index of the command to select.
    */
   private void selectNewCommand(int target)
   {
      // No-op if target was already selected
      if (selected_ == target)
         return;
      
      // Clear previous selection, if any
      if (selected_ >= 0)
      {
         entries_.get(selected_).setSelected(false);
      }
      
      // Set new selection
      selected_ = target;
      CommandPaletteEntry selected = entries_.get(selected_);
      selected.setSelected(true);
      selected.getElement().scrollIntoView();

      // Update active descendant for accessibility
      Roles.getComboboxRole().setAriaActivedescendantProperty(
            searchBox_.getElement(), Id.of(selected.getElement()));
   }
   
   private final Host host_;
   private final ShortcutManager shortcuts_;
   private final Commands commands_;
   private final RAddins addins_;
   private final CommandPaletteEntrySource extraEntriesSource_;
   private int selected_;
   private List<CommandPaletteEntry> entries_;
   private String searchText_;
   private boolean attached_;
   private int pageSize_;

   @UiField public TextBox searchBox_;
   @UiField public FlowPanel commandList_;
   @UiField AriaLiveStatusWidget resultsCount_;
   @UiField HTMLPanel noResults_;
   @UiField ScrollPanel scroller_;
   @UiField Styles styles_;
}
