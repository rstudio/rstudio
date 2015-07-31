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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
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
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Command;
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
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.ApplicationCommandManager;
import org.rstudio.core.client.command.EditorCommandManager;
import org.rstudio.core.client.command.EditorCommandManager.EditorKeyBindings;
import org.rstudio.core.client.command.KeyboardHelper;
import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
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
   public static class CommandBinding
   {
      public CommandBinding(String id,
                            String displayName,
                            KeySequence keySequence,
                            int commandType,
                            boolean isCustom)
      {
         id_ = id;
         name_ = displayName;
         keySequence_ = keySequence;
         commandType_ = commandType;
         isCustom_ = isCustom;
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
         if (commandType_ == TYPE_RSTUDIO_COMMAND)
            return "RStudio Command";
         else if (commandType_ == TYPE_EDITOR_COMMAND)
            return "Editor Command";
         
         return "<Unknown Command>";
      }
      
      public boolean isCustomBinding()
      {
         return isCustom_;
      }
      
      public void setKeySequence(KeySequence keys)
      {
         newKeySequence_ = keys.clone();
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
         if (object == null || !(object instanceof CommandBinding))
            return false;
         
         CommandBinding other = (CommandBinding) object;
         return
               commandType_ == other.commandType_ &&
               id_.equals(other.id_);
      }
      
      private final String id_;
      private final String name_;
      private final KeySequence keySequence_;
      private final int commandType_;
      private final boolean isCustom_;
      
      private KeySequence newKeySequence_;
      
      public static final int TYPE_RSTUDIO_COMMAND =  1; // RStudio AppCommands
      public static final int TYPE_EDITOR_COMMAND =   2; // e.g. Ace commands
   }
   
   public ModifyKeyboardShortcutsWidget()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      changes_ = new HashMap<CommandBinding, CommandBinding>();
      buffer_ = new KeySequence();
      
      table_ = new DataGrid<CommandBinding>(1000, RES, KEY_PROVIDER);
      
      FlowPanel emptyWidget = new FlowPanel();
      Label emptyLabel = new Label("No bindings available");
      emptyLabel.getElement().getStyle().setMarginTop(20, Unit.PX);
      emptyLabel.getElement().getStyle().setColor("#888");
      emptyWidget.add(emptyLabel);
      table_.setEmptyTableWidget(emptyWidget);
      
      table_.setWidth("800px");
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
                                    indicator.onCompleted();
                                    resetState();
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
      EditorKeyBindings editorBindings = EditorKeyBindings.create();
      EditorKeyBindings appBindings = EditorKeyBindings.create();
      
      
      // Loop through all changes and apply based on type
      for (Map.Entry<CommandBinding, CommandBinding> entry : changes_.entrySet())
      {
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
      }
      
      appCommands_.addBindingsAndSave(appBindings);
      editorCommands_.addBindingsAndSave(editorBindings);
      
      closeDialog();
   }
   
   @Inject
   public void initialize(EditorCommandManager editorCommands,
                          ApplicationCommandManager appCommands,
                          Commands commands,
                          GlobalDisplay globalDisplay)
   {
      editorCommands_ = editorCommands;
      appCommands_ = appCommands;
      commands_ = commands;
      globalDisplay_ = globalDisplay;
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
   
   private void filter()
   {
      String query = filterWidget_.getValue();
      boolean customOnly = radioCustomized_.getValue();
      
      List<CommandBinding> filtered = new ArrayList<CommandBinding>();
      for (int i = 0; i < originalBindings_.size(); i++)
      {
         CommandBinding binding = originalBindings_.get(i);
         String name = binding.getName();
         
         if (StringUtil.isNullOrEmpty(name))
            continue;
         
         if (customOnly && !binding.isCustomBinding())
            continue;
         
         if (name.toLowerCase().indexOf(query.toLowerCase()) != -1)
            filtered.add(binding);
      }
      
      updateData(filtered);
      
   }
   
   private void onShortcutCellPreview(CellPreviewEvent<CommandBinding> preview)
   {
      NativeEvent event = preview.getNativeEvent();
      String type = event.getType();
      
      if (type.equals("blur"))
      {
         buffer_.clear();
      }
      else if (event.getType().equals("keydown"))
      {
         if (KeyboardHelper.isModifierKey(event.getKeyCode()))
            return;
         
         CommandBinding binding = preview.getValue();
         
         if (event.getKeyCode() == KeyCodes.KEY_BACKSPACE)
         {
            event.stopPropagation();
            event.preventDefault();
            buffer_.pop();
            binding.setKeySequence(buffer_);
         }
         else if (event.getKeyCode() == KeyCodes.KEY_DELETE)
         {
            event.stopPropagation();
            event.preventDefault();
            buffer_.clear();
            binding.restoreOriginalKeySequence();
         }
         else
         {
            buffer_.add(event);
            binding.setKeySequence(buffer_);

            // Add the new command binding to later be accepted + registered
            CommandBinding newBinding = new CommandBinding(
                  binding.getId(),
                  binding.getName(),
                  buffer_.clone(),
                  binding.getCommandType(),
                  true);

            changes_.put(binding, newBinding);
         }
         
         ensureCellSelectionUnchanged();
         updateData(dataProvider_.getList());
         
      }
   }
   
   private void ensureCellSelectionUnchanged()
   {
      final int row = table_.getKeyboardSelectedRow();
      final int column = table_.getKeyboardSelectedColumn();
      
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            table_.setKeyboardSelectedRow(row, true);
            table_.setKeyboardSelectedColumn(column);
         }
      });
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
      final List<CommandBinding> bindings = new ArrayList<CommandBinding>();
      
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
               int type = CommandBinding.TYPE_EDITOR_COMMAND;
               bindings.add(new CommandBinding(id, name, keys, type, custom));
            }
         }
      }
      
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
               KeySequence keySequence = command.getKeySequence();
               int type = CommandBinding.TYPE_RSTUDIO_COMMAND;
               boolean isCustom = customBindings.hasKey(id);
               bindings.add(new CommandBinding(id, name, keySequence, type, isCustom));
            }

            originalBindings_ = bindings;
            updateData(bindings);
         }
      });
   }
   
   private void updateData(List<CommandBinding> bindings)
   {
      dataProvider_.setList(bindings);
      
      // Loop through and update styling on each row.
      for (int i = 0; i < bindings.size(); i++)
      {
         CommandBinding binding = bindings.get(i);
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
         CommandBinding cb1 = bindings.get(i);
         if (cb1.getKeySequence() == null || cb1.getKeySequence().isEmpty())
            continue;
         
         for (int j = 0; j < originalBindings_.size(); j++)
         {
            CommandBinding cb2 = originalBindings_.get(j);
            
            if (cb1.equals(cb2))
               continue;
            
            int t1 = cb1.getCommandType();
            int t2 = cb2.getCommandType();
            
            KeySequence ks1 = cb1.getKeySequence();
            KeySequence ks2 = cb2.getKeySequence();
            
            if (ks1 == null || ks2 == null)
               continue;
            
            boolean hasConflict =
                  ks1.equals(ks2) ||
                  ks1.startsWith(ks2) ||
                  ks2.startsWith(ks1);
            
            if (hasConflict)
            {
               if (t1 == CommandBinding.TYPE_EDITOR_COMMAND && t1 != t2)
                  addMaskedCommandStyles(i, cb2);
               else if (t1 == t2)
                  addConflictCommandStyles(i, cb2);
            }
         }
      }
   }
   
   private String describeCommand(CommandBinding command)
   {
      StringBuilder builder = new StringBuilder();
      builder.append("'").append(command.getName()).append("'");
      if (command.getKeySequence() != null)
         builder.append(" (").append(command.getKeySequence().toString()).append(")");
      return builder.toString();
   }
   
   private void addMaskedCommandStyles(int index, CommandBinding maskedBy)
   {
      Element shortcutCell =
            table_.getRowElement(index).getChild(1).cast();
      
      embedIcon(
            shortcutCell,
            ThemeResources.INSTANCE.syntaxInfo(),
            "Masked by RStudio command: " + describeCommand(maskedBy));
      
      shortcutCell.addClassName(RES.dataGridStyle().maskedEditorCommandCell());
   }
   
   private void addConflictCommandStyles(int index, CommandBinding conflictsWith)
   {
      Element shortcutCell =
            table_.getRowElement(index).getChild(1).cast();
      
      embedIcon(
            shortcutCell,
            ThemeResources.INSTANCE.syntaxWarning(),
            "Conflicts with command: " + describeCommand(conflictsWith));
      
      shortcutCell.addClassName(RES.dataGridStyle().conflictRow());
   }
   
   private void embedIcon(Element el, ImageResource res, String toolTipText)
   {
      Image icon = new Image(res);
      icon.addStyleName(RES.dataGridStyle().icon());
      icon.setTitle(toolTipText);
      bindNativeClickToShowToolTip(icon.getElement(), toolTipText);
      el.appendChild(icon.getElement());
   }
   
   private static native final void bindNativeClickToShowToolTip(Element icon, String text)
   /*-{
      icon.addEventListener("click", $entry(function(evt) {
         
         // Prevent click from reaching shortcut cell
         evt.stopPropagation();
         evt.preventDefault();
         
         @org.rstudio.core.client.widget.ModifyKeyboardShortcutsWidget::showToolTip(Ljava/lang/Object;Ljava/lang/String;)(icon, text);
      }));
   }-*/;
   
   private static void showToolTip(Object object, String text)
   {
      assert object instanceof Element;
      Element el = (Element) object;
      
      MiniPopupPanel tooltip = new MiniPopupPanel(true);
      tooltip.add(new Label(text));
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
      if (!command.isRebindable() || !command.isVisible())
         return true;
      
      String id = command.getId();
      
      if (StringUtil.isNullOrEmpty(id))
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
   private final DataGrid<CommandBinding> table_;
   private final ListDataProvider<CommandBinding> dataProvider_;
   private final Map<CommandBinding, CommandBinding> changes_;
   private final SearchWidget filterWidget_;
   
   private final RadioButton radioAll_;
   private final RadioButton radioCustomized_;
   private static final String RADIO_BUTTON_GROUP =
         "radioCustomizeKeyboardShortcuts";
   
   private List<CommandBinding> originalBindings_;
   
   // Columns ----
   private TextColumn<CommandBinding> nameColumn_;
   private TextColumn<CommandBinding> shortcutColumn_;
   private TextColumn<CommandBinding> typeColumn_;
   
   // Injected ----
   private EditorCommandManager editorCommands_;
   private ApplicationCommandManager appCommands_;
   private Commands commands_;
   private GlobalDisplay globalDisplay_;
   
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
      String icon();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.dataGridStyle().ensureInjected();
   }
   
}
