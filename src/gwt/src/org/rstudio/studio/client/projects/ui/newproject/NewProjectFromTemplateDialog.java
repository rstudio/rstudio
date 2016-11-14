package org.rstudio.studio.client.projects.ui.newproject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.projects.model.NewProjectContext;
import org.rstudio.studio.client.projects.model.ProjectTemplateDescription;
import org.rstudio.studio.client.projects.model.ProjectTemplateRegistry;
import org.rstudio.studio.client.projects.model.ProjectTemplateServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;

public class NewProjectFromTemplateDialog extends ModalDialog<NewProjectFromTemplateDialog.Result>
{
   public static class Result
   {
      public Result(ProjectTemplateDescription description)
      {
         description_ = description;
      }
      
      public ProjectTemplateDescription getProjectTemplateDescription()
      {
         return description_;
      }
      
      private final ProjectTemplateDescription description_;
   }
   
   public interface ValueGetter
   {
      String getValue(ProjectTemplateDescription description);
   }
   
   public NewProjectFromTemplateDialog(final NewProjectContext context,
                                       final OperationWithInput<Result> operation)
   {
      super("New Project from Template", operation);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      setOkButtonCaption("Create Project");
      
      context_ = context;
      columns_ = new HashMap<String, TextColumn<ProjectTemplateDescription>>();
      
      table_ = new DataGrid<ProjectTemplateDescription>(
            1000,
            RES,
            new ProvidesKey<ProjectTemplateDescription>()
            {
               @Override
               public Object getKey(ProjectTemplateDescription description)
               {
                  return description.hashCode();
               }
            });
      
      table_.setEmptyTableWidget(new Label("Loading templates..."));
      
      table_.setWidth("500px");
      table_.setHeight("200px");
      
      dataProvider_ = new ListDataProvider<ProjectTemplateDescription>();
      dataProvider_.addDataDisplay(table_);
      
      selectionModel_ = new SingleSelectionModel<ProjectTemplateDescription>();
      selectionModel_.addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            selection_ = selectionModel_.getSelectedObject();
         }
      });
      table_.setSelectionModel(selectionModel_);
      
      addColumn("Package", "100px", new ValueGetter()
      {
         @Override
         public String getValue(ProjectTemplateDescription description)
         {
            return description.getPackage();
         }
      });
      
      addColumn("Binding", "200", new ValueGetter()
      {
         @Override
         public String getValue(ProjectTemplateDescription description)
         {
            return description.getBinding();
         }
      });
      
      addColumn("Description", null, new ValueGetter()
      {
         @Override
         public String getValue(ProjectTemplateDescription description)
         {
            return description.getDescription();
         }
      });
      
      container_ = new VerticalPanel();
      
      Grid grid = new Grid(1, 2);
      grid.setWidth("100%");
      
      VerticalPanel lhsPanel = new VerticalPanel();
      lhsPanel.add(new Label("Directory name:"));
      lhsPanel.add(new TextBox());
      grid.setWidget(0, 0, lhsPanel);
      
      grid.setWidget(0, 1, new DirectoryChooserTextBox("Create project in directory:", null));
      
      container_.add(grid);
      
      container_.add(table_);
      
      server_.getProjectTemplateRegistry(new ServerRequestCallback<ProjectTemplateRegistry>()
      {
         @Override
         public void onResponseReceived(ProjectTemplateRegistry registry)
         {
            if (registry.isEmpty())
            {
               table_.setEmptyTableWidget(new Label("No project templates available."));
               return;
            }
            
            final List<ProjectTemplateDescription> templateList =
                  new ArrayList<ProjectTemplateDescription>();
            
            JsArrayString keys = registry.keys();
            for (String key : JsUtil.asIterable(keys))
               templateList.addAll(JsArrayUtil.toArrayList(registry.get(key)));
            
            dataProvider_.setList(templateList);
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   @Inject
   private void initialize(ProjectTemplateServerOperations server)
   {
      server_ = server;
   }
   
   @Override
   protected Result collectInput()
   {
      return new Result(selection_);
   }

   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }
   
   private void addColumn(String columnName,
                          String columnWidth,
                          final ValueGetter getter)
   {
      TextColumn<ProjectTemplateDescription> column = new TextColumn<ProjectTemplateDescription>()
      {
         @Override
         public String getValue(ProjectTemplateDescription description)
         {
            String value = getter.getValue(description);
            return StringUtil.truncate(value, 120, "...");
         }
      };
      table_.addColumn(column, new TextHeader(columnName));
      
      if (columnWidth != null)
         table_.setColumnWidth(column, columnWidth);
      
      columns_.put(columnName, column);
   }
 
   private final NewProjectContext context_;
   
   private final VerticalPanel container_;
   private final DataGrid<ProjectTemplateDescription> table_;
   private final Map<String, TextColumn<ProjectTemplateDescription>> columns_;
   private final ListDataProvider<ProjectTemplateDescription> dataProvider_;
   private final SingleSelectionModel<ProjectTemplateDescription> selectionModel_;
   
   private ProjectTemplateDescription selection_;
   
   // Injected ----
   private ProjectTemplateServerOperations server_;
   
   // Resources, etc ----
   public interface Resources extends RStudioDataGridResources
   {
      @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS})
      Styles dataGridStyle();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.dataGridStyle().ensureInjected();
   }
   
}
