/*
 * PackagesPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.ImageButtonColumn;
import org.rstudio.core.client.cellview.ImageButtonColumn.TitleProvider;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.RStudioDataGrid;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.packrat.model.PackratContext;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.projects.ProjectContext;
import org.rstudio.studio.client.workbench.projects.RenvContext;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInfo;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallContext;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallRequest;
import org.rstudio.studio.client.workbench.views.packages.model.PackageLibraryUtils;
import org.rstudio.studio.client.workbench.views.packages.model.PackageManagerRepository;
import org.rstudio.studio.client.workbench.views.packages.model.PackageStatus;
import org.rstudio.studio.client.workbench.views.packages.model.PackageVulnerabilityTypes.RepositoryPackageVulnerabilityListMap;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.packages.ui.InstallPackageDialog;
import org.rstudio.studio.client.workbench.views.packages.ui.PackageLinkColumn;
import org.rstudio.studio.client.workbench.views.packages.ui.PackageManagerSelectRepositoryModalDialog;
import org.rstudio.studio.client.workbench.views.packages.ui.PackagesCellTableResources;
import org.rstudio.studio.client.workbench.views.packages.ui.PackagesDataGridResources;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.DefaultCellTableBuilder;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.inject.Inject;

import jsinterop.base.Js;

public class PackagesPane extends WorkbenchPane implements Packages.Display
{
   private class WidgetTextHeader extends TextHeader
   {
      public WidgetTextHeader()
      {
         super("");
      }

      @Override
      public String getHeaderStyleNames()
      {
         return dataGridRes_.dataGridStyle().widgetColumnHeader();
      }
   }

   @Inject
   public PackagesPane(Commands commands, 
                       Session session,
                       GlobalDisplay display,
                       EventBus events,
                       PackagesServerOperations server)
   {
      super(constants_.packagesTitle(), events);

      commands_ = commands;
      session_ = session;
      display_ = display;
      server_ = server;
      
      dataGridRes_ = GWT.create(PackagesDataGridResources.class);
      ensureWidget();
   }
   
   @Override
   public void setObserver(PackagesDisplayObserver observer)
   {
      observer_ = observer;
   }
   
   @Override
   public void setPackageState(ProjectContext projectContext, 
                               List<PackageInfo> packages,
                               RepositoryPackageVulnerabilityListMap vulns,
                               JsObject activeRepository)
   {
      projectContext_ = projectContext;
      activeRepository_ = activeRepository;
      vulns_ = vulns;

      packagesDataProvider_.setList(packages);
      createPackagesTable();

      // manage visibility of repository button
      String reposLabel = getRepositoryButtonLabel();
      String reposTitle = getRepositoryButtonTitle();
      repositoryButton_.setText(StringUtil.notNull(reposLabel));
      repositoryButton_.setTitle(reposTitle);
      repositoryButton_.setVisible(!StringUtil.isNullOrEmpty(reposLabel));

      // manage visibility of Packrat / renv menu buttons
      PackratContext packratContext = projectContext_.getPackratContext();
      RenvContext renvContext = projectContext_.getRenvContext();
      
      projectButtonSeparator_.setVisible(false);
      packratMenuButton_.setVisible(false);
      snapshotButton_.setVisible(false);
      snapshotRestoreSeparator_.setVisible(false);
      restoreButton_.setVisible(false);
      helpButton_.setVisible(false);
      helpSeparator_.setVisible(false);

      if (packratContext.isModeOn())
      {
         projectButtonSeparator_.setVisible(true);
         packratMenuButton_.setVisible(true);
      }
      else if (renvContext.active)
      {
         projectButtonSeparator_.setVisible(true);
         snapshotButton_.setVisible(true);
         snapshotRestoreSeparator_.setVisible(true);
         restoreButton_.setVisible(true);
         helpButton_.setVisible(true);
         helpSeparator_.setVisible(true);
      }
   }
   
   @Override
   public void installPackage(PackageInstallContext installContext,
                              PackageInstallOptions defaultInstallOptions,
                              PackagesServerOperations server,
                              GlobalDisplay globalDisplay,
                              OperationWithInput<PackageInstallRequest> operation)
   {
      new InstallPackageDialog(installContext,
                               defaultInstallOptions,
                               server, 
                               globalDisplay, 
                               operation).showModal();
   }
   
   @Override
   public void setPackageStatus(PackageStatus status)
   {
      int row = packageRow(status.getName(), status.getLibrary());
      
      if (row != -1)
      {
         List<PackageInfo> packages = packagesDataProvider_.getList();
         PackageInfo packageInfo = packages.get(row);
         packageInfo.setAttached(status.isAttached());
         packages.set(row, packageInfo);
      }
      
      // go through any duplicates to reconcile their status
      // (in case the same package is installed into multiple libraries)
      List<PackageInfo> packages = packagesDataProvider_.getList();
      for (int i = 0; i < packages.size(); i++)
      {
         if (i == row)
         {
            continue;
         }
         else if (packages.get(i).getName() == status.getName())
         {
            PackageInfo packageInfo = packages.get(i);
            packageInfo.setAttached(false);
            packages.set(i, packageInfo);
         }
      }
   }
   
   private int packageRow(String packageName, String packageLib)
   {
      // if we haven't retrieved packages yet then return not found
      if (packagesDataProvider_ == null)
         return -1;
      
      List<PackageInfo> packages = packagesDataProvider_.getList();
      
      // figure out which row of the table includes this package
      int row = -1;
      for (int i=0; i<packages.size(); i++)
      {
         PackageInfo packageInfo = packages.get(i);
         if (packageInfo.getName() == packageName &&
             packageInfo.getLibrary() == packageLib)
         {
            row = i;
            break;
         }
      }
      return row;
   }

   private void selectRepository(PackageManagerRepository ppmRepo)
   {
      server_.selectRepository(ppmRepo.getName(), ppmRepo.getSnapshot(), new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject response)
         {
            if (Js.isFalsy(response))
               return;

            String name  = response.getString("name");
            String value = response.getString("value");

            if (StringUtil.isNullOrEmpty(name))
            {
               String code = "options(repos = \"" + value + "\")";
               events_.fireEvent(new SendToConsoleEvent(code, true));
            }
            else
            {
               String code = "options(repos = c(" + name + " = \"" + value + "\"))";
               events_.fireEvent(new SendToConsoleEvent(code, true));
            }
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }

   private void showRepositoryMenu()
   {
      server_.getRepositories(new ServerRequestCallback<JsArray<PackageManagerRepository>>()
      {
         @Override
         public void onResponseReceived(JsArray<PackageManagerRepository> response)
         {
            OperationWithInput<PackageManagerRepository> onSelected = new OperationWithInput<>()
            {
               @Override
               public void execute(PackageManagerRepository input)
               {
                  server_.selectRepository(input.getName(), input.getSnapshot(), new ServerRequestCallback<JsObject>()
                  {
                     @Override
                     public void onResponseReceived(JsObject response)
                     {
                        if (Js.isFalsy(response))
                           return;

                        String name = response.getString("name");
                        String value = response.getString("value");

                        if (StringUtil.isNullOrEmpty(name))
                        {
                           String code = "options(repos = \"" + value + "\")";
                           events_.fireEvent(new SendToConsoleEvent(code, true));
                        }
                        else
                        {
                           String code = "options(repos = c(" + name + " = \"" + value + "\"))";
                           events_.fireEvent(new SendToConsoleEvent(code, true));
                        }
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        Debug.logError(error);
                     }
                  });
               }
            };

            PackageManagerSelectRepositoryModalDialog dialog =
               new PackageManagerSelectRepositoryModalDialog(response, onSelected);
            
            dialog.showModal();
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar(constants_.packagesTabLabel());
     
      // install packages
      toolbar.addLeftWidget(commands_.installPackage().createToolbarButton());
      
      // update packages
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.updatePackages().createToolbarButton());

      projectButtonSeparator_ = toolbar.addLeftSeparator();
      
      // create packrat menu + button
      ToolbarPopupMenu packratMenu = new ToolbarPopupMenu();
      packratMenu.addItem(commands_.packratHelp().createMenuItem(false));
      packratMenu.addSeparator();
      packratMenu.addItem(commands_.packratCheckStatus().createMenuItem(false));
      packratMenu.addItem(commands_.packratClean().createMenuItem(false));
      packratMenu.addItem(commands_.packratBundle().createMenuItem(false));
      packratMenu.addSeparator();
      packratMenu.addItem(commands_.packratOptions().createMenuItem(false));
      packratMenuButton_ = new ToolbarMenuButton(
            "Packrat", 
            ToolbarButton.NoTitle,
            commands_.packratBootstrap().getImageResource(),
            packratMenu
       );
      toolbar.addLeftWidget(packratMenuButton_);
      packratMenuButton_.setVisible(false);
      
      // create renv menu + button
      snapshotButton_ = commands_.renvSnapshot().createToolbarButton(false);
      toolbar.addLeftWidget(snapshotButton_);
      snapshotButton_.setVisible(false);

      snapshotRestoreSeparator_ = toolbar.addLeftSeparator();

      restoreButton_ = commands_.renvRestore().createToolbarButton(false);
      toolbar.addLeftWidget(restoreButton_);
      restoreButton_.setVisible(false);

      searchWidget_ = new SearchWidget(constants_.filterByPackageNameLabel(), new SuggestOracle() {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            // no suggestions
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<>()));
         }
      });
      
      searchWidget_.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            observer_.onPackageFilterChanged(event.getValue().trim());   
         }
      });
      
      helpButton_ = commands_.renvHelp().createToolbarButton(false);
      toolbar.addRightWidget(helpButton_);
      helpButton_.setVisible(false);
      helpSeparator_ = toolbar.addRightSeparator();
      helpSeparator_.setVisible(false);

      repositoryButton_ = new ToolbarButton(
         "",
         "",
         (ImageResource) null,
         new ClickHandler()
         {
            @Override
            public void onClick(ClickEvent event)
            {
               showRepositoryMenu();
            }
         });

      repositoryButton_.setVisible(false);
      toolbar.addRightWidget(repositoryButton_);
      toolbar.addRightSeparator();

      ElementIds.assignElementId(searchWidget_, ElementIds.SW_PACKAGES);
      toolbar.addRightWidget(searchWidget_);
      toolbar.addRightSeparator();

      ToolbarButton refreshButton = commands_.refreshPackages().createToolbarButton();
      refreshButton.addStyleName(ThemeStyles.INSTANCE.refreshToolbarButton());
      toolbar.addRightWidget(refreshButton);
      
      return toolbar;
   }
   
   private class VersionCell extends AbstractCell<PackageInfo>
   {
      public VersionCell(boolean packratVersion)
      {
         packratVersion_ = packratVersion;
      }

      @Override
      public void render(Context context, PackageInfo value, SafeHtmlBuilder sb)
      {
         sb.appendHtmlConstant("<div title=\"");
         if (!packratVersion_)
            sb.appendEscaped(value.getLibrary());
         sb.appendHtmlConstant("\"");
         sb.appendHtmlConstant(">");
         if (packratVersion_)
         {
            sb.appendEscaped(value.getPackratVersion());
         }
         else
         {
            sb.appendEscaped(value.getVersion());
         }
         sb.appendHtmlConstant("</div>"); 
      }
      
      private boolean packratVersion_;
   }

   private class SourceCell extends AbstractCell<PackageInfo>
   {
      @Override
      public void render(Context context, PackageInfo value, SafeHtmlBuilder sb)
      {
         String pkgSource = value.getPackageSource();
         String source = (pkgSource != null) ? pkgSource : "[Unknown]";
         sb.append(renderText(source));
      }
   }

   private class MetadataCell extends AbstractCell<PackageInfo>
   {
      @Override
      public void render(Context context, PackageInfo object, SafeHtmlBuilder sb)
      {
         sb.appendHtmlConstant("<div style=\"font-style: oblique;\">");
         sb.appendEscaped(object.getMetadata());
         sb.appendHtmlConstant("</div>");
      }
   }
   
   @Override
   protected Widget createMainWidget()
   {
      packagesDataProvider_ = new ListDataProvider<>();
      packagesTableContainer_ = new LayoutPanel();
      packagesTableContainer_.addStyleName("ace_editor_theme");
      return packagesTableContainer_;
   }
   
   @Override
   public void onResize()
   {
      super.onResize();
      
      if (packagesTable_ != null)
         packagesTable_.onResize();
   }
   
   @Override
   public void onSelected()
   {
      super.onSelected();
      
      // If the packages table is created while the tab isn't visible, it will
      // have a cached height of 0. Refresh this height when the tab is 
      // selected.
      if (packagesTable_ != null)
         packagesTable_.onResize();
   }
   
   private void createPackagesTable()
   {
      try
      {
         packagesTableContainer_.clear();
         packagesTable_ = new RStudioDataGrid<>(
            packagesDataProvider_.getList().size(),
            dataGridRes_);
      }
      catch (Exception e)
      {
         // constructing the data grid can fail in superdevmode; so if we're
         // in superdevmode, try a few times 
         if (SuperDevMode.isActive())
         {
            if (gridRenderRetryCount_ >= 5)
            {
               Debug.log("WARNING: Failed to render packages pane data grid");
            }
            gridRenderRetryCount_++;
            Debug.log("WARNING: Retrying packages data grid render (" + 
                      gridRenderRetryCount_ + ")");
            Timer t = new Timer() {
               @Override
               public void run()
               {
                  createPackagesTable();
               }
            };
            t.schedule(5);
         }
      }
      
      if (packagesTable_ != null)
      {
         initPackagesTable();
      }
   }
   
   private boolean showButtonImpl(PackageInfo object)
   {
      if (object.getPackageSource() == null)
         return false;
      
      String source = object.getPackageSource();
      return source != "Base";
   }
   
   private void initPackagesTable()
   {
      packagesTable_.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      packagesTable_.setSelectionModel(new NoSelectionModel<>());
      packagesTable_.addCellPreviewHandler(new CellPreviewEvent.Handler<PackageInfo>()
      {
         @Override
         public void onCellPreview(CellPreviewEvent<PackageInfo> preview)
         {
            NativeEvent event = preview.getNativeEvent();
            if (event.getType() != BrowserEvents.CLICK)
               return;

            EventTarget target = event.getEventTarget();
            if (!Element.is(target))
               return;

            Element targetEl = Element.as(target);
            if (!targetEl.hasTagName(InputElement.TAG))
               return;

            event.stopPropagation();
            event.preventDefault();

            boolean isChecked = targetEl.hasAttribute("checked");
            if (isChecked)
               observer_.unloadPackage(preview.getValue());
            else
               observer_.loadPackage(preview.getValue());
         }
      });

      loadedColumn_ = new LoadedColumn();
      nameColumn_ = new NameColumn();
    
      descColumn_ = new Column<PackageInfo, PackageInfo>(new DescriptionCell())
      {
         @Override
         public PackageInfo getValue(PackageInfo object)
         {
            return object;
         } 
      };  
      
      sourceColumn_ = new Column<PackageInfo, PackageInfo>(new SourceCell())
      {
         @Override
         public PackageInfo getValue(PackageInfo object)
         {
            return object;
         }
      };

      versionColumn_ = new Column<PackageInfo, PackageInfo>(new VersionCell(false))
      {
         @Override
         public PackageInfo getValue(PackageInfo object)
         {
            return object;
         }
      };

      metadataColumn_ = new Column<PackageInfo, PackageInfo>(new MetadataCell())
      {
         @Override
         public PackageInfo getValue(PackageInfo object)
         {
            return object;
         }
      };

      helpColumn_ = new ImageButtonColumn<PackageInfo>(
            new ImageResource2x(ThemeResources.INSTANCE.helpSmall2x()),
            new OperationWithInput<PackageInfo>() {
               @Override
               public void execute(PackageInfo packageInfo)
               {
                  RStudioGinjector.INSTANCE.getGlobalDisplay().openWindow(packageInfo.getPackageUrl());
               }
            },
            new TitleProvider<PackageInfo>()
            {
               @Override
               public String get(PackageInfo object)
               {
                  return object.getPackageUrl();
               }
            })
      {
         @Override
         public boolean showButton(PackageInfo object)
         {
            return Js.isTruthy(object.getPackageUrl());
         }
      };

      browseColumn_ = new ImageButtonColumn<PackageInfo>(
            new ImageResource2x(ThemeResources.INSTANCE.browsePackage2x()),
            new OperationWithInput<PackageInfo>() {
               @Override
               public void execute(PackageInfo packageInfo)
               {
                  RStudioGinjector.INSTANCE.getGlobalDisplay().openWindow(packageInfo.getBrowseUrl());
               }
            },
            new TitleProvider<PackageInfo>()
            {
               @Override
               public String get(PackageInfo object)
               {
                  String source = object.getPackageSource();
                  String url = object.getBrowseUrl();

                  if (Js.isTruthy(source))
                  {
                     if (Js.isTruthy(url))
                     {
                        return constants_.browsePackageOn(source, url);
                     }
                     else
                     {
                        return "";
                     }
                  }
                  else
                  {
                     if (Js.isTruthy(url))
                     {
                        return constants_.browsePackageLabel(url);
                     }
                     else
                     {
                        return constants_.browsePackageCRANLabel();
                     }
                  }
               }
            })
      {
         @Override
         public boolean showButton(PackageInfo object)
         {
            return showButtonImpl(object);
         }
      };
      
      removeColumn_ = new ImageButtonColumn<PackageInfo>(
            new ImageResource2x(ThemeResources.INSTANCE.removePackage2x()),
            new OperationWithInput<PackageInfo>() {
               @Override
               public void execute(PackageInfo packageInfo)
               {
                  observer_.removePackage(packageInfo);
               }
            },
            constants_.removePackageTitle())
      {
         @Override
         public boolean showButton(PackageInfo object)
         {
            return showButtonImpl(object);
         }
      };

      // add common columns
      packagesTable_.addColumn(loadedColumn_, new TextHeader(""));
      packagesTable_.addColumn(nameColumn_, new TextHeader("Package"));
      packagesTable_.addColumn(descColumn_, new TextHeader(constants_.descriptionText()));
      packagesTable_.addColumn(metadataColumn_, new TextHeader("Risk Level"));
      packagesTable_.addColumn(sourceColumn_, new TextHeader(constants_.sourceText()));
      packagesTable_.addColumn(versionColumn_, new TextHeader(constants_.versionText()));

      // set initial column widths
      packagesTable_.setColumnWidth(loadedColumn_, 30, Unit.PX);
      packagesTable_.setColumnWidth(nameColumn_, 130, Unit.PX);
      packagesTable_.setColumnWidth(descColumn_, "auto");
      packagesTable_.setColumnWidth(metadataColumn_, 80, Unit.PX);
      packagesTable_.setColumnWidth(sourceColumn_, 180, Unit.PX);
      packagesTable_.setColumnWidth(versionColumn_, 100, Unit.PX);

      // add columns when using project-local library
      if (projectContext_.isActive())
      {
         lockfileVersionColumn_ = 
            new Column<PackageInfo, PackageInfo>(new VersionCell(true)) {

               @Override
               public PackageInfo getValue(PackageInfo object)
               {
                  return object;
               }
         };
      
         packagesTable_.addColumn(lockfileVersionColumn_, new TextHeader(constants_.lockfileText()));
         packagesTable_.setColumnWidth(lockfileVersionColumn_, 100, Unit.PX);
      }
     
      // help column is common
      packagesTable_.addColumn(helpColumn_, new WidgetTextHeader());
      packagesTable_.setColumnWidth(helpColumn_, 20, Unit.PX);

      // browse column is common
      packagesTable_.addColumn(browseColumn_, new WidgetTextHeader());
      packagesTable_.setColumnWidth(browseColumn_, 20, Unit.PX);

      // remove column is common (note that we allocate extra column
      // width to provide space for a scrollbar if needed)
      int scrollWidth = DomUtils.getScrollbarWidth();
      if (scrollWidth > 0)
         scrollWidth += 3;
      
      packagesTable_.addColumn(removeColumn_, new TextHeader(""));
      packagesTable_.setColumnWidth(removeColumn_, 20 + scrollWidth, Unit.PX);

      packagesTable_.setTableBuilder(new PackageTableBuilder(packagesTable_));
      packagesTable_.setSkipRowHoverCheck(true);
      
      packagesTableContainer_.add(packagesTable_);
      layoutPackagesTable();
      updateColumnWidths();
      
      // unbind old table from data provider in case we've re-generated the pane
      for (HasData<PackageInfo> display : packagesDataProvider_.getDataDisplays())
         packagesDataProvider_.removeDataDisplay(display);
      
      packagesDataProvider_.addDataDisplay(packagesTable_);
   }

   private void layoutPackagesTable()
   {
      layoutPackagesTable(0);
   }
   
   private void layoutPackagesTable(double top)
   {
      packagesTableContainer_.setWidgetLeftRight(
            packagesTable_, 0, Unit.PX, 0, Unit.PX);
      packagesTableContainer_.setWidgetTopBottom(
            packagesTable_, top, Unit.PX, 0, Unit.PX);
   }

   private void updateColumnWidths()
   {
   }

   class LoadedCell extends AbstractCell<PackageInfo>
   {
      public LoadedCell()
      {
      }

      @Override
      public void render(Context context, PackageInfo value, SafeHtmlBuilder sb)
      {
         SafeHtml checkbox = value.isAttached()
            ? TEMPLATES.checkboxChecked(value.getName())
            : TEMPLATES.checkboxUnchecked(value.getName());

         sb.append(checkbox);
      }
   }
   
   class LoadedColumn extends Column<PackageInfo, PackageInfo>
   {
      public LoadedColumn()
      {
         super(new LoadedCell());
      }

      @Override
      public PackageInfo getValue(PackageInfo packageInfo)
      {
         return packageInfo;
      }
   }
   
   // package name column which includes a hyperlink to package docs
   class NameColumn extends PackageLinkColumn
   {
      public NameColumn()
      {
         super(packagesDataProvider_, dataGridRes_.dataGridStyle(), vulns_, new OperationWithInput<PackageInfo>() 
         {
            @Override
            public void execute(PackageInfo packageInfo)
            {
               if (packageInfo.getHelpUrl() == null || 
                   packageInfo.getHelpUrl().length() == 0)
               {
                  display_.showMessage(GlobalDisplay.MSG_INFO, 
                        constants_.helpNotAvailableCaption(),
                        constants_.helpNotAvailableMessage(packageInfo.getName()));
               }
               else
               {
                  observer_.showHelp(packageInfo);
               }
            }
         },
         false);
      }
      
      @Override
      public PackageInfo getValue(PackageInfo packageInfo)
      {
         return packageInfo;
      }
   }
   
   class PackageTableBuilder extends DefaultCellTableBuilder<PackageInfo>
   {
      public PackageTableBuilder(AbstractCellTable<PackageInfo> cellTable)
      {
         super(cellTable);
      }

      @Override
      public void buildRowImpl(PackageInfo pkg, int idx)
      {
         String library = pkg.isInProjectLibrary() ? 
               pkg.getSourceLibrary() : pkg.getLibrary();
         if (pkg.isFirstInLibrary())
         {
           TableRowBuilder row = startRow();
           TableCellBuilder cell = row.startTD();
           cell.colSpan(5).className(
                 PackagesCellTableResources.INSTANCE.cellTableStyle()
                 .libraryHeader());
           cell.title(library);
           cell.startH1().text(
                 PackageLibraryUtils.getLibraryDescription(session_, library))
                 .endH1();
           row.endTD();
           
           row.endTR();
         }
         super.buildRowImpl(pkg, idx);
      }
   }   
   
   private class DescriptionCell extends AbstractCell<PackageInfo>
   {
      @Override
      public void render(Context context, PackageInfo pkgInfo, SafeHtmlBuilder sb)
      {
         String className;
         String packageDescription;

         if (pkgInfo.getDesc().isEmpty())
         {
            packageDescription = "(Not installed)";
            className = dataGridRes_.dataGridStyle().packageNotApplicableColumn();
         }
         else
         {
            packageDescription = pkgInfo.getDesc();
            className = dataGridRes_.dataGridStyle().packageColumn();
         }

         sb.append(TEMPLATES.description(className, packageDescription));
      }

   }

   private String getRepositoryButtonLabel()
   {
      if (activeRepository_ == null)
         return null;
      
      String repos = activeRepository_.getString("repos");
      if (Js.isFalsy(repos))
         return null;

      String snapshot = activeRepository_.getString("snapshot");
      if (Js.isFalsy(snapshot))
         return null;

      return repos + "/" + snapshot;
   }

   private String getRepositoryButtonTitle()
   {
      if (activeRepository_ == null)
         return null;

      String url = activeRepository_.getString("url");
      if (Js.isFalsy(url))
         return null;
      
      return url;
   }

   private final SafeHtml renderText(String text)
   {
      String className = dataGridRes_.dataGridStyle().packageColumn();
      return TEMPLATES.text(className, text);
   }

   interface Templates extends SafeHtmlTemplates
   {
      @Template("<div class=\"{0}\" title=\"{1}\">{1}</div>")
      SafeHtml text(String className, String text);

      @Template("<div class=\"{0}\" title=\"{1}\">{1}</div>")
      SafeHtml description(String className, String packageDescription);

      @Template("<input type=\"checkbox\" tabindex=\"-1\" aria-label=\"{0}\" checked />")
      SafeHtml checkboxChecked(String label);

      @Template("<input type=\"checkbox\" tabindex=\"-1\" aria-label=\"{0}\" />")
      SafeHtml checkboxUnchecked(String label);
   }

   private DataGrid<PackageInfo> packagesTable_;
   private ListDataProvider<PackageInfo> packagesDataProvider_;
   private ToolbarButton repositoryButton_;
   private SearchWidget searchWidget_;
   private PackagesDisplayObserver observer_;

   private LoadedColumn loadedColumn_;
   private NameColumn nameColumn_;
   private Column<PackageInfo, PackageInfo> descColumn_;
   private Column<PackageInfo, PackageInfo> versionColumn_;
   private Column<PackageInfo, PackageInfo> sourceColumn_;
   private Column<PackageInfo, PackageInfo> metadataColumn_;
   private ImageButtonColumn<PackageInfo> helpColumn_;
   private ImageButtonColumn<PackageInfo> browseColumn_;
   private ImageButtonColumn<PackageInfo> removeColumn_;

   private Column<PackageInfo, PackageInfo> lockfileVersionColumn_;
   
   private ToolbarMenuButton packratMenuButton_;
   private Widget projectButtonSeparator_;

   private ToolbarButton snapshotButton_;
   private Widget snapshotRestoreSeparator_;
   private ToolbarButton restoreButton_;
   private ToolbarButton helpButton_;
   private Widget helpSeparator_;
   
   private LayoutPanel packagesTableContainer_;
   private int gridRenderRetryCount_;
   private ProjectContext projectContext_;
   private JsObject activeRepository_;
   private RepositoryPackageVulnerabilityListMap vulns_;

   private final Commands commands_;
   private final Session session_;
   private final GlobalDisplay display_;
   private final PackagesServerOperations server_;

   private final PackagesDataGridResources dataGridRes_;

   private static final PackagesConstants constants_ = com.google.gwt.core.client.GWT.create(PackagesConstants.class);

   private static final Templates TEMPLATES = GWT.create(Templates.class);

}
