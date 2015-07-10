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

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;

import org.rstudio.core.client.CustomKeyboardShortcutDispatcher;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.CustomKeyboardShortcutDispatcher.UserCommand;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.cellview.ScrollingDataGrid;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModifyKeyboardShortcutsWidget extends ModalDialogBase
{
   public static class CommandBinding
   {
      public CommandBinding(String name, String shortcut, String type)
      {
         name_ = name;
         shortcut_ = shortcut;
         type_ = type;
      }
      
      public String getName()
      {
         return prettyCamel(name_);
      }
      
      public String getShortcut()
      {
         return shortcut_;
      }
      
      public String getType()
      {
         return type_;
      }
      
      public String getDisplayType()
      {
         if (type_.equals(TYPE_USER_COMMAND))
            return "User-Defined Command";
         else if (type_.equals(TYPE_INTERNAL_COMMAND))
            return "RStudio Command";
         else if (type_.equals(TYPE_EDITOR_COMMAND))
            return "Editor Command";
         return "<Unknown Command>";
      }
      
      private final String name_;
      private final String shortcut_;
      private final String type_;
      
      public static final String TYPE_USER_COMMAND = "user_command";
      public static final String TYPE_INTERNAL_COMMAND = "internal_command";
      public static final String TYPE_EDITOR_COMMAND = "editor_command"; // Ace
   }
   
   public ModifyKeyboardShortcutsWidget()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      table_ = new ScrollingDataGrid<CommandBinding>(1000, KEY_PROVIDER);
      table_.setWidth("500px");
      table_.setHeight("400px");
      
      dataProvider_ = new ListDataProvider<CommandBinding>();
      dataProvider_.addDataDisplay(table_);
      
      addColumns();
      
      setText("Keyboard Shortcuts");
      addCancelButton().setText("Close");
   }
   
   @Inject
   public void initialize(Commands commands,
                          CustomKeyboardShortcutDispatcher shortcuts)
   {
      commands_ = commands;
      shortcuts_ = shortcuts;
   }
   
   private void addColumns()
   {
      addCheckColumn();
      
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
            return object.getShortcut();
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
   
   private void addCheckColumn()
   {
      CheckboxCell cell = new CheckboxCell(true, false);
      Column<CommandBinding, Boolean> checkColumn =
            new Column<CommandBinding, Boolean>(cell) {
         
         @Override
         public Boolean getValue(CommandBinding binding)
         {
            return true;
         }
      };
      
      checkColumn.setCellStyleNames(RES.styles().checkColumn());
      table_.addColumn(checkColumn);
      table_.setColumnWidth(checkColumn, "24px");
   }
   
   private void addTextColumn(String name, TextColumn<CommandBinding> column)
   {
      column.setSortable(true);
      table_.addColumn(column, name);
   }
   
   @Override
   protected Widget createMainWidget()
   {
      collectShortcuts();
      DockPanel dockPanel = new DockPanel();
      dockPanel.add(table_, DockPanel.CENTER);
      return dockPanel;
   }
   
   private void collectShortcuts()
   {
      List<CommandBinding> bindings = new ArrayList<CommandBinding>();
      
      Map<String, UserCommand> userCommands = shortcuts_.getCommandMap();
      for (Map.Entry<String, UserCommand> entry : userCommands.entrySet())
      {
         String name = entry.getValue().getName();
         String shortcut = entry.getKey();
         String type = CommandBinding.TYPE_USER_COMMAND;
         bindings.add(new CommandBinding(name, shortcut, type));
      }
      
      Map<String, AppCommand> commands = commands_.getCommands();
      for (Map.Entry<String, AppCommand> entry : commands.entrySet())
      {
         AppCommand command = entry.getValue();
         
         String name = command.getId();
         String shortcut = command.getShortcutRaw();
         String type = CommandBinding.TYPE_INTERNAL_COMMAND;
         bindings.add(new CommandBinding(name, shortcut, type));
      }
      
      Debug.logToRConsole("Found " + bindings.size() + " bindings");
      
      dataProvider_.setList(bindings);
   }
   
   private static final ProvidesKey<CommandBinding> KEY_PROVIDER =
         new ProvidesKey<CommandBinding>() {

            @Override
            public Object getKey(CommandBinding item)
            {
               return item.getName();
            }
   };
   
   private static String prettyCamel(String string)
   {
      if (StringUtil.isNullOrEmpty(string))
         return string;
      
      String result = string.replaceAll("\\s*([A-Z])", " $1");
      return result.substring(0, 1).toUpperCase() +
             result.substring(1);
   }
   
   private final ScrollingDataGrid<CommandBinding> table_;
   private final ListDataProvider<CommandBinding> dataProvider_;
   
   // Injected ----
   private Commands commands_;
   private CustomKeyboardShortcutDispatcher shortcuts_;
   
   // Resources, etc ----
   public interface Resources extends ClientBundle
   {
      @Source("ModifyKeyboardShortcutsWidget.css")
      Styles styles();
   }
   
   public interface Styles extends CssResource
   {
      String checkColumn();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
   
}
