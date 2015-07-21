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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.ScrollingDataGrid;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.ApplicationCommandManager;
import org.rstudio.core.client.command.EditorCommandManager;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.command.UserCommandManager;
import org.rstudio.core.client.command.UserCommandManager.UserCommand;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceCommandManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModifyKeyboardShortcutsWidget extends ModalDialogBase
{
   public static class CommandBinding
   {
      public CommandBinding(String id,
                            String displayName,
                            KeySequence keySequence,
                            int commandType)
      {
         id_ = id;
         name_ = displayName;
         keySequence_ = keySequence;
         commandType_ = commandType;
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
         return keySequence_;
      }
      
      public int getCommandType()
      {
         return commandType_;
      }
      
      public String getDisplayType()
      {
         if (commandType_ == TYPE_USER_COMMAND)
            return "User-Defined Command";
         else if (commandType_ == TYPE_RSTUDIO_COMMAND)
            return "RStudio Command";
         else if (commandType_ == TYPE_EDITOR_COMMAND)
            return "Editor Command";
         
         return "<Unknown Command>";
      }
      
      private final String id_;
      private final String name_;
      private final KeySequence keySequence_;
      private final int commandType_;
      
      public static final int TYPE_USER_COMMAND =     0; // execute user R code
      public static final int TYPE_RSTUDIO_COMMAND =  1; // RStudio AppCommands
      public static final int TYPE_EDITOR_COMMAND =   2; // e.g. Ace commands
   }
   
   public ModifyKeyboardShortcutsWidget()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      changes_ = new HashMap<CommandBinding, CommandBinding>();
      buffer_ = new KeySequence();
      
      table_ = new ScrollingDataGrid<CommandBinding>(1000, KEY_PROVIDER);
      table_.setWidth("500px");
      table_.setHeight("400px");
      
      dataProvider_ = new ListDataProvider<CommandBinding>();
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
      
      searchWidget_ = new SearchWidget(new SuggestOracle() {

         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<Suggestion>()));
         }
         
      });
      
      searchWidget_.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            filter(event.getValue());
         }
      });
      
      searchWidget_.getElement().getStyle().setMarginTop(3, Unit.PX);
      searchWidget_.getElement().getStyle().setMarginLeft(3, Unit.PX);
      searchWidget_.setPlaceholderText("Filter...");
      
      addLeftWidget(searchWidget_);
   }
   
   private void applyChanges()
   {
      // Build up command diffs for save after application
      EditorKeyBindings editorBindings = EditorKeyBindings.create();
      EditorKeyBindings appBindings = EditorKeyBindings.create();
      
      
      // Loop through all changes and apply based on type
      for (Map.Entry<CommandBinding, CommandBinding> entry : changes_.entrySet())
      {
         CommandBinding oldBinding = entry.getKey();
         CommandBinding newBinding = entry.getValue();
         
         int commandType = newBinding.getCommandType();
         if (commandType == CommandBinding.TYPE_RSTUDIO_COMMAND)
         {
            appBindings.setBinding(
                  newBinding.getId(),
                  newBinding.getKeySequence());
         }
         else if (commandType == CommandBinding.TYPE_EDITOR_COMMAND)
         {
            editorBindings.setBinding(
                  newBinding.getId(),
                  newBinding.getKeySequence());
         }
         else if (commandType == CommandBinding.TYPE_USER_COMMAND)
         {
            Map<KeyboardShortcut, UserCommand> userCommands = userCommands_.getCommands();
            UserCommand command = userCommands.get(
                  new KeyboardShortcut(oldBinding.getKeySequence()));
            assert command != null :
               "Failed to find user command bound to '" + oldBinding.getKeySequence().toString() + "'";
            
            KeyboardShortcut oldShortcut = new KeyboardShortcut(oldBinding.getKeySequence());
            userCommands.remove(oldShortcut);
            
            KeyboardShortcut newShortcut = new KeyboardShortcut(newBinding.getKeySequence());
            userCommands.put(newShortcut, command);
         }
      }
      
      appCommands_.addBindingsAndSave(appBindings);
      editorCommands_.addBindingsAndSave(editorBindings);
      
      closeDialog();
   }
   
   @Inject
   public void initialize(UserCommandManager userCommands,
                          EditorCommandManager editorCommands,
                          ApplicationCommandManager appCommands,
                          Commands commands,
                          FilesServerOperations files)
   {
      userCommands_ = userCommands;
      editorCommands_ = editorCommands;
      appCommands_ = appCommands;
      commands_ = commands;
      files_ = files;
   }
   
   private void addColumns()
   {
      nameColumn_ = textColumn("Name", new TextColumn<CommandBinding>()
      {
         @Override
         public String getValue(CommandBinding object)
         {
            return object.getName();
         }
      });
      
      shortcutColumn_ = textColumn("Shortcut", new TextColumn<CommandBinding>()
      {
         @Override
         public String getValue(CommandBinding object)
         {
            KeySequence sequence = object.getKeySequence();
            return sequence == null ? "" : sequence.toString();
         }
      });
      
      typeColumn_ = textColumn("Type", new TextColumn<CommandBinding>()
      {
         @Override
         public String getValue(CommandBinding object)
         {
            return object.getDisplayType();
         }
      });
   }
   
   private TextColumn<CommandBinding> textColumn(String name, TextColumn<CommandBinding> column)
   {
      column.setSortable(true);
      table_.addColumn(column, new TextHeader(name));
      return column;
   }
   
   private void addHandlers()
   {
      table_.addCellPreviewHandler(new CellPreviewEvent.Handler<CommandBinding>()
      {
         @Override
         public void onCellPreview(CellPreviewEvent<CommandBinding> preview)
         {
            int column = preview.getColumn();
            if (column == 0)
               ;
            else if (column == 1)
               onShortcutCellPreview(preview);
            else if (column == 2)
               ;
         }
      });
      
      table_.addColumnSortHandler(new ColumnSortEvent.Handler()
      {
         @Override
         public void onColumnSort(ColumnSortEvent event)
         {
            List<CommandBinding> data = dataProvider_.getList();
            if (event.getColumn().equals(nameColumn_))
               sort(data, 0, event.isSortAscending());
            else if (event.getColumn().equals(shortcutColumn_))
               sort(data, 1, event.isSortAscending());
            else if (event.getColumn().equals(typeColumn_))
               sort(data, 2, event.isSortAscending());
            
            dataProvider_.setList(data);
         }
      });
   }
   
   private void sort(List<CommandBinding> data,
                     final int column,
                     final boolean ascending)
   {
      Collections.sort(data, new Comparator<CommandBinding>()
      {
         @Override
         public int compare(CommandBinding o1, CommandBinding o2)
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
               result = o1.getCommandType() > o2.getCommandType() ? 1 : -1;
            }
               
            return ascending ? result : -result;
         }
      });
   }
   
   private void filter(String query)
   {
      List<CommandBinding> filtered = new ArrayList<CommandBinding>();
      for (int i = 0; i < originalBindings_.size(); i++)
      {
         CommandBinding binding = originalBindings_.get(i);
         String name = binding.getName();
         if (StringUtil.isNullOrEmpty(name))
            continue;
         
         if (name.toLowerCase().indexOf(query.toLowerCase()) != -1)
            filtered.add(binding);
      }
      dataProvider_.setList(filtered);
   }
   
   private void onShortcutCellPreview(CellPreviewEvent<CommandBinding> preview)
   {
      NativeEvent event = preview.getNativeEvent();
      String type = event.getType();
      if (type.equals("focus") || type.equals("blur"))
      {
         buffer_.clear();
      }
      else if (event.getType().equals("keydown"))
      {
         if (KeyboardHelper.isModifierKey(event.getKeyCode()))
            return;
         
         buffer_.add(event);
         Element target = event.getEventTarget().cast();
         target.setInnerHTML(buffer_.toString());
         
         // Provide a visual cue that this row has changed
         Element rowEl = DomUtils.findParentElement(
               target,
               new ElementPredicate()
               {
                  @Override
                  public boolean test(Element el)
                  {
                     return el.getTagName().toLowerCase().equals("tr");
                  }
               });
         
         if (rowEl != null)
         {
            rowEl.addClassName(RES.styles().modifiedRow());
         }
         
         // Add the new command binding to later be accepted + registered
         CommandBinding oldBinding = preview.getValue();
         CommandBinding newBinding = new CommandBinding(
               oldBinding.getId(),
               oldBinding.getName(),
               buffer_.clone(),
               oldBinding.getCommandType());
               
         changes_.put(oldBinding, newBinding);
         
         // TODO: Highlight any existing conflicts.
         
      }
   }
   
   @Override
   protected Widget createMainWidget()
   {
      changes_.clear();
      collectShortcuts();
      DockPanel dockPanel = new DockPanel();
      dockPanel.add(table_, DockPanel.CENTER);
      return dockPanel;
   }
   
   private void collectShortcuts()
   {
      List<CommandBinding> bindings = new ArrayList<CommandBinding>();
      
      // User Commands
      Map<KeyboardShortcut, UserCommand> userCommands = userCommands_.getCommands();
      for (Map.Entry<KeyboardShortcut, UserCommand> entry : userCommands.entrySet())
      {
         KeyboardShortcut shortcut = entry.getKey();
         UserCommand command = entry.getValue();
         
         bindings.add(new CommandBinding(
               command.getName(),
               StringUtil.prettyCamel(command.getName()),
               shortcut.getKeySequence(),
               CommandBinding.TYPE_USER_COMMAND));
      }
      
      // Ace Commands
      JsArray<AceCommand> aceCommands = AceCommandManager.getRelevantCommands();
      for (int i = 0; i < aceCommands.length(); i++)
      {
         AceCommand command = aceCommands.get(i);
         String id = command.getInternalName();
         String name = command.getDisplayName();
         JsArrayString shortcuts = command.getBindingsForCurrentPlatform();
         
         if (shortcuts != null)
         {
            for (int j = 0; j < shortcuts.length(); j++)
            {
               String shortcut = shortcuts.get(j);
               KeySequence keys = KeySequence.fromShortcutString(shortcut);
               int type = CommandBinding.TYPE_EDITOR_COMMAND;
               bindings.add(new CommandBinding(id, name, keys, type));
            }
         }
      }
      
      // RStudio Commands
      Map<String, AppCommand> commands = commands_.getCommands();
      for (Map.Entry<String, AppCommand> entry : commands.entrySet())
      {
         AppCommand command = entry.getValue();
         if (isExcludedCommand(command))
            continue;
         
         String id = command.getId();
         String name = StringUtil.prettyCamel(command.getId());
         KeySequence keySequence = command.getKeySequence();
         int type = CommandBinding.TYPE_RSTUDIO_COMMAND;
         bindings.add(new CommandBinding(id, name, keySequence, type));
      }
      
      originalBindings_ = bindings;
      dataProvider_.setList(bindings);
   }
   
   private boolean isExcludedCommand(AppCommand command)
   {
      String id = command.getId();
      
      if (id == null)
         return false;
      
      if (id.startsWith("mru"))
         return true;
      
      return false;
   }
   
   private static final ProvidesKey<CommandBinding> KEY_PROVIDER =
         new ProvidesKey<CommandBinding>() {

            @Override
            public Object getKey(CommandBinding item)
            {
               return item.getName();
            }
   };
   
   private final KeySequence buffer_;
   private final ScrollingDataGrid<CommandBinding> table_;
   private final ListDataProvider<CommandBinding> dataProvider_;
   private final Map<CommandBinding, CommandBinding> changes_;
   private final SearchWidget searchWidget_;
   
   private List<CommandBinding> originalBindings_;
   
   // Columns ----
   private TextColumn<CommandBinding> nameColumn_;
   private TextColumn<CommandBinding> shortcutColumn_;
   private TextColumn<CommandBinding> typeColumn_;
   
   // Injected ----
   private UserCommandManager userCommands_;
   private EditorCommandManager editorCommands_;
   private ApplicationCommandManager appCommands_;
   private Commands commands_;
   private FilesServerOperations files_;
   
   // Resources, etc ----
   public interface Resources extends ClientBundle
   {
      @Source("ModifyKeyboardShortcutsWidget.css")
      Styles styles();
   }
   
   public interface Styles extends CssResource
   {
      String modifiedRow();
      String checkColumn();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
   
}
