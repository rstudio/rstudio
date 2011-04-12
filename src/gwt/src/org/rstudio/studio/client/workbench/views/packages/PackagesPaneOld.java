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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.HTMLTable.ColumnFormatter;
import com.google.inject.Inject;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.packages.Packages.Display;
import org.rstudio.studio.client.workbench.views.packages.model.InstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.packages.ui.InstallPackageDialog;
import org.rstudio.studio.client.workbench.views.packages.ui.PackageNameWidget;

import java.util.List;

public class PackagesPaneOld extends WorkbenchPane implements Packages.Display
{
   @Inject
   public PackagesPaneOld(Commands commands)
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
      packageListPanel_.setWidget(null);
      setProgress(true);

      packages_ = new FlexTable();
      packages_.setCellSpacing(0);
      packages_.setCellPadding(3);
      packages_.setWidth("100%");
      ColumnFormatter colFormat = packages_.getColumnFormatter() ;
      colFormat.setWidth(0, "20px");
      packages_.getColumnFormatter().setWidth(1, "25%");
      packages_.getColumnFormatter().setWidth(2, "75%");

      for (int i=0; i<packages.size(); i++)
      {
         final PackageInfo packageInfo = packages.get(i);

         CheckBox checkBox = new CheckBox();
         checkBox.setValue(Boolean.valueOf(packageInfo.isLoaded()));
         checkBox.addValueChangeHandler(
               new ValueChangeHandler<Boolean>() {

                  public void onValueChange(ValueChangeEvent<Boolean> event)
                  {
                     if (event.getValue().booleanValue())
                        observer_.loadPackage(packageInfo.getName()) ;
                     else
                        observer_.unloadPackage(packageInfo.getName()) ;
                  }
               });

         packages_.setWidget(i, 0, checkBox);

         PackageNameWidget name = new PackageNameWidget(packageInfo.getName()) ;
         name.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event)
            {
               event.preventDefault();
               event.stopPropagation();
               observer_.showHelp(packageInfo) ;
            }
         }) ;
         packages_.setWidget(i, 1, name) ;
         Label desc = new Label(packageInfo.getDesc()) ;
         desc.setWordWrap(true) ;
         packages_.setWidget(i, 2, desc) ;

         packages_.getRowFormatter().setVerticalAlign(i,
                                                      HasVerticalAlignment.ALIGN_TOP) ;
         if ((i % 2) != 0)
            packages_.getRowFormatter().addStyleName(i,
                                                     ThemeStyles.INSTANCE.odd());
      }

      packageListPanel_.setWidget(packages_);
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
      int row = findPackage(packageName) ;
      
      if (row != -1)
      {
         CheckBox checkBox = (CheckBox)packages_.getWidget(row, 0);
         PackageNameWidget name = (PackageNameWidget) packages_.getWidget(row, 1);

         switch (status)
         {
         case Display.PACKAGE_NOT_LOADED:
            checkBox.setValue(false, false);
            name.showProgress(false);
            break;
         case Display.PACKAGE_LOADED:
            checkBox.setValue(true, false);
            name.showProgress(false);
            break;
         case Display.PACKAGE_PROGRESS:
            name.showProgress(true);
            break;
         }

         
      }
   }

   private int findPackage(String packageName)
   {
      // if we haven't retreived packages yet then return not found
      if (packages_ == null)
         return -1;
      
      // figure out which row of the table includes this package
      int row = -1;
      for (int i=0; i<packages_.getRowCount(); i++)
      {
         PackageNameWidget name = (PackageNameWidget) packages_.getWidget(i, 1);
         if (name.getName().equals(packageName))
         {
            row = i ;
            break;
         }
      }
      return row ;
   }
   
   public void clearPackageProgress(String packageName)
   {
      int row = findPackage(packageName) ;
      if (row < 0)
         return ;
      
      CheckBox checkBox = (CheckBox)packages_.getWidget(row, 0);
      boolean loaded = checkBox.getValue().booleanValue() ;
      setPackageStatus(packageName, loaded ? PACKAGE_LOADED 
                                           : PACKAGE_NOT_LOADED) ;
   }

   @Override
   protected Widget createMainWidget()
   {
      packageListPanel_ = new ScrollPanel();
      return packageListPanel_;
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
     
      toolbar.addLeftWidget(commands_.installPackage().createToolbarButton());
      toolbar.addRightWidget(commands_.refreshPackages().createToolbarButton());
      return toolbar;
   }

   private ScrollPanel packageListPanel_;
   private FlexTable packages_;
   private PackagesDisplayObserver observer_ ;
   private final Commands commands_;
}
