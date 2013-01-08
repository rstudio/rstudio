/*
 * CheckForUpdatesDialog.java
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
package org.rstudio.studio.client.workbench.views.packages.ui;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.ImageButtonColumn;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.views.packages.model.PackageUpdate;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ScrollPanel;
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
      super("Update Packages", checkOperation);
      globalDisplay_ = globalDisplay;
      updatesDS_ = updatesDS;
      
      setOkButtonCaption("Install Updates");
    
      addLeftButton(selectAllButton_ = new ThemedButton("Select All", 
                                                        new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
           setGlobalApplyUpdate(true);       
         } 
      }));
     
      addLeftButton(selectNoneButton_ = new ThemedButton("Select None", 
                                                         new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            setGlobalApplyUpdate(false);   
         } 
      }));  
      
      enableOkButton(false);
      enableCancelButton(false);
      selectAllButton_.setEnabled(false);
      selectNoneButton_.setEnabled(false);
   }

   @Override
   protected ArrayList<PackageUpdate> collectInput()
   {
      ArrayList<PackageUpdate> updates = new ArrayList<PackageUpdate>();
      for (PendingUpdate update : updatesDataProvider_.getList())
      {
         if (update.getApplyUpdate())
            updates.add(update.getUpdateInfo());
      }
      return updates;
   }

   @Override
   protected boolean validate(ArrayList<PackageUpdate> input)
   {
      return input.size() > 0;
   }

   @Override
   protected Widget createMainWidget()
   {
      updatesTable_ = new CellTable<PendingUpdate>(
            15,
            GWT.<PackagesCellTableResources> create(PackagesCellTableResources.class));
      updatesTable_.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      updatesTable_.setSelectionModel(new NoSelectionModel<PendingUpdate>());
      updatesTable_.setWidth("100%", true);
      
      UpdateColumn updateColumn = new UpdateColumn();
      updatesTable_.addColumn(updateColumn);
      updatesTable_.setColumnWidth(updateColumn, 30, Unit.PX);
      
      
      TextColumn<PendingUpdate> nameColumn = new TextColumn<PendingUpdate>() {
         public String getValue(PendingUpdate update)
         {
            return update.getUpdateInfo().getPackageName();
         } 
      };  
      updatesTable_.addColumn(nameColumn, "Package");
      updatesTable_.setColumnWidth(nameColumn, 28, Unit.PCT);
      
      TextColumn<PendingUpdate> installedColumn = new TextColumn<PendingUpdate>() {
         public String getValue(PendingUpdate update)
         {
            return update.getUpdateInfo().getInstalled();
         } 
      };  
      updatesTable_.addColumn(installedColumn, "Installed");
      updatesTable_.setColumnWidth(installedColumn, 28, Unit.PCT);
      
      TextColumn<PendingUpdate> availableColumn = new TextColumn<PendingUpdate>() {
         public String getValue(PendingUpdate update)
         {
            return update.getUpdateInfo().getAvailable();
         } 
      };  
      updatesTable_.addColumn(availableColumn, "Available");
      updatesTable_.setColumnWidth(availableColumn, 28, Unit.PCT);
      
      ImageButtonColumn<PendingUpdate> newsColumn = 
         new ImageButtonColumn<PendingUpdate>(
           AbstractImagePrototype.create(ThemeResources.INSTANCE.newsButton()),
           new OperationWithInput<PendingUpdate>() {
             public void execute(PendingUpdate update)
             {
                GlobalDisplay.NewWindowOptions options = 
                                  new GlobalDisplay.NewWindowOptions();
                options.setName("_rstudio_package_news");
                options.setAlwaysUseBrowser(true);
                globalDisplay_.openWindow(update.getUpdateInfo().getNewsUrl(),
                                          options);
             }  
           },
           "Show package NEWS") 
           {
               @Override
               protected boolean showButton(PendingUpdate update)
               {
                  return !StringUtil.isNullOrEmpty(
                                          update.getUpdateInfo().getNewsUrl());
               }
           };
      updatesTable_.addColumn(newsColumn, "NEWS");
      updatesTable_.setColumnWidth(newsColumn, 16, Unit.PCT);
      
      ScrollPanel scrollPanel = new ScrollPanel();
      scrollPanel.setStylePrimaryName(RESOURCES.styles().mainWidget());
      scrollPanel.setWidget(updatesTable_);
      
      // query for updates
      updatesDS_.requestData(new SimpleRequestCallback<JsArray<PackageUpdate>>() {

         @Override
         public void onResponseReceived(JsArray<PackageUpdate> packageUpdates)
         {
            if (packageUpdates.length() > 0)
            {
               ArrayList<PendingUpdate> updates = new ArrayList<PendingUpdate>();
               for (int i=0; i<packageUpdates.length(); i++)
                  updates.add(new PendingUpdate(packageUpdates.get(i), false));
               updatesTable_.setPageSize(updates.size());
               updatesDataProvider_ = new ListDataProvider<PendingUpdate>();
               updatesDataProvider_.setList(updates);
               updatesDataProvider_.addDataDisplay(updatesTable_);
               
               enableCancelButton(true);
               selectAllButton_.setEnabled(true);
               selectNoneButton_.setEnabled(true);
            }
            else
            {
               closeDialog();
               globalDisplay_.showMessage(
                     MessageDialog.INFO, 
                     "Check for Updates", 
                     "All packages are up to date.");
            }
         }
         
         @Override
         public void onError(ServerError error)
         {
            closeDialog();
            super.onError(error);            
         }  
      });
 
      return scrollPanel;
   }
   
   class UpdateColumn extends Column<PendingUpdate, Boolean>
   {
      public UpdateColumn()
      {
         super(new CheckboxCell(false, false));
         
         setFieldUpdater(new FieldUpdater<PendingUpdate,Boolean>() {
            public void update(int index, PendingUpdate update, Boolean value)
            {
               List<PendingUpdate> updates = updatesDataProvider_.getList();
               updates.set(updates.indexOf(update), 
                           new PendingUpdate(update.getUpdateInfo(), value));
               manageUIState();
            }    
         });
      }

      @Override
      public Boolean getValue(PendingUpdate update)
      {
         return update.getApplyUpdate();
      }
   }
   
   private class PendingUpdate
   {
      public PendingUpdate(PackageUpdate updateInfo, boolean applyUpdate)
      {
         updateInfo_ = updateInfo;
         applyUpdate_ = applyUpdate;
      }
      
      public PackageUpdate getUpdateInfo()
      {
         return updateInfo_;
      }
      
      public boolean getApplyUpdate()
      {
         return applyUpdate_;
      }
      
      private final PackageUpdate updateInfo_;
      private final boolean applyUpdate_;
   }
   
   private void setGlobalApplyUpdate(Boolean applyUpdate)
   {
      List<PendingUpdate> updates = updatesDataProvider_.getList();
      ArrayList<PendingUpdate> newUpdates = new ArrayList<PendingUpdate>();
      for(PendingUpdate update : updates)
         newUpdates.add(new PendingUpdate(update.getUpdateInfo(), applyUpdate));
      updatesDataProvider_.setList(newUpdates);
      manageUIState();
   }
   
   private void manageUIState()
   {
      enableOkButton(collectInput().size() > 0);
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
   private CellTable<PendingUpdate> updatesTable_;
   private ServerDataSource<JsArray<PackageUpdate>> updatesDS_;
   private ListDataProvider<PendingUpdate> updatesDataProvider_;
   private ThemedButton selectAllButton_;
   private ThemedButton selectNoneButton_;

}
