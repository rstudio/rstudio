/*
 * RStudio.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

package org.rstudio.studio.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.LinkColumn;
import org.rstudio.core.client.files.filedialog.FileDialogResources;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.CaptionWithHelp;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.LocalRepositoriesWidget;
import org.rstudio.core.client.widget.ProgressCallback;
import org.rstudio.core.client.widget.ProgressDialog;
import org.rstudio.core.client.widget.ResizeGripper;
import org.rstudio.core.client.widget.SlideLabel;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.core.client.widget.WizardResources;
import org.rstudio.core.client.widget.images.ProgressImages;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.ui.AboutDialogContents;
import org.rstudio.studio.client.application.ui.appended.ApplicationEndedPopupPanel;
import org.rstudio.studio.client.application.ui.serializationprogress.ApplicationSerializationProgress;
import org.rstudio.studio.client.application.ui.support.SupportPopupMenu;
import org.rstudio.studio.client.common.StudioResources;
import org.rstudio.studio.client.common.mirrors.ChooseMirrorDialog;
import org.rstudio.studio.client.common.repos.SecondaryReposDialog;
import org.rstudio.studio.client.common.repos.SecondaryReposWidget;
import org.rstudio.studio.client.common.rpubs.ui.RPubsUploadDialog;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerListResources;
import org.rstudio.studio.client.common.spelling.ui.SpellingCustomDictionariesWidget;
import org.rstudio.studio.client.common.vcs.CreateKeyDialog;
import org.rstudio.studio.client.common.vcs.ShowPublicKeyDialog;
import org.rstudio.studio.client.common.vcs.SshKeyWidget;
import org.rstudio.studio.client.common.vcs.ignore.IgnoreDialog;
import org.rstudio.studio.client.htmlpreview.HTMLPreviewApplication;
import org.rstudio.studio.client.notebookv2.CompileNotebookv2OptionsDialog;
import org.rstudio.studio.client.packrat.ui.PackratActionDialog;
import org.rstudio.studio.client.packrat.ui.PackratResolveConflictDialog;
import org.rstudio.studio.client.plumber.PlumberAPISatellite;
import org.rstudio.studio.client.projects.ui.newproject.NewProjectResources;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesDialogResources;
import org.rstudio.studio.client.rmarkdown.RmdOutputSatellite;
import org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy;
import org.rstudio.studio.client.shiny.ShinyApplicationSatellite;
import org.rstudio.studio.client.vcs.VCSApplication;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchResources;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotResources;
import org.rstudio.studio.client.workbench.prefs.views.PreferencesDialog;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.views.buildtools.ui.BuildPaneResources;
import org.rstudio.studio.client.workbench.views.connections.ui.NewConnectionShinyHost;
import org.rstudio.studio.client.workbench.views.connections.ui.NewConnectionSnippetDialog;
import org.rstudio.studio.client.workbench.views.connections.ui.NewConnectionSnippetHost;
import org.rstudio.studio.client.workbench.views.connections.ui.NewConnectionWizard;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.files.ui.FilesListDataGridResources;
import org.rstudio.studio.client.workbench.views.history.view.HistoryPane;
import org.rstudio.studio.client.workbench.views.history.view.Shelf;
import org.rstudio.studio.client.workbench.views.packages.ui.CheckForUpdatesDialog;
import org.rstudio.studio.client.workbench.views.packages.ui.InstallPackageDialog;
import org.rstudio.studio.client.workbench.views.packages.ui.PackagesCellTableResources;
import org.rstudio.studio.client.workbench.views.plots.ui.manipulator.ManipulatorResources;
import org.rstudio.studio.client.workbench.views.source.SourceSatellite;
import org.rstudio.studio.client.workbench.views.source.editors.codebrowser.CodeBrowserEditingTargetWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkSatellite;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionResources;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermWidget;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.LineTableView;
import org.rstudio.studio.client.workbench.views.vcs.dialog.DiffFrame;
import org.rstudio.studio.client.workbench.views.environment.dataimport.ImportFileSettingsDialog;

public class RStudio implements EntryPoint
{  
   public void onModuleLoad() 
   {
      Debug.injectDebug();
      maybeSetWindowName("rstudio-" + StringUtil.makeRandomId(16));
      maybeDelayLoadApplication(this);
   }
   
   private Command showProgress(ProgressCallback callback)
   {
      final Label background = new Label();
      background.getElement().getStyle().setZIndex(1000);
      background.getElement().getStyle().setBackgroundColor("#e1e2e5");
      final RootLayoutPanel rootPanel = RootLayoutPanel.get();
      rootPanel.add(background);
      rootPanel.setWidgetTopBottom(background, 0, Style.Unit.PX, 
                                               0, Style.Unit.PX);
      rootPanel.setWidgetLeftRight(background, 0, Style.Unit.PX, 
                                               0, Style.Unit.PX);
      
      String progressUrl = ProgressImages.createLargeGray().getUrl();
      StringBuilder str = new StringBuilder();
      str.append("<img src=\"");
      str.append(progressUrl);
      str.append("\"");
      if (BrowseCap.devicePixelRatio() > 1.0)
         str.append("width=24 height=24");
      str.append("/>");
      final SimplePanel progressPanel = new SimplePanel();
      Element div = progressPanel.getElement();
      div.setInnerHTML(str.toString());
      ElementIds.assignElementId(div, ElementIds.LOADING_SPINNER);

      final VerticalPanel vertical = new VerticalPanel();
      vertical.add(progressPanel);
      vertical.setCellHorizontalAlignment(progressPanel, VerticalPanel.ALIGN_CENTER);

      if (callback != null)
      {
         final Button actionButton = new Button();
         actionButton.addClickHandler(evt -> callback.callback().execute());
         actionButton.setText(callback.action());
         vertical.add(actionButton);
         vertical.setCellHorizontalAlignment(actionButton, VerticalPanel.ALIGN_CENTER);
      }
      
      div = vertical.getElement();
      div.getStyle().setWidth(100, Style.Unit.PCT);
      div.getStyle().setMarginTop(200, Style.Unit.PX);
      div.getStyle().setProperty("textAlign", "center");
      div.getStyle().setZIndex(1000);

      rootPanel.add(vertical);
     
      return new Command()
      {
         public void execute()
         {
            try
            {
               rootPanel.remove(vertical);
               rootPanel.remove(background);
            }
            catch (Exception e)
            {
               Debug.log(e.toString());
            }
         }
      };
   }
   
   private static final native void maybeSetWindowName(String name)
   /*-{
      $wnd.name = $wnd.name || name;
   }-*/;
   
   private static final native void maybeDelayLoadApplication(RStudio rstudio)
   /*-{
      if ($wnd.qt)
      {
         // on the desktop main window, we may need to wait for Qt to finish
         // initialization before loading GWT
         if ($wnd.qt.webChannelReady)
         {
            // Qt is ready; load the application now
            rstudio.@org.rstudio.studio.client.RStudio::delayLoadApplication()();
         }
         else
         {
            // Qt not yet ready; set a hook and let the Qt WebChannel
            // initialization script call it to finish initialization
            $wnd.rstudioDelayLoadApplication = $entry(function() {
               rstudio.@org.rstudio.studio.client.RStudio::delayLoadApplication()();
            });
            
            // set a timeout and attempt load just in case something goes wrong with
            // Qt initialization (we don't want to just leave the user with a blank
            // window)
            setTimeout(function() {
               if (typeof $wnd.rstudioDelayLoadApplication == "function") {
                  
                  // let the user know things might go wrong
                  var msg = "WARNING: RStudio launched before desktop initialization known to be complete!";
                  @org.rstudio.core.client.Debug::log(Ljava/lang/String;)(msg);
                  
                  // begin load
                  $wnd.rstudioDelayLoadApplication();
                  $wnd.rstudioDelayLoadApplication = null;
               }
            }, 60000);
         }
      }
      else
      {
         // server and satellites can load as usual
         rstudio.@org.rstudio.studio.client.RStudio::delayLoadApplication()();
      }
      
   }-*/;
   
   private void delayLoadApplication()
   {
      ProgressCallback callback = null;
      
      // if we are loading the main window, add a button for bailing out and
      // retrying in safe mode
      String view = Window.Location.getParameter("view");
      if (StringUtil.isNullOrEmpty(view))
      {
         callback = new ProgressCallback("Retry in Safe Mode",
               () -> { Debug.devlog("retry"); });
      }

      dismissProgressAnimation_ = showProgress(callback);

      final SerializedCommandQueue queue = new SerializedCommandQueue();
      
      // TODO (gary) This early loading of XTermWidget dependencies needs to be
      // removed once I figure out why XTermWidget.load in 
      // TerminalPane:createMainWidget) isn't sufficient. Suspect due to xterm.js
      // loading its add-ons (fit.js) but need to investigate. 
      queue.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(Command continuation)
         {
            XTermWidget.load(continuation);
         }
      });
      
      // ensure Ace is loaded up front
      queue.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(Command continuation)
         {
            AceEditor.load(continuation);
         }
      });
      
      // load the requested page
      queue.addCommand(new SerializedCommand()
      {
         @Override
         public void onExecute(Command continuation)
         {
            onDelayLoadApplication();
         }
      });
      
      GWT.runAsync(new RunAsyncCallback()
      {
         @Override
         public void onSuccess()
         {
            queue.run();
         }
         
         @Override
         public void onFailure(Throwable reason)
         {
            dismissProgressAnimation_.execute();
            Window.alert("Error: " + reason.getMessage());
         }
      });
   }
   
   private void onDelayLoadApplication()
   {
      ensureStylesInjected();

      String view = Window.Location.getParameter("view");
      if (VCSApplication.NAME.equals(view))
      {
         RStudioGinjector.INSTANCE.getVCSApplication().go(
               RootLayoutPanel.get(),
               dismissProgressAnimation_);
      }
      else if (HTMLPreviewApplication.NAME.equals(view))
      {
         RStudioGinjector.INSTANCE.getHTMLPreviewApplication().go(
               RootLayoutPanel.get(),
               dismissProgressAnimation_);
      }
      else if (ShinyApplicationSatellite.NAME.equals(view))
      {
         RStudioGinjector.INSTANCE.getShinyApplicationSatellite().go(
               RootLayoutPanel.get(),
               dismissProgressAnimation_);
      }
      else if (RmdOutputSatellite.NAME.equals(view))
      {
         RStudioGinjector.INSTANCE.getRmdOutputSatellite().go(
               RootLayoutPanel.get(), 
               dismissProgressAnimation_);
      }
      else if (view != null && 
            view.startsWith(SourceSatellite.NAME_PREFIX))
      {
         SourceSatellite satellite = new SourceSatellite(view);
         satellite.go(RootLayoutPanel.get(), 
               dismissProgressAnimation_);
      }
      else if (view != null && 
            view.startsWith(ChunkSatellite.NAME_PREFIX))
      {
         ChunkSatellite satellite = new ChunkSatellite(view);
         satellite.go(RootLayoutPanel.get(), 
               dismissProgressAnimation_);
      }
      else if (PlumberAPISatellite.NAME.equals(view))
      {
         RStudioGinjector.INSTANCE.getPlumberAPISatellite().go(
               RootLayoutPanel.get(),
               dismissProgressAnimation_);
      }
      else
      {
         RStudioGinjector.INSTANCE.getApplication().go(
               RootLayoutPanel.get(),
               dismissProgressAnimation_);
      }
   }
   
   private void ensureStylesInjected()
   {
      ThemeResources.INSTANCE.themeStyles().ensureInjected();
      CoreResources.INSTANCE.styles().ensureInjected();
      StudioResources.INSTANCE.styles().ensureInjected();
      ConsoleResources.INSTANCE.consoleStyles().ensureInjected();
      FileDialogResources.INSTANCE.styles().ensureInjected();
      ManipulatorResources.INSTANCE.manipulatorStyles().ensureInjected();
      PackagesCellTableResources.INSTANCE.cellTableStyle().ensureInjected();
      FilesListDataGridResources.INSTANCE.dataGridStyle().ensureInjected();
      ExportPlotResources.INSTANCE.styles().ensureInjected();
      CodeSearchResources.INSTANCE.styles().ensureInjected();
      SourceMarkerListResources.INSTANCE.styles().ensureInjected();
      BuildPaneResources.INSTANCE.styles().ensureInjected();
      
      ProgressDialog.ensureStylesInjected();
      SupportPopupMenu.ensureStylesInjected();
      SlideLabel.ensureStylesInjected();
      ThemedButton.ensureStylesInjected();
      ThemedPopupPanel.ensureStylesInjected();
      InstallPackageDialog.ensureStylesInjected();
      ApplicationEndedPopupPanel.ensureStylesInjected();
      ApplicationSerializationProgress.ensureStylesInjected();
      HistoryPane.ensureStylesInjected();
      Shelf.ensureStylesInjected();
      ImportFileSettingsDialog.ensureStylesInjected();
      FindReplaceBar.ensureStylesInjected();
      FontSizer.ensureStylesInjected();
      PreferencesDialogBaseResources.INSTANCE.styles().ensureInjected();
      PreferencesDialog.ensureStylesInjected();
      ProjectPreferencesDialogResources.INSTANCE.styles().ensureInjected();
      LinkColumn.ensureStylesInjected();
      CaptionWithHelp.ensureStylesInjected();
      CheckForUpdatesDialog.ensureStylesInjected();
      UnsavedChangesDialog.ensureStylesInjected();
      ChooseMirrorDialog.ensureStylesInjected();
      ResizeGripper.ensureStylesInjected();
      LineTableView.ensureStylesInjected();
      ChangelistTable.ensureStylesInjected();
      DiffFrame.ensureStylesInjected();
      CodeBrowserEditingTargetWidget.ensureStylesInjected();
      ShowPublicKeyDialog.ensureStylesInjected();
      CreateKeyDialog.ensureStylesInjected();
      SshKeyWidget.ensureStylesInjected();
      IgnoreDialog.ensureStylesInjected();
      SpellingCustomDictionariesWidget.ensureStylesInjected();
      RPubsUploadDialog.ensureStylesInjected();
      WizardResources.INSTANCE.styles().ensureInjected();
      NewProjectResources.INSTANCE.styles().ensureInjected();
      AboutDialogContents.ensureStylesInjected();
      CompileNotebookv2OptionsDialog.ensureStylesInjected();
      PackratResolveConflictDialog.ensureStylesInjected();
      PackratActionDialog.ensureStylesInjected();
      LocalRepositoriesWidget.ensureStylesInjected();
      CppCompletionResources.INSTANCE.styles().ensureInjected();
      RSConnectDeploy.RESOURCES.style().ensureInjected();
      NewConnectionShinyHost.ensureStylesInjected();
      NewConnectionSnippetHost.ensureStylesInjected();
      NewConnectionSnippetDialog.ensureStylesInjected();
      NewConnectionWizard.ensureStylesInjected();
      SecondaryReposWidget.ensureStylesInjected();
      SecondaryReposDialog.ensureStylesInjected();
      
      StyleInjector.inject(
            "button::-moz-focus-inner {border:0}");
   }
   
   private Command dismissProgressAnimation_;
}
