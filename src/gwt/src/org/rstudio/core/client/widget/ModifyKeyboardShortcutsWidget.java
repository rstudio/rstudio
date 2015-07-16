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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.ScrollingDataGrid;
import org.rstudio.core.client.command.AceCommandManager;
import org.rstudio.core.client.command.AceCommandManager.AceCommand;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.command.UserCommandManager;
import org.rstudio.core.client.command.UserCommandManager.UserCommand;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.ElementPredicate;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;

import java.util.ArrayList;
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
   }
   
   private void applyChanges()
   {
      for (Map.Entry<CommandBinding, CommandBinding> entry : changes_.entrySet())
      {
         CommandBinding oldBinding = entry.getKey();
         CommandBinding newBinding = entry.getValue();
         
         int commandType = newBinding.getCommandType();
         if (commandType == CommandBinding.TYPE_RSTUDIO_COMMAND)
         {
            AppCommand command = commands_.getCommandById(newBinding.getId());
            assert command != null :
               "Failed to discover AppCommand with id '" + newBinding.getId() + "'";
            
            Debug.logToRConsole("Updating command '" + newBinding.getId() + "' -> '" + newBinding.getKeySequence().toString() + "'");
            ShortcutManager.INSTANCE.replaceBinding(
                  newBinding.getKeySequence(),
                  command);
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
         else if (commandType == CommandBinding.TYPE_EDITOR_COMMAND)
         {
            aceCommands_.rebindCommand(
                  newBinding.getId(),
                  newBinding.getKeySequence());
         }
      }
      
      closeDialog();
   }
   
   @Inject
   public void initialize(UserCommandManager userCommands,
                          AceCommandManager aceCommands,
                          Commands commands)
   {
      userCommands_ = userCommands;
      aceCommands_ = aceCommands;
      commands_ = commands;
   }
   
   private void addColumns()
   {
      addTextColumn("Name", new TextColumn<CommandBinding>()
      {
         @Override
         public String getValue(CommandBinding object)
         {
            return object.getName();
         }
      });
      
      addTextColumn("Shortcut", new TextColumn<CommandBinding>()
      {
         @Override
         public String getValue(CommandBinding object)
         {
            KeySequence sequence = object.getKeySequence();
            return sequence == null ? "" : sequence.toString();
         }
      });
      
      addTextColumn("Type", new TextColumn<CommandBinding>()
      {
         @Override
         public String getValue(CommandBinding object)
         {
            return object.getDisplayType();
         }
      });
   }
   
   private void addTextColumn(String name, TextColumn<CommandBinding> column)
   {
      column.setSortable(true);
      table_.addColumn(column, name);
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
      JsArray<AceCommand> aceCommands = AceCommandManager.getDefaultAceCommands();
      Debug.logObject(aceCommands);
      
      for (int i = 0; i < aceCommands.length(); i++)
      {
         AceCommand command = aceCommands.get(i);
         String id = command.getInternalName();
         String name = command.getDisplayName();
         JsArrayString shortcuts = command.getBindingsForCurrentPlatform();
         Debug.logToRConsole("Shortcuts: " + shortcuts.toString());
         
         if (shortcuts != null)
         {
            for (int j = 0; j < shortcuts.length(); j++)
            {
               String shortcut = shortcuts.get(j);
               Debug.logToRConsole("Parsing Ace shortcut: '" + shortcut + "'");
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
      
      dataProvider_.setList(bindings);
   }
   
   private boolean isExcludedCommand(AppCommand command)
   {
      String id = command.getId();
      
      if (id == null)
         return false;
      
      if (id.startsWith("mru"))
         return true;
      if (command.getKeySequence() == null || command.getKeySequence().size() == 0)
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
   
   // Injected ----
   private UserCommandManager userCommands_;
   private AceCommandManager aceCommands_;
   private Commands commands_;
   
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
