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

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.inject.Inject;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.help.search.SearchWidget;
import org.rstudio.studio.client.workbench.views.packages.model.InstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.packages.ui.InstallPackageDialog;

import java.util.ArrayList;
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
      packagesTable_.setPageSize(packages.size());
      packagesDataProvider_.setList(packages);
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
   
   public void setPackageStatus(String packageName, boolean loaded)
   {
      int row = packageRow(packageName) ;
      
      if (row != -1)
      {
         List<PackageInfo> packages = packagesDataProvider_.getList();
        
         packages.set(row, loaded ? packages.get(row).asLoaded() :
                                    packages.get(row).asUnloaded());
      }
   }
   
   private int packageRow(String packageName)
   {
      // if we haven't retreived packages yet then return not found
      if (packagesDataProvider_ == null)
         return -1;
      
      List<PackageInfo> packages = packagesDataProvider_.getList();
      
      // figure out which row of the table includes this package
      int row = -1;
      for (int i=0; i<packages.size(); i++)
      {
         PackageInfo packageInfo = packages.get(i);
         if (packageInfo.getName().equals(packageName))
         {
            row = i ;
            break;
         }
      }
      return row ;
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
     
      toolbar.addLeftWidget(commands_.installPackage().createToolbarButton());
      toolbar.addLeftWidget(commands_.updatePackages().createToolbarButton());
      searchWidget_ = new SearchWidget(new SuggestOracle() {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            // no suggestions
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<Suggestion>()));
         }
      });
      searchWidget_.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            observer_.onPackageFilterChanged(event.getValue());   
         }
      });
      toolbar.addRightWidget(searchWidget_);
      
      return toolbar;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      packagesTable_ = new CellTable<PackageInfo>(
                        15,
                        GWT.<Resources> create(Resources.class));
      packagesTable_.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      packagesTable_.setSelectionModel(new NoSelectionModel<PackageInfo>());
      packagesTable_.setWidth("100%", true);
        
      LoadedColumn loadedColumn = new LoadedColumn();
      packagesTable_.addColumn(loadedColumn);
      packagesTable_.setColumnWidth(loadedColumn, 35, Unit.PX);
      
      NameColumn nameColumn = new NameColumn();
      packagesTable_.addColumn(nameColumn);
      packagesTable_.setColumnWidth(nameColumn, 20, Unit.PCT);
    
      TextColumn<PackageInfo> descColumn = new TextColumn<PackageInfo>() {
         public String getValue(PackageInfo packageInfo)
         {
            return packageInfo.getDesc();
         } 
      };  
      
      packagesTable_.addColumn(descColumn);
      packagesTable_.setColumnWidth(descColumn, 80, Unit.PCT);
      
      RemoveColumn removeColumn = new RemoveColumn();
      packagesTable_.addColumn(removeColumn);
      packagesTable_.setColumnWidth(removeColumn, 33, Unit.PX);
      
      
      packagesDataProvider_ = new ListDataProvider<PackageInfo>();
      packagesDataProvider_.addDataDisplay(packagesTable_);
      
      ScrollPanel scrollPanel = new ScrollPanel();
      scrollPanel.setWidget(packagesTable_);
      return scrollPanel;
   }
   
   
   class LoadedColumn extends Column<PackageInfo, Boolean>
   {
      public LoadedColumn()
      {
         super(new CheckboxCell(false, false));
         
         setFieldUpdater(new FieldUpdater<PackageInfo,Boolean>() {
            public void update(int index, PackageInfo packageInfo, Boolean value)
            {
               if (value.booleanValue())
                  observer_.loadPackage(packageInfo.getName()) ;
               else
                  observer_.unloadPackage(packageInfo.getName()) ;
               
            }    
         });
      }
      
      @Override
      public Boolean getValue(PackageInfo packageInfo)
      {
         return packageInfo.isLoaded();
      }
      
   }
   
   class RemoveColumn extends Column<PackageInfo, String>
   {
      public RemoveColumn()
      {
         super(new ButtonCell(){
            @Override
            public void render(Context context, 
                                  SafeHtml value, 
                                  SafeHtmlBuilder sb) 
            {   
               sb.appendHtmlConstant(removeButtonPrototype_.getHTML());
            }                                
         });

         setFieldUpdater(new FieldUpdater<PackageInfo,String>() {
            public void update(int index, PackageInfo pkgInfo, String value)
            {
               observer_.removePackage(pkgInfo);
            }
         });
      }

      @Override
      public String getValue(PackageInfo object)
      {
         return null;
      }
   }
   
   final static AbstractImagePrototype removeButtonPrototype_ = 
      AbstractImagePrototype.create(ThemeResources.INSTANCE.removePackage());
   
   
   // package name column which includes a hyperlink to package docs
   class NameColumn extends Column<PackageInfo, String>
   {
      public NameColumn()
      {
         super(new ClickableTextCell(){
            
            // render anchor using custom styles. detect selection and
            // add selected style to invert text color
            @Override
            protected void render(Context context, 
                                  SafeHtml value, 
                                  SafeHtmlBuilder sb) 
            {   
               if (value != null) 
               {
                 Styles styles = RESOURCES.styles();
                 StringBuilder div = new StringBuilder();
                 div.append("<div class=\"");
                 div.append(styles.packageNameLink());
                 div.append("\">");
                 
                 sb.appendHtmlConstant(div.toString());
                 sb.append(value);
                 sb.appendHtmlConstant("</div>");
               }
             }
            
            // click event which occurs on the actual package link div
            // results in showing help for that package
            @Override
            public void onBrowserEvent(Context context, Element parent, 
                                       String value, NativeEvent event, 
                                       ValueUpdater<String> valueUpdater) 
            {
              super.onBrowserEvent(context, parent, value, event, valueUpdater);
              if ("click".equals(event.getType()))
              {  
                 // verify that the click was on the package link
                 JavaScriptObject evTarget = event.getEventTarget().cast();
                 if (Element.is(evTarget) &&
                     Element.as(evTarget).getClassName().startsWith(
                                        RESOURCES.styles().packageNameLink()))
                 {  
                    observer_.showHelp(
                      packagesDataProvider_.getList().get(context.getIndex()));
                 }
              }
            }            
         });
      }
      
      @Override
      public String getValue(PackageInfo packageInfo)
      {
         return packageInfo.getName();
      }
   }
   
   static interface Styles extends CssResource
   {
      String packageNameLink();
      String packageNameLinkSelected();
   }
  
   interface Resources extends CellTable.Resources 
   {
      @Source("PackagesPane.css")
      Styles styles();
      
      @Source("PackagesCellTable.css")
      CellTable.Style cellTableStyle();    
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }

   private CellTable<PackageInfo> packagesTable_;
   private ListDataProvider<PackageInfo> packagesDataProvider_;
   private SearchWidget searchWidget_;
   private PackagesDisplayObserver observer_ ;
   private final Commands commands_;
}
