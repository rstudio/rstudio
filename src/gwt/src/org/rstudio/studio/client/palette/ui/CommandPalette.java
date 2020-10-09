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

package org.rstudio.studio.client.palette.ui;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.widget.AriaLiveStatusWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.palette.model.CommandPaletteItem.InvocationSource;

import com.google.gwt.aria.client.ExpandedValue;
import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

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

   public CommandPalette(List<CommandPaletteEntrySource> sources, Host host)
   {
      initWidget(uiBinder.createAndBindUi(this));

      items_ = new ArrayList<>();
      visible_ = new ArrayList<>();
      host_ = host;
      selected_ = -1;
      attached_ = false;
      pageSize_ = 0;
      sources_ = sources;
      needles_ = new String[0];
      registrations_ = new HandlerRegistrations();
      styles_.ensureInjected();
      
      Element searchBox = searchBox_.getElement();
      searchBox.setAttribute("spellcheck", "false");
      searchBox.setAttribute("autocomplete", "off");

      // Accessibility attributes: list box
      Element commandList = commandList_.getElement();
      ElementIds.assignElementId(commandList, ElementIds.COMMAND_PALETTE_LIST);
      Roles.getListboxRole().set(commandList);
      Roles.getListboxRole().setAriaLabelProperty(commandList, "Matching commands and settings");

      // Accessibility attributes: search box
      ElementIds.assignElementId(searchBox_, ElementIds.COMMAND_PALETTE_SEARCH);
      Roles.getComboboxRole().setAriaOwnsProperty(searchBox, Id.of(commandList_.getElement()));
      Roles.getComboboxRole().set(searchBox);
      Roles.getComboboxRole().setAriaLabelProperty(searchBox, "Search for commands and settings");
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
      if (commandList_.getWidgetCount() > 0)
      {
         Scheduler.get().scheduleDeferred(() ->
         {
            computePageSize();
         });
      }
   }
   
   @Override
   public void onDetach()
   {
      // Clean up event handlers
      registrations_.removeHandler();
   }

   /**
    * Performs a one-time population of the palette with all available commands.
    */
   private void populate()
   {
      // Handle most keystrokes on KeyUp so that the contents of the text box
      // have already been changed
      searchBox_.addKeyUpHandler((evt) ->
      {
         if (evt.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
         {
            // Pressing ESC dismisses the host (removing the palette popup)
            host_.dismiss();
         }
         else
         {
            // Just update the filter if the text has changed
            String searchText = searchBox_.getText();
            if (!StringUtil.equals(searchText_, searchText))
            {
               searchText_ = searchText;
               needles_ = searchBox_.getText().toLowerCase().split("\\s+");
               applyFilter_.nudge();
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
         if (evt.getNativeKeyCode() == KeyCodes.KEY_TAB)
         {
            evt.stopPropagation();
            evt.preventDefault();
            return;
         }
         else if (evt.getNativeKeyCode() == KeyCodes.KEY_ENTER)
         {
            // Enter runs the selected command. Turn off default behavior so
            // that the Enter key-up isn't handled by the IDE once the palette
            // is dismissed.
            evt.stopPropagation();
            evt.preventDefault();
            invokeSelection();
         }

         // Ignore modified arrows so that e.g. Shift Up/Down to select the
         // contents of the textbox work as expected
         if (evt.isAnyModifierKeyDown())
            return;
         
         if (evt.getNativeKeyCode() == KeyCodes.KEY_UP)
         {
            // Directional keys often trigger behavior in textboxes (e.g. moving
            // the cursor to the beginning/end of text) but we're hijacking them
            // to do navigation in the results list, so disable that.
            evt.stopPropagation();
            evt.preventDefault();
            moveSelection(-1);
         }
         else if (evt.getNativeKeyCode() == KeyCodes.KEY_DOWN)
         {
            evt.stopPropagation();
            evt.preventDefault();
            moveSelection(1);
         }
         else if (evt.getNativeKeyCode() == KeyCodes.KEY_PAGEUP)
         {
            // Page Up moves up by the page size (computed based on the size of
            // entries in the DOM)
            moveSelection(-1 * pageSize_);
         }
         else if (evt.getNativeKeyCode() == KeyCodes.KEY_PAGEDOWN)
         {
            moveSelection(pageSize_);
         }
      });
      
      // Render the first page of elements
      renderNextPage();
      
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
      for (CommandPaletteItem item: items_)
      {
         Widget entry = item.asWidget();
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
      // Clear the command list and render marker in preparation for a re-render
      commandList_.clear();
      renderedItem_ = 0;
      if (selected_ >= 0)
         visible_.get(selected_).setSelected(false);
      visible_.clear();

      selected_ = -1;
      
      // Render the next page of command entries
      renderNextPage();
   }
   
   /**
    * Runs when the render pass is completed.
    */
   private void completeRender()
   {
      int matches = commandList_.getWidgetCount();
      
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
    * Changes the selected palette entry in response to user input.
    * 
    * @param units The number of units to move selection (negative to go
    *   backwards)
    */
   private void moveSelection(int units)
   {
      // Identify target element
      int target = selected_ + units;

      // Clip to boundaries of display
      if (target < 0)
      {
         target = 0;
      }
      else if (target >= visible_.size())
      {
         target = visible_.size() - 1;
      }

      // Select new command if we moved
      if (target != selected_)
      {
         selectNewCommand(target);
      }
   }
   
   /**
    * Focuses the palette's search box in preparation for user input.
    */
   public void focus()
   {
      searchBox_.setFocus(true);
   }
   
   /**
    * Invoke the currently selected command.
    */
   private void invokeSelection()
   {
      if (selected_ >= 0)
      {
         if (visible_.get(selected_).dismissOnInvoke())
         {
            host_.dismiss();
         }
         visible_.get(selected_).invoke(InvocationSource.Keyboard);
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
         visible_.get(selected_).setSelected(false);
      }
      
      // Set new selection
      selected_ = target;
      CommandPaletteItem selected = visible_.get(selected_);
      selected.setSelected(true);
      selected.asWidget().getElement().scrollIntoView();

      // Update active descendant for accessibility
      Roles.getComboboxRole().setAriaActivedescendantProperty(
            searchBox_.getElement(), Id.of(selected.asWidget().getElement()));
   }
   
   /**
    * Renders the next page of search results from the command palette.
    * 
    * By far the slowest part of the command palette is the rendering of
    * individual items into GWT widgets, so doing this all at once would cause
    * the palette to take several seconds to appear. To make it performant, we
    * render just a few widgets at a time, letting the browser do a render
    * pass after each batch. 
    */
   private void renderNextPage()
   {
      // If we haven't already pulled items from all our sources and we have
      // less than a page of data left, pull in data from the next source.
      if (renderedSource_ < sources_.size() &&
          items_.size() - renderedItem_ < RENDER_PAGE_SIZE)
      {
         // Read the next non-null data source
         List<CommandPaletteItem> items = null;
         do
         {
            items = sources_.get(renderedSource_).getCommandPaletteItems();
            renderedSource_++;
         } while (items == null);
            
         items_.addAll(items);
      }
      
      // Set initial conditions for render loop
      int rendered = 0;
      int idx = renderedItem_;

      // Main render loop; render items until we have rendered a full page
      while (idx < items_.size() && rendered < RENDER_PAGE_SIZE)
      {
         CommandPaletteItem item = items_.get(idx);

         // Render this item if non-null and matches the search keywords, if we
         // have them
         if (item != null && (needles_.length == 0 || item.matchesSearch(needles_)))
         {
            // Remember whether this item has been rendered
            boolean isRendered = item.isRendered();
            
            // Render the item to a widget (this is the expensive step)
            Widget widget = item.asWidget();
            if (widget != null)
            {
               // Add and highlight the item
               commandList_.add(item.asWidget());
               visible_.add(item);
               item.setSearchHighlight(needles_);
               
               // If we just added the first widget to the box, select it
               if (visible_.size() == 1)
               {
                  selectNewCommand(0);
               }
               rendered++;
            }
            
            // Attach an invocation handler if this is the first time we've
            // rendered this item
            if (!isRendered)
            {
               registrations_.add(item.addInvokeHandler((evt) ->
               {
                  if (evt.getItem().dismissOnInvoke())
                  {
                     host_.dismiss();
                  }
                  evt.getItem().invoke(InvocationSource.Mouse);
               }));
            }
         }
         
         // Advance to next command palette item
         idx++;
      }
      
      // Save our place so we'll start rendering at the next page
      renderedItem_ = idx;
      
      // If we didn't render everything, schedule another pass
      if (renderedItem_ < items_.size() || renderedSource_ < sources_.size())
      {
         // Don't populate while user is typing as dumping more elements into
         // the DOM is distracting (plus the additional elements will be
         // discarded once the timer finishes running)
         if (!applyFilter_.isRunning())
         {
            Scheduler.get().scheduleDeferred(() ->
            {
               renderNextPage();
            });
         }
      }
      else
      {
         completeRender();
      }
   }
   
   private final Host host_;
   private final List<CommandPaletteEntrySource> sources_;
   private final List<CommandPaletteItem> items_;
   private final List<CommandPaletteItem> visible_;
   private final HandlerRegistrations registrations_;
   private int selected_;
   private String searchText_;
   private String[] needles_;
   private boolean attached_;
   private int pageSize_;
   
   private int renderedItem_; // The index of the last rendered item
   private int renderedSource_ = 0; // The index of the last rendered data source
   private final int RENDER_PAGE_SIZE = 50;

   DebouncedCommand applyFilter_ = new DebouncedCommand(100)
   {
      @Override
      protected void execute()
      {
         applyFilter();
      }
   };

   @UiField public TextBox searchBox_;
   @UiField public HTMLPanel commandList_;
   @UiField AriaLiveStatusWidget resultsCount_;
   @UiField HTMLPanel noResults_;
   @UiField ScrollPanel scroller_;
   @UiField Styles styles_;
}
