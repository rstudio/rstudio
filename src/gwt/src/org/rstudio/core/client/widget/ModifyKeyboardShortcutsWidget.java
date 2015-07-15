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
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.command.ShortcutManager;
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
                            KeySequence keySequence,
                            int commandType)
      {
         id_ = id;
         name_ = prettyCamel(id);
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
      }
      
      closeDialog();
   }
   
   @Inject
   public void initialize(Commands commands)
   {
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
               buffer_.clone(),
               oldBinding.getCommandType());
               
         changes_.put(oldBinding, newBinding);
         
         // Highlight any existing conflicts.
         
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
      
      Map<String, AppCommand> commands = commands_.getCommands();
      for (Map.Entry<String, AppCommand> entry : commands.entrySet())
      {
         AppCommand command = entry.getValue();
         
         String name = command.getId();
         KeySequence keySequence = command.getKeySequence();
         int type = CommandBinding.TYPE_RSTUDIO_COMMAND;
         bindings.add(new CommandBinding(name, keySequence, type));
      }
      
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
   
   private final KeySequence buffer_;
   private final ScrollingDataGrid<CommandBinding> table_;
   private final ListDataProvider<CommandBinding> dataProvider_;
   private final Map<CommandBinding, CommandBinding> changes_;
   
   // Injected ----
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
