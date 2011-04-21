package org.rstudio.studio.client.workbench.views.packages.ui;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.ImageButtonColumn;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.views.packages.model.PackageUpdate;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;

public class CheckForUpdatesDialog extends ModalDialog<ArrayList<PackageUpdate>>
{

   public CheckForUpdatesDialog(
         GlobalDisplay globalDisplay,
         ServerDataSource<JsArray<PackageUpdate>> updatesDS,
         OperationWithInput<ArrayList<PackageUpdate>> checkOperation)
   {
      super("Check for Package Updates", checkOperation);
      globalDisplay_ = globalDisplay;
      updatesDS_ = updatesDS;
      
      setOkButtonCaption("Install Updates");
      enableOkButton(false);
   }

   @Override
   protected ArrayList<PackageUpdate> collectInput()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   protected boolean validate(ArrayList<PackageUpdate> input)
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel mainPanel = new VerticalPanel();
      mainPanel.setStylePrimaryName(RESOURCES.styles().mainWidget());
 
      updatesTable_ = new CellTable<PackageUpdate>(
            15,
            GWT.<PackagesCellTableResources> create(PackagesCellTableResources.class));
      updatesTable_.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      updatesTable_.setSelectionModel(new NoSelectionModel<PackageUpdate>());
      updatesTable_.setWidth("100%", false);
      
      updatesTable_.addColumn(new UpdateColumn(), "Update?");
      
      TextColumn<PackageUpdate> nameColumn = new TextColumn<PackageUpdate>() {
         public String getValue(PackageUpdate packageUpdate)
         {
            return packageUpdate.getPackageName();
         } 
      };  
      updatesTable_.addColumn(nameColumn, "Package");
      
      TextColumn<PackageUpdate> installedColumn = new TextColumn<PackageUpdate>() {
         public String getValue(PackageUpdate packageUpdate)
         {
            return packageUpdate.getInstalled();
         } 
      };  
      updatesTable_.addColumn(installedColumn, "Installed");
      
      TextColumn<PackageUpdate> availableColumn = new TextColumn<PackageUpdate>() {
         public String getValue(PackageUpdate packageUpdate)
         {
            return packageUpdate.getAvailable();
         } 
      };  
      updatesTable_.addColumn(availableColumn, "Available");
      
      ImageButtonColumn<PackageUpdate> newsColumn = 
         new ImageButtonColumn<PackageUpdate>(
           AbstractImagePrototype.create(ThemeResources.INSTANCE.zoomDataset()),
           new OperationWithInput<PackageUpdate>() {
             public void execute(PackageUpdate packageUpdate)
             {
                globalDisplay_.openMinimalWindow(packageUpdate.getNewsUrl(),
                                                 false, 
                                                 700, 
                                                 800, 
                                                 "_rstudio_package_news", 
                                                 true);
             }  
           }) 
           {
               @Override
               protected boolean showButton(PackageUpdate packageUpdate)
               {
                  return !StringUtil.isNullOrEmpty(packageUpdate.getNewsUrl());
               }
           };
      updatesTable_.addColumn(newsColumn, "NEWS");
  
      ScrollPanel scrollPanel = new ScrollPanel();
      scrollPanel.setWidget(updatesTable_);
      mainPanel.add(scrollPanel);
      
      // query for updates
      updatesDS_.requestData(new SimpleRequestCallback<JsArray<PackageUpdate>>() {

         @Override
         public void onResponseReceived(JsArray<PackageUpdate> packageUpdates)
         {
            ArrayList<PackageUpdate> updates = new ArrayList<PackageUpdate>();
            for (int i=0; i<packageUpdates.length(); i++)
               updates.add(packageUpdates.get(i));
            updatesTable_.setPageSize(updates.size());
            updatesDataProvider_ = new ListDataProvider<PackageUpdate>();
            updatesDataProvider_.setList(updates);
            updatesDataProvider_.addDataDisplay(updatesTable_);
         }
         
         @Override
         public void onError(ServerError error)
         {
            closeDialog();
            super.onError(error);            
         }  
      });
      
      
      
      
      return mainPanel;
   }
   
   class UpdateColumn extends Column<PackageUpdate, Boolean>
   {
      public UpdateColumn()
      {
         super(new CheckboxCell(false, false));
         
         setFieldUpdater(new FieldUpdater<PackageUpdate,Boolean>() {
            public void update(int index, PackageUpdate packageInfo, Boolean value)
            {
               
               
            }    
         });
      }

      @Override
      public Boolean getValue(PackageUpdate update)
      {
         return null;
      }
   }
   

   static interface Styles extends CssResource
   {
      String mainWidget();
   }

   static interface Resources extends ClientBundle
   {
      @Source("CheckForUpdatesDialog.css")
      Styles styles();
   }

   static Resources RESOURCES = (Resources) GWT.create(Resources.class);

   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private final GlobalDisplay globalDisplay_;
   private CellTable<PackageUpdate> updatesTable_;
   private ServerDataSource<JsArray<PackageUpdate>> updatesDS_;
   private ListDataProvider<PackageUpdate> updatesDataProvider_;

}
