/*
 * ModifyKeyboardShortcutsWidget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.widget;

import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.*;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBinding;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.command.ShortcutManager.Handle;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
import org.rstudio.core.client.events.EditorKeybindingsChangedEvent;
import org.rstudio.core.client.events.RStudioKeybindingsChangedEvent;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.AddinsMRUList;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;
import org.rstudio.studio.client.workbench.addins.AddinsKeyBindingsChangedEvent;
import org.rstudio.studio.client.workbench.addins.AddinsServerOperations;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.console.shell.assist.PopupPositioner;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyKeyboardShortcutsWidget extends ModalDialogBase
{
   public static class KeyboardShortcutEntry
   {
      public KeyboardShortcutEntry(String id,
                                   String displayName,
                                   KeySequence keySequence,
                                   int commandType,
                                   boolean isCustom,
                                   AppCommand.Context context)
      {
         id_ = id;
         name_ = displayName;
         keySequence_ = keySequence;
         commandType_ = commandType;
         isCustom_ = isCustom;
         context_ = context;
      }
      
      public String getId()
      {
         return id_;
      }
      
      public String getName()
      {
         return name_;
      }
      
      public KeySequence getKeySequence()
      {
         if (newKeySequence_ != null)
            return newKeySequence_;
         
         return keySequence_;
      }
      
      public int getCommandType()
      {
         return commandType_;
      }
      
      public String getDisplayType()
      {
         if (commandType_ == TYPE_EDITOR_COMMAND)
            return "Editor";
         
         return context_.toString();
      }
      
      public boolean isCustomBinding()
      {
         return isCustom_;
      }
      
      public AppCommand.Context getContext()
      {
         return context_;
      }
      
      public void setDefaultKeySequence(KeySequence keys)
      {
         keySequence_ = keys.clone();
         newKeySequence_ = null;
      }
      
      public void setKeySequence(KeySequence keys)
      {
         if (keys.equals(keySequence_))
            newKeySequence_ = null;
         else
            newKeySequence_ = keys.clone();
      }
      
      public KeySequence getOriginalKeySequence()
      {
         return keySequence_;
      }
      
      public void restoreOriginalKeySequence()
      {
         newKeySequence_ = null;
      }
      
      public boolean isModified()
      {
         return newKeySequence_ != null;
      }
      
      @Override
      public boolean equals(Object object)
      {
         if (object == null || !(object instanceof KeyboardShortcutEntry))
            return false;
         
         KeyboardShortcutEntry other = (KeyboardShortcutEntry) object;
         return
               commandType_ == other.commandType_ &&
               id_.equals(other.id_);
      }
      
      private final String id_;
      private final String name_;
      private final int commandType_;
      private final AppCommand.Context context_;
      
      private boolean isCustom_ = false;
      private KeySequence keySequence_;
      private KeySequence newKeySequence_;
      
      public static final int TYPE_RSTUDIO_COMMAND = 1; // RStudio AppCommands
      public static final int TYPE_EDITOR_COMMAND  = 2; // e.g. Ace commands
      public static final int TYPE_ADDIN           = 3; 
   }
   
   private static interface ValueGetter<T>
   {
      public String getValue(T object);
   }
   
   public ModifyKeyboardShortcutsWidget()
   {
      this(null);
   }
   
   public ModifyKeyboardShortcutsWidget(String filterText)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      initialFilterText_ = filterText;
      shortcuts_ = ShortcutManager.INSTANCE;
      
      changes_ = new HashMap<KeyboardShortcutEntry, KeyboardShortcutEntry>();
      buffer_ = new KeySequence();
      
      table_ = new DataGrid<KeyboardShortcutEntry>(1000, RES, KEY_PROVIDER);
      
      FlowPanel emptyWidget = new FlowPanel();
      Label emptyLabel = new Label("No bindings available");
      emptyLabel.getElement().getStyle().setMarginTop(20, Unit.PX);
      emptyLabel.getElement().getStyle().setColor("#888");
      emptyWidget.add(emptyLabel);
      table_.setEmptyTableWidget(emptyWidget);
      
      table_.setWidth("700px");
      table_.setHeight("400px");
      
      // Add a 'global' click handler that performs a row selection regardless
      // of the cell clicked (it seems GWT clicks can be 'fussy' about whether
      // you click on the contents of a cell vs. the '<td>' element itself)
      table_.addDomHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            Element el = event.getNativeEvent().getEventTarget().cast();
            Element rowEl = DomUtils.findParentElement(el, new ElementPredicate()
            {
               @Override
               public boolean test(Element el)
               {
                  return el.getTagName().toLowerCase().equals("tr");
               }
            });
            
            if (rowEl == null)
               return;
            
            if (rowEl.hasAttribute("__gwt_row"))
            {
               int row = StringUtil.parseInt(rowEl.getAttribute("__gwt_row"), -1);
               if (row != -1)
               {
                  event.stopPropagation();
                  event.preventDefault();
                  table_.setKeyboardSelectedRow(row);
                  table_.setKeyboardSelectedColumn(0);
               }
            }
         }
      }, ClickEvent.getType());
      
      table_.setKeyboardSelectionHandler(new CellPreviewEvent.Handler<KeyboardShortcutEntry>()
      {
         private final AbstractCellTable.CellTableKeyboardSelectionHandler<KeyboardShortcutEntry> handler_ =
               new AbstractCellTable.CellTableKeyboardSelectionHandler<KeyboardShortcutEntry>(table_);
         
         @Override
         public void onCellPreview(CellPreviewEvent<KeyboardShortcutEntry> preview)
         {
            NativeEvent event = preview.getNativeEvent();
            int code = event.getKeyCode();
            
            // Don't let arrow keys change the selection when a shortcut cell
            // has been selected.
            if (preview.getColumn() == 1)
            {
               if (code == KeyCodes.KEY_UP ||
                   code == KeyCodes.KEY_DOWN ||
                   code == KeyCodes.KEY_LEFT ||
                   code == KeyCodes.KEY_RIGHT)
               {
                  return;
               }
            }
            
            // Also disable 'left', 'right' keys as they can 'navigate' the widget
            // into an unusable state.
            if (code == KeyCodes.KEY_LEFT ||
                code == KeyCodes.KEY_RIGHT)
            {
               return;
            }
            
            handler_.onCellPreview(preview);
         }
      });
      
      dataProvider_ = new ListDataProvider<KeyboardShortcutEntry>();
      dataProvider_.addDataDisplay(table_);
      
      addColumns();
      addHandlers();
      
      setText("Keyboard Shortcuts");
      addOkButton(new ThemedButton("Apply", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            applyChanges();
         }
      }));
      
      addCancelButton();
      
      radioAll_ = radioButton("All", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            filter();
         }
      });
      
      radioCustomized_ = radioButton("Customized", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            filter();
         }
      });
      
      filterWidget_ = new SearchWidget(new SuggestOracle() {

         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<Suggestion>()));
         }
         
      });
      
      filterWidget_.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            filter();
         }
      });
      
      filterWidget_.setPlaceholderText("Filter...");
      
      addLeftWidget(new ThemedButton("Reset...", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            globalDisplay_.showYesNoMessage(
                  GlobalDisplay.MSG_QUESTION,
                  "Reset Keyboard Shortcuts",
                  "Are you sure you want to reset keyboard shortcuts to their default values? " +
                  "This action cannot be undone.",
                  new ProgressOperation()
                  {
                     @Override
                     public void execute(final ProgressIndicator indicator)
                     {
                        indicator.onProgress("Resetting Keyboard Shortcuts...");
                        appCommands_.resetBindings(new CommandWithArg<EditorKeyBindings>()
                        {
                           @Override
                           public void execute(EditorKeyBindings appBindings)
                           {
                              editorCommands_.resetBindings(new Command()
                              {
                                 @Override
                                 public void execute()
                                 {
                                    addins_.resetBindings(new Command()
                                    {
                                       @Override
                                       public void execute()
                                       {
                                          indicator.onCompleted();
                                          ShortcutManager.INSTANCE.resetAppCommandBindings();
                                          resetState();
                                       }
                                    });
                                 }
                              });
                           }
                        });
                     }
                  },
                  false);
         }
      }));
   }
   
   private void applyChanges()
   {
      // Build up command diffs for save after application
      final EditorKeyBindings editorBindings = EditorKeyBindings.create();
      final EditorKeyBindings appBindings = EditorKeyBindings.create();
      final EditorKeyBindings addinBindings = EditorKeyBindings.create();
      
      // Loop through all changes and apply based on type
      for (Map.Entry<KeyboardShortcutEntry, KeyboardShortcutEntry> entry : changes_.entrySet())
      {
         KeyboardShortcutEntry newBinding = entry.getValue();
         String id = newBinding.getId();
         
         // Get all commands with this ID.
         List<KeyboardShortcutEntry> bindingsWithId = new ArrayList<KeyboardShortcutEntry>();
         for (KeyboardShortcutEntry binding : originalBindings_)
            if (binding.getId().equals(id))
               bindingsWithId.add(binding);
         
         // Collect all shortcuts.
         List<KeySequence> keys = new ArrayList<KeySequence>();
         for (KeyboardShortcutEntry binding : bindingsWithId)
            keys.add(binding.getKeySequence());
            
         int commandType = newBinding.getCommandType();
         
         if (commandType == KeyboardShortcutEntry.TYPE_RSTUDIO_COMMAND)
            appBindings.setBindings(id, keys);
         else if (commandType == KeyboardShortcutEntry.TYPE_EDITOR_COMMAND)
            editorBindings.setBindings(id, keys);
         else if (commandType == KeyboardShortcutEntry.TYPE_ADDIN)
            addinBindings.setBindings(id, keys);
      }
      
      // Tell satellites that they need to update bindings.
      appCommands_.addBindingsAndSave(appBindings, new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(EditorKeyBindings bindings)
         {
            events_.fireEventToAllSatellites(new RStudioKeybindingsChangedEvent(bindings));
         }
      });
      
      editorCommands_.addBindingsAndSave(editorBindings, new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(EditorKeyBindings bindings)
         {
            events_.fireEventToAllSatellites(new EditorKeybindingsChangedEvent(bindings));
         }
      });
      
      addins_.addBindingsAndSave(addinBindings, new CommandWithArg<EditorKeyBindings>()
      {
         @Override
         public void execute(EditorKeyBindings bindings)
         {
            events_.fireEvent(new AddinsKeyBindingsChangedEvent(bindings));
         }
      });
      
      closeDialog();
   }
   
   @Inject
   public void initialize(EditorCommandManager editorCommands,
                          ApplicationCommandManager appCommands,
                          AddinsCommandManager addins,
                          AddinsServerOperations addinsServer,
                          Commands commands,
                          GlobalDisplay globalDisplay,
                          EventBus events,
                          AddinsMRUList mruAddins)
   {
      editorCommands_ = editorCommands;
      appCommands_ = appCommands;
      addins_ = addins;
      addinsServer_ = addinsServer;
      commands_ = commands;
      globalDisplay_ = globalDisplay;
      events_ = events;
      mruAddins_ = mruAddins;
   }
   
   private void addColumns()
   {
      nameColumn_ = textColumn("Name", new ValueGetter<KeyboardShortcutEntry>()
      {
         @Override
         public String getValue(KeyboardShortcutEntry object)
         {
            return object.getName();
         }
      });
      
      shortcutColumn_ = editableTextColumn("Shortcut", new ValueGetter<KeyboardShortcutEntry>()
      {
         @Override
         public String getValue(KeyboardShortcutEntry object)
         {
            KeySequence sequence = object.getKeySequence();
            return sequence == null ? "" : sequence.toString();
         }
      });
      
      typeColumn_ = textColumn("Scope", new ValueGetter<KeyboardShortcutEntry>()
      {
         @Override
         public String getValue(KeyboardShortcutEntry object)
         {
            return object.getDisplayType();
         }
      });
      table_.setColumnWidth(typeColumn_, "160px");
      
   }
   
   private TextColumn<KeyboardShortcutEntry>
   textColumn(String name, final ValueGetter<KeyboardShortcutEntry> getter)
   {
      TextColumn<KeyboardShortcutEntry> column = new TextColumn<KeyboardShortcutEntry>()
      {
         @Override
         public String getValue(KeyboardShortcutEntry binding)
         {
            return getter.getValue(binding);
         }
      };
      
      column.setSortable(true);
      table_.addColumn(column, new TextHeader(name));
      return column;
   }
   
   private Column<KeyboardShortcutEntry, String>
   editableTextColumn(String name, final ValueGetter<KeyboardShortcutEntry> getter)
   {
      EditTextCell editTextCell = new EditTextCell()
      {
         @Override
         public void onBrowserEvent(final Context context,
                                    final Element parent,
                                    final String value,
                                    final NativeEvent event,
                                    final ValueUpdater<String> updater)
         {
            // GWT's EditTextCell will reset the text of the cell to the last
            // entered text on an Escape keypress. We don't desire that
            // behaviour (we want to restore the _first_ value presented when
            // the user opened the widget); so instead we just blur the input
            // element (thereby committing the current selection) and ensure
            // that selection has been appropriately reset in an earlier preview
            // handler.
            if (event.getType().equals("keyup") &&
                event.getKeyCode() == KeyCodes.KEY_ESCAPE)
            {
               parent.getFirstChildElement().blur();
               return;
            }
            
            super.onBrowserEvent(context, parent, value, event, updater);
         }
      };
      
      Column<KeyboardShortcutEntry, String> column =
            new Column<KeyboardShortcutEntry, String>(editTextCell)
      {
         @Override
         public String getValue(KeyboardShortcutEntry binding)
         {
            return getter.getValue(binding);
         }
      };
      
      column.setFieldUpdater(new FieldUpdater<KeyboardShortcutEntry, String>()
      {
         @Override
         public void update(int index, KeyboardShortcutEntry binding, String value)
         {
            KeySequence keys = KeySequence.fromShortcutString(value);
            
            // Differentiate between resetting the key sequence and
            // adding a new key sequence.
            if (keys.equals(binding.getOriginalKeySequence()))
            {
               changes_.remove(binding);
               binding.restoreOriginalKeySequence();
            }
            else
            {
               KeyboardShortcutEntry newBinding = new KeyboardShortcutEntry(
                     binding.getId(),
                     binding.getName(),
                     keys,
                     binding.getCommandType(),
                     true,
                     binding.getContext());
               
               changes_.put(binding, newBinding);
               binding.setKeySequence(keys);
            }

            table_.setKeyboardSelectedColumn(0);
            updateData(dataProvider_.getList());
         }
      });
      
      column.setSortable(true);
      table_.addColumn(column, new TextHeader(name));
      return column;
   }
   
   private void addHandlers()
   {
      table_.addCellPreviewHandler(new CellPreviewEvent.Handler<KeyboardShortcutEntry>()
      {
         @Override
         public void onCellPreview(CellPreviewEvent<KeyboardShortcutEntry> preview)
         {
            Handle shortcutsHandler = shortcuts_.disable();
            int column = preview.getColumn();
            if (column == 0)
               onNameCellPreview(preview);
            else if (column == 1)
               onShortcutCellPreview(preview);
            else if (column == 2)
               onNameCellPreview(preview);
            shortcutsHandler.close();
         }
      });
      
      table_.addColumnSortHandler(new ColumnSortEvent.Handler()
      {
         @Override
         public void onColumnSort(ColumnSortEvent event)
         {
            List<KeyboardShortcutEntry> data = dataProvider_.getList();
            if (event.getColumn().equals(nameColumn_))
               sort(data, 0, event.isSortAscending());
            else if (event.getColumn().equals(shortcutColumn_))
               sort(data, 1, event.isSortAscending());
            else if (event.getColumn().equals(typeColumn_))
               sort(data, 2, event.isSortAscending());
            
            updateData(data);
         }
      });
      
      // Fix a bug where clicking on a table header would also
      // select the cell at position [0, 0]. It seems that GWT's
      // DataGrid over-aggressively selects the first cell on the
      // _first_ mouse down event seen; after the first click,
      // cell selection occurs only after full mouse clicks.
      table_.addDomHandler(new MouseDownHandler()
      {
         @Override
         public void onMouseDown(MouseDownEvent event)
         {
            Element target = event.getNativeEvent().getEventTarget().cast();
            if (target.hasAttribute("__gwt_header"))
            {
               event.stopPropagation();
               event.preventDefault();
            }
         }
      }, MouseDownEvent.getType());
   }
   
   private void sort(List<KeyboardShortcutEntry> data,
                     final int column,
                     final boolean ascending)
   {
      Collections.sort(data, new Comparator<KeyboardShortcutEntry>()
      {
         @Override
         public int compare(KeyboardShortcutEntry o1, KeyboardShortcutEntry o2)
         {
            int result = 0;
            if (column == 0)
            {
               result = o1.getName().compareTo(o2.getName());
            }
            else if (column == 1)
            {
               KeySequence k1 = o1.getKeySequence();
               KeySequence k2 = o2.getKeySequence();
               if (k1 == null && k2 == null)
                  result = 0;
               else if (k1 == null)
                  result = 1;
               else if (k2 == null)
                  result = -1;
               else
                  result = k1.toString().compareTo(k2.toString());
            }
            else if (column == 2)
            {
               result = o1.getContext().toString().compareTo(o2.getContext().toString());
            }
               
            return ascending ? result : -result;
         }
      });
   }
   
   private void filter()
   {
      String query = filterWidget_.getValue();
      
      boolean isEmptyQuery = StringUtil.isNullOrEmpty(query);
      boolean customOnly = radioCustomized_.getValue();
      
      List<KeyboardShortcutEntry> filtered = new ArrayList<KeyboardShortcutEntry>();
      for (int i = 0; i < originalBindings_.size(); i++)
      {
         KeyboardShortcutEntry binding = originalBindings_.get(i);
         
         String name = binding.getName();
         String context = binding.getContext().toString();
         
         if (StringUtil.isNullOrEmpty(name))
            continue;
         
         if (customOnly && !(binding.isCustomBinding() || binding.isModified()))
            continue;
         
         boolean isGoodBinding =
               isEmptyQuery ||
               name.toLowerCase().indexOf(query.toLowerCase()) != -1 ||
               context.toLowerCase().indexOf(query.toLowerCase()) != -1;
         
         if (isGoodBinding)
            filtered.add(binding);
      }
      
      updateData(filtered);
      
   }
   
   private void onNameCellPreview(CellPreviewEvent<KeyboardShortcutEntry> preview)
   {
      NativeEvent event = preview.getNativeEvent();
      String type = event.getType();
      if (type.equals("blur"))
      {
         buffer_.clear();
      }
      else if (type.equals("keydown"))
      {
         int keyCode = event.getKeyCode();
         int modifiers = KeyboardShortcut.getModifierValue(event);
         if (keyCode == KeyCodes.KEY_ESCAPE && modifiers == 0)
         {
            event.stopPropagation();
            event.preventDefault();
            filterWidget_.focus();
         }
         else if (keyCode == KeyCodes.KEY_ENTER && modifiers == 0)
         {
            event.stopPropagation();
            event.preventDefault();
            table_.setKeyboardSelectedColumn(1);
         }
      }
   }
   
   private Element getElement(DataGrid<?> grid, int row, int column)
   {
      return grid.getRowElement(row).getChild(column).cast();
   }
   
   private Element shortcutInput()
   {
      Element el = DOM.createInputText();
      el.addClassName(RES.dataGridStyle().shortcutInput());
      return el;
   }
   
   private void onShortcutCellPreview(CellPreviewEvent<KeyboardShortcutEntry> preview)
   {
      NativeEvent event = preview.getNativeEvent();
      String type = event.getType();
      
      if (type.equals("keydown"))
      {
         int keyCode = event.getKeyCode();
         int modifiers = KeyboardShortcut.getModifierValue(event);
         
         // Don't handle raw 'Enter' keypresses (let underlying input
         // widget process)
         if (keyCode == KeyCodes.KEY_ENTER && modifiers == 0)
            return;
         
         // Handle any other key events.
         if (modifiers != 0)
            swallowNextKeyUpEvent_ = true;
         
         event.stopPropagation();
         event.preventDefault();
         
         if (KeyboardHelper.isModifierKey(event.getKeyCode()))
            return;
         
         if (keyCode == KeyCodes.KEY_BACKSPACE && modifiers == 0)
         {
            buffer_.pop();
         }
         else if (keyCode == KeyCodes.KEY_DELETE && modifiers == 0)
         {
            buffer_.clear();
         }
         else if (keyCode == KeyCodes.KEY_ESCAPE && modifiers == 0)
         {
            buffer_.set(preview.getValue().getOriginalKeySequence());
         }
         else
         {
            buffer_.add(event);
         }
         
         // Sneak into the element and find the active <input>, then update it.
         Element el = getElement(table_, preview.getIndex(), preview.getColumn());
         Element input = el.getFirstChildElement().getFirstChildElement();
         if (input == null)
            return;
         
         assert input.getTagName().toLowerCase().equals("input")
            : "Failed to find <input> element in table";

         String bufferString = buffer_.toString();
         input.setAttribute("value", bufferString);
         input.setInnerHTML(bufferString);
         
         // Move the cursor to the end of the selection.
         DomUtils.setSelectionRange(input, bufferString.length(), bufferString.length());
      }
   }
   
   private RadioButton radioButton(String label, ClickHandler handler)
   {
      RadioButton button = new RadioButton(RADIO_BUTTON_GROUP, label);
      button.getElement().getStyle().setMarginRight(6, Unit.PX);
      button.getElement().getStyle().setFloat(Style.Float.LEFT);
      button.getElement().getStyle().setMarginTop(-2, Unit.PX);
      button.addClickHandler(handler);
      return button;
   }
   
   private void resetState()
   {
      filterWidget_.clear();
      changes_.clear();
      radioAll_.setValue(true);
      collectShortcuts();
   }
   
   @Override
   protected Widget createMainWidget()
   {
      resetState();
      
      setEscapeDisabled(true);
      setEnterDisabled(true);
      
      previewHandler_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent preview)
         {
            if (swallowNextKeyUpEvent_ && preview.getTypeInt() == Event.ONKEYUP)
            {
               swallowNextKeyUpEvent_ = false;
               preview.cancel();
               preview.getNativeEvent().stopPropagation();
               preview.getNativeEvent().preventDefault();
            }
            else if (preview.getTypeInt() == Event.ONKEYDOWN)
            {
               int keyCode = preview.getNativeEvent().getKeyCode();
               if (keyCode == KeyCodes.KEY_ESCAPE || keyCode == KeyCodes.KEY_ENTER)
               {
                  // If the DataGrid (or an underlying element) has focus, let it
                  // handle the escape / enter key.
                  Element target = preview.getNativeEvent().getEventTarget().cast();
                  Element foundTable = DomUtils.findParentElement(target, new ElementPredicate()
                  {
                     @Override
                     public boolean test(Element el)
                     {
                        return el.equals(table_.getElement());
                     }
                  });

                  if (foundTable != null)
                     return;
                  
                  // If the filter widget has focus, Enter / Escape shouldn't close
                  // the widget.
                  if (filterWidget_.isFocused())
                  {
                     if (keyCode == KeyCodes.KEY_ENTER)
                     {
                        table_.setKeyboardSelectedRow(0);
                        table_.setKeyboardSelectedColumn(0);
                        return;
                     }
                     else if (keyCode == KeyCodes.KEY_ESCAPE)
                     {
                        focusOkButton();
                        return;
                     }
                  }

                  // Otherwise, handle Enter / Escape 'modally' as we might normally do.
                  preview.cancel();
                  preview.getNativeEvent().stopPropagation();
                  preview.getNativeEvent().preventDefault();
                  
                  if (keyCode == KeyCodes.KEY_ENTER)
                  {
                     clickOkButton();
                     return;
                  }
                  else if (keyCode == KeyCodes.KEY_ESCAPE)
                  {
                     closeDialog();
                     return;
                  }
               }
            }
         }
      });
      
      addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (event.isAttached())
               ;
            else
               previewHandler_.removeHandler();
         }
      });
      
      VerticalPanel container = new VerticalPanel();
      
      FlowPanel headerPanel = new FlowPanel();
      
      Label radioLabel = new Label("Show:");
      radioLabel.getElement().getStyle().setFloat(Style.Float.LEFT);
      radioLabel.getElement().getStyle().setMarginRight(8, Unit.PX);
      headerPanel.add(radioLabel);
      headerPanel.add(radioAll_);
      radioAll_.setValue(true);
      headerPanel.add(radioCustomized_);
      
      filterWidget_.getElement().getStyle().setFloat(Style.Float.LEFT);
      filterWidget_.getElement().getStyle().setMarginLeft(10, Unit.PX);
      filterWidget_.getElement().getStyle().setMarginTop(-1, Unit.PX);
      headerPanel.add(filterWidget_);
      
      HelpLink link = new HelpLink(
            "Customizing Keyboard Shortcuts",
            "custom_keyboard_shortcuts");
      link.getElement().getStyle().setFloat(Style.Float.RIGHT);
      headerPanel.add(link);
      
      container.add(headerPanel);
      
      FlowPanel spacer = new FlowPanel();
      spacer.setWidth("100%");
      spacer.setHeight("4px");
      container.add(spacer);
      
      DockPanel dockPanel = new DockPanel();
      dockPanel.add(table_, DockPanel.CENTER);
      container.add(dockPanel);
      
      return container;
   }
   
   private void collectShortcuts()
   {
      final List<KeyboardShortcutEntry> bindings = new ArrayList<KeyboardShortcutEntry>();
      SerializedCommandQueue queue = new SerializedCommandQueue();
      
      // Load addins discovered as part of package exports. This registers
      // the addin, with the actual keybinding to be registered later,
      // if discovered.
      queue.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(final Command continuation)
         {
            RAddins rAddins = addins_.getRAddins();
            for (String key : JsUtil.asIterable(rAddins.keys()))
            {
               RAddin addin = rAddins.get(key);
               
               bindings.add(new KeyboardShortcutEntry(
                     addin.getPackage() + "::" + addin.getBinding(),
                     addin.getName(),
                     new KeySequence(),
                     KeyboardShortcutEntry.TYPE_ADDIN,
                     false,
                     AppCommand.Context.Addin));
            }
            continuation.execute();
         }
      });
      
      // Load saved addin bindings
      queue.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(final Command continuation)
         {
            addins_.loadBindings(new CommandWithArg<EditorKeyBindings>()
            {
               @Override
               public void execute(EditorKeyBindings addinBindings)
               {
                  for (String commandId : addinBindings.iterableKeys())
                  {
                     EditorKeyBinding addinBinding = addinBindings.get(commandId);
                     for (KeyboardShortcutEntry binding : bindings)
                     {
                        if (binding.getId() == commandId)
                        {
                           List<KeySequence> keys = addinBinding.getKeyBindings();
                           if (keys.size() >= 1)
                              binding.setDefaultKeySequence(keys.get(0));
                           
                           if (keys.size() >= 2)
                           {
                              for (int i = 1; i < keys.size(); i++)
                              {
                                 bindings.add(new KeyboardShortcutEntry(
                                       binding.getId(),
                                       binding.getName(),
                                       keys.get(i),
                                       KeyboardShortcutEntry.TYPE_ADDIN,
                                       false,
                                       AppCommand.Context.Addin));
                              }
                           }
                        }
                     }
                  }
                  
                  continuation.execute();
               }
            });
         }
      });
      
      // Ace loading command
      queue.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(final Command continuation)
         {
            // Ace Commands
            JsArray<AceCommand> aceCommands = editorCommands_.getCommands();
            for (int i = 0; i < aceCommands.length(); i++)
            {
               AceCommand command = aceCommands.get(i);
               JsArrayString shortcuts = command.getBindingsForCurrentPlatform();
               
               if (shortcuts != null)
               {
                  String id = command.getInternalName();
                  String name = command.getDisplayName();
                  boolean custom = command.isCustomBinding();
               
                  for (int j = 0; j < shortcuts.length(); j++)
                  {
                     String shortcut = shortcuts.get(j);
                     KeySequence keys = KeySequence.fromShortcutString(shortcut);
                     int type = KeyboardShortcutEntry.TYPE_EDITOR_COMMAND;
                     bindings.add(new KeyboardShortcutEntry(id, name, keys, type, custom,
                           AppCommand.Context.Editor));
                  }
               }
            }
            
            continuation.execute();
         }
      });
      
      // RStudio commands
      queue.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(final Command continuation)
         {
            // RStudio Commands
            appCommands_.loadBindings(new CommandWithArg<EditorKeyBindings>()
            {
               @Override
               public void execute(final EditorKeyBindings customBindings)
               {
                  Map<String, AppCommand> commands = commands_.getCommands();
                  for (Map.Entry<String, AppCommand> entry : commands.entrySet())
                  {
                     AppCommand command = entry.getValue();
                     if (isExcludedCommand(command))
                        continue;
      
                     String id = command.getId();
                     String name = getAppCommandName(command);
                     int type = KeyboardShortcutEntry.TYPE_RSTUDIO_COMMAND;
                     boolean isCustom = customBindings.hasKey(id);
                     
                     List<KeySequence> keySequences = new ArrayList<KeySequence>();
                     if (isCustom)
                        keySequences = customBindings.get(id).getKeyBindings();
                     else
                        keySequences.add(command.getKeySequence());
                           
                     for (KeySequence keys : keySequences)
                     {
                        KeyboardShortcutEntry binding = new KeyboardShortcutEntry(
                              id, name, keys, type, isCustom, command.getContext());
                        bindings.add(binding);
                     }
                  }
                  
                  continuation.execute();
               }
            });
         }
      });
      
      // Sort and finish up
      queue.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(final Command continuation)
         {
            Collections.sort(bindings, new Comparator<KeyboardShortcutEntry>()
            {
               @Override
               public int compare(KeyboardShortcutEntry o1, KeyboardShortcutEntry o2)
               {
                  if (o1.getContext() != o2.getContext())
                     return o1.getContext().compareTo(o2.getContext());

                  return o1.getName().compareTo(o2.getName());
               }
            });

            originalBindings_ = bindings;
            updateData(bindings);
            continuation.execute();
         }
      });
      
      queue.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(Command continuation)
         {
            if (initialFilterText_ != null)
            {
               filterWidget_.setText(initialFilterText_);
               filter();
            }
            continuation.execute();
         }
      });
      
      // Exhaust the queue
      queue.run();
   }
   
   private void updateData(List<KeyboardShortcutEntry> bindings)
   {
      dataProvider_.setList(bindings);
      
      // Loop through and update styling on each row.
      for (int i = 0; i < bindings.size(); i++)
      {
         KeyboardShortcutEntry binding = bindings.get(i);
         if (binding.isCustomBinding() || binding.isModified())
         {
            TableRowElement rowEl = table_.getRowElement(i);
            DomUtils.toggleClass(rowEl, RES.dataGridStyle().customBindingRow(), binding.isCustomBinding());
            DomUtils.toggleClass(rowEl, RES.dataGridStyle().modifiedRow(), binding.isModified());
         }
      }
      
      // Identify conflicts / masking in the set of bindings and report
      // them. Note that this is an O(n^2) run through of commands but
      // given that the list shouldn't be excessively large it's probably
      // something we could live with.
      for (int i = 0; i < bindings.size(); i++)
      {
         KeyboardShortcutEntry cb1 = bindings.get(i);
         if (cb1.getKeySequence() == null || cb1.getKeySequence().isEmpty())
            continue;
         
         for (int j = 0; j < originalBindings_.size(); j++)
         {
            KeyboardShortcutEntry cb2 = originalBindings_.get(j);
            
            if (cb1.equals(cb2))
               continue;
            
            int t1 = cb1.getCommandType();
            int t2 = cb2.getCommandType();
            
            // allow for keybindings within the same keymap when they
            // map to different contexts. this is mainly done to support
            // 'dynamic' commands as handled with AppCommands
            if (t1 == t2 && cb1.getContext() != cb2.getContext())
               continue;
            
            KeySequence ks1 = cb1.getKeySequence();
            KeySequence ks2 = cb2.getKeySequence();
            
            if (ks1 == null || ks2 == null || ks1.isEmpty() || ks2.isEmpty())
               continue;
            
            boolean hasConflict =
                  ks1.equals(ks2) ||
                  ks1.startsWith(ks2, true) ||
                  ks2.startsWith(ks1, true);
            
            if (hasConflict)
            {
               // editor commands can be masked by AppCommands and addins
               if (t1 == KeyboardShortcutEntry.TYPE_EDITOR_COMMAND && t1 != t2)
                  addMaskedCommandStyles(i, j, cb2);
               
               // addins can mask both AppCommands and editor commands
               else if (t2 == KeyboardShortcutEntry.TYPE_ADDIN && t1 != t2)
                  addMaskedCommandStyles(i, j, cb2);
               
               // two commands with the same binding in the same 'group' == conflict
               else if (t1 == t2)
                  addConflictCommandStyles(i, j, cb2);
            }
         }
      }
   }
   
   private String describeCommand(KeyboardShortcutEntry command)
   {
      StringBuilder builder = new StringBuilder();
      builder.append("'").append(command.getName()).append("'");
      if (command.getKeySequence() != null)
         builder.append(" (").append(command.getKeySequence().toString()).append(")");
      return builder.toString();
   }
   
   private void addMaskedCommandStyles(int index, int maskedIndex, KeyboardShortcutEntry maskedBy)
   {
      Element shortcutCell =
            table_.getRowElement(index).getChild(1).cast();
      
      embedIcon(
            shortcutCell,
            new ImageResource2x(ThemeResources.INSTANCE.syntaxInfo2x()),
            "Masked by RStudio command: ",
            maskedIndex);
      
      shortcutCell.addClassName(RES.dataGridStyle().maskedEditorCommandCell());
   }
   
   private void addConflictCommandStyles(int index, int maskedIndex, KeyboardShortcutEntry conflictsWith)
   {
      Element shortcutCell =
            table_.getRowElement(index).getChild(1).cast();
      
      embedIcon(
            shortcutCell,
            new ImageResource2x(ThemeResources.INSTANCE.syntaxWarning2x()),
            "Conflicts with command: ",
            maskedIndex);
      
      shortcutCell.addClassName(RES.dataGridStyle().conflictRow());
   }
   
   private void embedIcon(Element el, ImageResource res, String toolTipText, int maskedIndex)
   {
      Image icon = new Image(res);
      icon.addStyleName(RES.dataGridStyle().icon());
      icon.setTitle(toolTipText);
      icon.getElement().setAttribute("__rstudio_masked_index", String.valueOf(maskedIndex));
      bindNativeClickToShowToolTip(icon.getElement(), toolTipText);
      el.appendChild(icon.getElement());
   }
   
   private native final void bindNativeClickToShowToolTip(Element icon, String text)
   /*-{
      var self = this;
      icon.addEventListener("click", $entry(function(evt) {
         
         // Prevent click from reaching shortcut cell
         evt.stopPropagation();
         evt.preventDefault();
         
         self.@org.rstudio.core.client.widget.ModifyKeyboardShortcutsWidget::showToolTip(Ljava/lang/Object;Ljava/lang/String;)(icon, text);
      }));
   }-*/;
   
   private native final void bindNativeClickToSelectRow(Element el, Element parent, int index) /*-{
      var self = this;
      el.addEventListener("click", $entry(function(evt) {
         
         evt.stopPropagation();
         evt.preventDefault();
         
         parent.parentNode.removeChild(parent);
         
         self.@org.rstudio.core.client.widget.ModifyKeyboardShortcutsWidget::selectRow(I)(index);
      }));
   }-*/;
   
   private void selectRow(int index)
   {
      table_.setKeyboardSelectedRow(index);
      table_.setKeyboardSelectedColumn(0);
   }
   
   private void showToolTip(Object object, String text)
   {
      assert object instanceof Element;
      Element el = (Element) object;
      
      int index = StringUtil.parseInt(el.getAttribute("__rstudio_masked_index"), -1);
      KeyboardShortcutEntry conflictBinding = originalBindings_.get(index);
      
      Element divEl = DOM.createDiv();
      Element spanEl = DOM.createSpan();
      spanEl.setInnerHTML(text);
      divEl.appendChild(spanEl);
      
      String conflictDescription = describeCommand(conflictBinding);
      
      // We use an anchor element here just to get browser default styling for
      // anchor links; we take over the click behaviour to ensure that the normal
      // 'href' navigation doesn't actually occur.
      Element conflictEl = DOM.createAnchor();
      conflictEl.setAttribute("href", "#");
      conflictEl.setInnerHTML(conflictDescription);
      divEl.appendChild(conflictEl);
      
      MiniPopupPanel tooltip = new MiniPopupPanel(true);
      
      bindNativeClickToSelectRow(
            conflictEl,
            tooltip.getElement(),
            index);
      
      tooltip.getElement().appendChild(divEl);
      tooltip.show();
      PopupPositioner.setPopupPosition(
            tooltip,
            el.getAbsoluteRight(),
            el.getAbsoluteBottom(),
            10);
   }
   
   private String getAppCommandName(AppCommand command)
   {
      String label = command.getLabel();
      if (!StringUtil.isNullOrEmpty(label))
         return label;
      
      return StringUtil.prettyCamel(command.getId());
   }
   
   private boolean isExcludedCommand(AppCommand command)
   {
      if (!command.isRebindable())
         return true;
      
      String id = command.getId();
      
      if (StringUtil.isNullOrEmpty(id))
         return true;
      
      return false;
   }
   
   private static final ProvidesKey<KeyboardShortcutEntry> KEY_PROVIDER =
         new ProvidesKey<KeyboardShortcutEntry>() {

            @Override
            public Object getKey(KeyboardShortcutEntry item)
            {
               return item.hashCode();
            }
   };
   
   private final ShortcutManager shortcuts_;
   private final KeySequence buffer_;
   private final DataGrid<KeyboardShortcutEntry> table_;
   private final ListDataProvider<KeyboardShortcutEntry> dataProvider_;
   private final Map<KeyboardShortcutEntry, KeyboardShortcutEntry> changes_;
   private final SearchWidget filterWidget_;
   private final String initialFilterText_;
   
   private final RadioButton radioAll_;
   private final RadioButton radioCustomized_;
   private static final String RADIO_BUTTON_GROUP =
         "radioCustomizeKeyboardShortcuts";
   
   private HandlerRegistration previewHandler_;
   private List<KeyboardShortcutEntry> originalBindings_;
   private Pair<Integer, Integer> lastSelectedIndices_;
   private boolean swallowNextKeyUpEvent_;
   
   // Columns ----
   private TextColumn<KeyboardShortcutEntry> nameColumn_;
   private Column<KeyboardShortcutEntry, String> shortcutColumn_;
   private TextColumn<KeyboardShortcutEntry> typeColumn_;
   
   // Injected ----
   private EditorCommandManager editorCommands_;
   private ApplicationCommandManager appCommands_;
   private AddinsCommandManager addins_;
   private AddinsServerOperations addinsServer_;
   private Commands commands_;
   private GlobalDisplay globalDisplay_;
   private EventBus events_;
   private AddinsMRUList mruAddins_;
   
   // Resources, etc ----
   public interface Resources extends RStudioDataGridResources
   {
      @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS, "ModifyKeyboardShortcutsWidget.css"})
      Styles dataGridStyle();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
      String customBindingRow();
      String modifiedRow();
      String maskedEditorCommandCell();
      String conflictRow();
      String shortcutInput();
      String icon();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.dataGridStyle().ensureInjected();
   }
   
}
