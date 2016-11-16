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
import org.rstudio.studio.client.RStudioGinjector;
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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;

public class NewProjectFromTemplateWidget
      extends DataGrid<ProjectTemplateDescription>
{
   public interface ValueGetter
   {
      String getValue(ProjectTemplateDescription description);
   }
   
   public NewProjectFromTemplateWidget()
   {
      super(1000, RES, new ProvidesKey<ProjectTemplateDescription>()
      {
         @Override
         public Object getKey(ProjectTemplateDescription description)
         {
            return description.hashCode();
         }
      });
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      columns_ = new HashMap<String, TextColumn<ProjectTemplateDescription>>();
      setEmptyTableWidget(new Label("Loading templates..."));
      
      setWidth("500px");
      setHeight("200px");
      
      dataProvider_ = new ListDataProvider<ProjectTemplateDescription>();
      dataProvider_.addDataDisplay(this);
      
      selectionModel_ = new SingleSelectionModel<ProjectTemplateDescription>();
      selectionModel_.addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            selection_ = selectionModel_.getSelectedObject();
         }
      });
      setSelectionModel(selectionModel_);
      
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
      
      server_.getProjectTemplateRegistry(new ServerRequestCallback<ProjectTemplateRegistry>()
      {
         @Override
         public void onResponseReceived(ProjectTemplateRegistry registry)
         {
            if (registry.isEmpty())
            {
               setEmptyTableWidget(new Label("No project templates available."));
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
      addColumn(column, new TextHeader(columnName));
      
      if (columnWidth != null)
         setColumnWidth(column, columnWidth);
      
      columns_.put(columnName, column);
   }
 
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
