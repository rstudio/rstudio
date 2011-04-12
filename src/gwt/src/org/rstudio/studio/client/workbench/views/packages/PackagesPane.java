/*
 * PackagesPane.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages;


import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.packages.model.InstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.packages.ui.InstallPackageDialog;

import java.util.List;

public class PackagesPane extends WorkbenchPane implements Packages.Display
{
   @Inject
   public PackagesPane(Commands commands)
   {
      super("Packages");
      commands_ = commands;
     
      ensureWidget();
   }
   
   public void setObserver(PackagesDisplayObserver observer)
   {
      observer_ = observer ;  
   }

   public void listPackages(List<PackageInfo> packages)
   {
      // prepare to retreive a new package list
      setProgress(true);

     

      setProgress(false);
   }
   
   public void installPackage(String installRepository,
                              PackagesServerOperations server,
                              GlobalDisplay globalDisplay,
                              OperationWithInput<InstallOptions> operation)
   {
      new InstallPackageDialog(installRepository,
                               server, 
                               globalDisplay, 
                               operation).showModal();
   }
      

   public void setPackageStatus(String packageName, int status)
   {
      
   }

  
   
   public void clearPackageProgress(String packageName)
   {
     
      
    
   }

   @Override
   protected Widget createMainWidget()
   {
      packagesTable_ = new CellTable<PackageInfo>();
      
     
      Column<PackageInfo, Boolean> loadedColumn = 
         new Column<PackageInfo, Boolean>(new CheckboxCell(false, false)) {
           @Override
           public Boolean getValue(PackageInfo packageInfo) {
              return packageInfo.isLoaded();
           }
         };
         
      packagesTable_.addColumn(loadedColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
      packagesTable_.setColumnWidth(loadedColumn, 40, Unit.PX);
      
      TextColumn<PackageInfo> nameColumn = new TextColumn<PackageInfo>(){
         @Override
         public String getValue(PackageInfo packageInfo)
         {
            return packageInfo.getName();
         }
         
      };
      packagesTable_.addColumn(nameColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
      packagesTable_.setColumnWidth(nameColumn, 100, Unit.PCT);
      
      return packagesTable_;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
     
      toolbar.addLeftWidget(commands_.installPackage().createToolbarButton());
      toolbar.addRightWidget(commands_.refreshPackages().createToolbarButton());
      return toolbar;
   }

   private CellTable<PackageInfo> packagesTable_;
   private PackagesDisplayObserver observer_ ;
   private final Commands commands_;
}
