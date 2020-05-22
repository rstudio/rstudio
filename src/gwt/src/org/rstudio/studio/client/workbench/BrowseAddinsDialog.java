/*
 * BrowseAddinsDialog.java
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
package org.rstudio.studio.client.workbench;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;

import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.ListUtil.FilterPredicate;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.widget.FilterWidget;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.ModifyKeyboardShortcutsWidget;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.RStudioDataGrid;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.addins.Addins.AddinExecutor;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.AddinsCommandManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BrowseAddinsDialog extends ModalDialog<Command>
{
   public BrowseAddinsDialog(OperationWithInput<Command> operation)
   {
      super("Addins", Roles.getDialogRole(), operation);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      setOkButtonCaption("Execute");
      
      filterWidget_ = new FilterWidget()
      {
         @Override
         public void filter(String query)
         {
            BrowseAddinsDialog.this.filter(query);
         }
      };
      filterWidget_.getElement().getStyle().setFloat(Style.Float.LEFT);
      
      helpLink_ = new HelpLink("Using RStudio Addins", "rstudio_addins", false);
      helpLink_.getElement().getStyle().setFloat(Style.Float.RIGHT);
      
      addAttachHandler((AttachEvent event) -> {
         if (event.isAttached())
            Scheduler.get().scheduleDeferred(() -> filterWidget_.focus());
      });
      
      keyProvider_ = new ProvidesKey<RAddin>()
      {
         @Override
         public Object getKey(RAddin addin)
         {
            return addin.hashCode();
         }
      };
      
      table_ = new RStudioDataGrid<RAddin>(1000, RES, keyProvider_);
      table_.setWidth("500px");
      table_.setHeight("400px");
      
      selectionModel_ = new SingleSelectionModel<RAddin>();
      selectionModel_.addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            selection_ = selectionModel_.getSelectedObject();
         }
      });
      
      table_.setSelectionModel(selectionModel_);
      table_.setEmptyTableWidget(emptyTableLabel("Loading addins..."));
      
      addColumns();
      
      dataProvider_ = new ListDataProvider<>();
      dataProvider_.addDataDisplay(table_);
      
      originalData_ = new ArrayList<>();
      
      // sync to current addins
      addins_ = addinsCommandManager_.getRAddins();
      List<RAddin> data = new ArrayList<>();
      for (String key : JsUtil.asIterable(addins_.keys()))
         data.add(addins_.get(key));
      dataProvider_.setList(data);
      originalData_ = data;
      table_.setEmptyTableWidget(emptyTableLabel("No addins available"));
      
      addLeftWidget(new ThemedButton("Keyboard Shortcuts...", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ModifyKeyboardShortcutsWidget widget = new ModifyKeyboardShortcutsWidget("addin");
            widget.showModal();
         }
      }));
      
      FlowPanel headerPanel = new FlowPanel();
      FormLabel filterLabel = new FormLabel(true, "Filter addins:", filterWidget_.getInputElement());
      filterLabel.addStyleName(RES.dataGridStyle().filterLabel());
      headerPanel.add(filterLabel);
      headerPanel.add(filterWidget_);
      headerPanel.add(helpLink_);
      
      container_ = new VerticalPanel();
      container_.add(headerPanel);
      container_.add(new VerticalSeparator("4px"));
      container_.add(table_);
   }
   
   private static class VerticalSeparator extends Composite
   {
      public VerticalSeparator(String size)
      {
         FlowPanel panel = new FlowPanel();
         panel.setHeight(size);
         initWidget(panel);
      }
   }
   
   @Inject
   private void initialize(AddinsCommandManager addinsCommandManager, AriaLiveService ariaLive)
   {
      addinsCommandManager_ = addinsCommandManager;
      ariaLive_ = ariaLive;
   }
   
   private void addColumns()
   {
      // Package ----
      pkgColumn_ = new TextColumn<RAddin>()
      {
         @Override
         public String getValue(RAddin addin)
         {
            return addin.getPackage();
         }
      };
      pkgColumn_.setSortable(true);
      table_.addColumn(pkgColumn_, new TextHeader("Package"));
      table_.setColumnWidth(pkgColumn_, "120px");
            
      // Name ----
      nameColumn_ = new TextColumn<RAddin>()
      {
         @Override
         public String getValue(RAddin addin)
         {
            return StringUtil.truncate(addin.getName(), 120, "...");
         }
      };
      nameColumn_.setSortable(true);
      table_.addColumn(nameColumn_, new TextHeader("Name"));
      table_.setColumnWidth(nameColumn_, "120px");
      
      // Description ----
      descColumn_ = new TextColumn<RAddin>()
      {
         @Override
         public String getValue(RAddin addin)
         {
            return addin.getDescription();
         }
      };
      descColumn_.setSortable(true);
      table_.addColumn(descColumn_, new TextHeader("Description"));
     
      table_.addColumnSortHandler(new ColumnSortEvent.Handler()
      {
         @Override
         public void onColumnSort(ColumnSortEvent event)
         {
            int index = -1;
            if (event.getColumn().equals(pkgColumn_))
               index = 0;
            else if (event.getColumn().equals(nameColumn_))
               index = 1;
            else if (event.getColumn().equals(descColumn_))
               index = 2;
            
            if (index == -1)
               return;
            
            sort(index, event.isSortAscending());
         }
      });
   }
   
   private void sort(final int index, final boolean forward)
   {
      Collections.sort(dataProvider_.getList(), new Comparator<RAddin>()
      {
         @Override
         public int compare(RAddin o1, RAddin o2)
         {
            String f1 = "";
            String f2 = "";
            
            if (index == 0)
            {
               f1 = o1.getPackage();
               f2 = o2.getPackage();
            }
            else if (index == 1)
            {
               f1 = o1.getName();
               f2 = o2.getName();
            }
            else if (index == 2)
            {
               f1 = o1.getDescription();
               f2 = o2.getDescription();
            }
            
            return forward
                  ? f1.compareTo(f2)
                  : f2.compareTo(f1);
         }
         
      });
   }
   
   private void filter(String query)
   {
      final String[] splat = query.toLowerCase().split("\\s+");
      List<RAddin> data = ListUtil.filter(originalData_, new FilterPredicate<RAddin>()
      {
         @Override
         public boolean test(RAddin object)
         {
            for (String el : splat)
            {
               boolean match =
                     object.getName().toLowerCase().contains(el) ||
                     object.getPackage().toLowerCase().contains(el);
               
               if (!match)
                  return false;
            }
            return true;
         }
      });
      dataProvider_.setList(data);
      ariaLive_.announce(AriaLiveService.FILTERED_LIST,
            "Found " + data.size() + " addins matching " + StringUtil.spacedString(query),
            Timing.DEBOUNCE, Severity.STATUS);
   }
   
   @Override
   protected Command collectInput()
   {
      // when we have no selection, assume the user intended to execute
      // the first command in the list
      if (selection_ == null)
      {
         List<RAddin> data = dataProvider_.getList();
         if (!data.isEmpty())
            selection_ = data.get(0);
      }
      
      // if we still don't have a selection, just return an empty command
      if (selection_ == null)
         return () -> {};
      
      return new ExecuteAddinCommand(
            addins_.get(selection_.getId()),
            new AddinExecutor());
   }
   
   private static class ExecuteAddinCommand implements Command
   {
      public ExecuteAddinCommand(RAddin addin, AddinExecutor executor)
      {
         addin_ = addin;
         executor_ = executor;
      }
      
      @Override
      public void execute()
      {
         executor_.execute(addin_);
      }
      
      private final RAddin addin_;
      private final AddinExecutor executor_;
   }

   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }
   
   private Label emptyTableLabel(String caption)
   {
      Label label = new Label(caption);
      label.getElement().getStyle().setMarginTop(20, Unit.PX);
      label.getElement().getStyle().setColor("#656565");
      return label;
   }
   
   // Private members ----
   private final VerticalPanel container_;
   private final FilterWidget filterWidget_;
   private final HelpLink helpLink_;
   
   private final DataGrid<RAddin> table_;
   private TextColumn<RAddin> pkgColumn_;
   private TextColumn<RAddin> nameColumn_;
   private TextColumn<RAddin> descColumn_;
   
   private final ProvidesKey<RAddin> keyProvider_;
   private final ListDataProvider<RAddin> dataProvider_;
   private final SingleSelectionModel<RAddin> selectionModel_;
   
   private List<RAddin> originalData_;
   private RAddins addins_;
   private RAddin selection_;
   
   // Injected ----
   private AddinsCommandManager addinsCommandManager_;
   private AriaLiveService ariaLive_;

   // Resources, etc ----
   public interface Resources extends RStudioDataGridResources
   {
      @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS, "BrowseAddinsDialog.css"})
      Styles dataGridStyle();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
      String filterLabel();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.dataGridStyle().ensureInjected();
   }
   
}
