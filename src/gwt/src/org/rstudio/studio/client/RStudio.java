/*
 * RStudio.java
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

package org.rstudio.studio.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.cellview.LinkColumn;
import org.rstudio.core.client.files.filedialog.FileDialogResources;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.CaptionWithHelp;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.LocalRepositoriesWidget;
import org.rstudio.core.client.widget.ProgressDialog;
import org.rstudio.core.client.widget.ResizeGripper;
import org.rstudio.core.client.widget.SlideLabel;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.core.client.widget.WizardResources;
import org.rstudio.core.client.widget.images.ProgressImages;
import org.rstudio.studio.client.application.ui.AboutDialogContents;
import org.rstudio.studio.client.application.ui.appended.ApplicationEndedPopupPanel;
import org.rstudio.studio.client.application.ui.serializationprogress.ApplicationSerializationProgress;
import org.rstudio.studio.client.application.ui.support.SupportPopupMenu;
import org.rstudio.studio.client.common.StudioResources;
import org.rstudio.studio.client.common.mirrors.ChooseMirrorDialog;
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
import org.rstudio.studio.client.workbench.views.packages.ui.actions.ActionCenter;
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
      Command dismissProgressAnimation = showProgress();
      delayLoadApplication(dismissProgressAnimation);
   }

   private Command showProgress()
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
      final Element div = progressPanel.getElement();
      div.setInnerHTML(str.toString());
      div.getStyle().setWidth(100, Style.Unit.PCT);
      div.getStyle().setMarginTop(200, Style.Unit.PX);
      div.getStyle().setProperty("textAlign", "center");
      div.getStyle().setZIndex(1000);
      ElementIds.assignElementId(div, ElementIds.LOADING_SPINNER);
      rootPanel.add(progressPanel);
     
      return new Command()
      {
         public void execute()
         {
            try
            {
               rootPanel.remove(progressPanel);
               rootPanel.remove(background);
            }
            catch (Exception e)
            {
               Debug.log(e.toString());
            }
         }
      };
   }
   
   private void delayLoadApplication(final Command dismissProgressAnimation)
   {
      final RunAsyncCallback runCallback = new RunAsyncCallback()
      {
         public void onFailure(Throwable reason)
         {
            dismissProgressAnimation.execute();
            Window.alert("Error: " + reason.getMessage());
         }

         public void onSuccess()
         {
            // TODO (gary) This early loading of XTermWidget dependencies needs to be
            // removed once I figure out why XTermWidget.load in 
            // TerminalPane:createMainWidget) isn't sufficient. Suspect due to xterm.js
            // loading its add-ons (fit.js) but need to investigate. 
            XTermWidget.load(new Command()
            {
               public void execute()
               {
                  AceEditor.load(new Command()
                  {
                     public void execute()
                     {
                        ensureStylesInjected();

                        String view = Window.Location.getParameter("view");
                        if (VCSApplication.NAME.equals(view))
                        {
                           RStudioGinjector.INSTANCE.getVCSApplication().go(
                                 RootLayoutPanel.get(),
                                 dismissProgressAnimation);
                        }
                        else if (HTMLPreviewApplication.NAME.equals(view))
                        {
                           RStudioGinjector.INSTANCE.getHTMLPreviewApplication().go(
                                 RootLayoutPanel.get(),
                                 dismissProgressAnimation);
                        }
                        else if (ShinyApplicationSatellite.NAME.equals(view))
                        {
                           RStudioGinjector.INSTANCE.getShinyApplicationSatellite().go(
                                 RootLayoutPanel.get(),
                                 dismissProgressAnimation);
                        }
                        else if (RmdOutputSatellite.NAME.equals(view))
                        {
                           RStudioGinjector.INSTANCE.getRmdOutputSatellite().go(
                                 RootLayoutPanel.get(), 
                                 dismissProgressAnimation);
                        }
                        else if (view != null && 
                              view.startsWith(SourceSatellite.NAME_PREFIX))
                        {
                           SourceSatellite satellite = new SourceSatellite(view);
                           satellite.go(RootLayoutPanel.get(), 
                                 dismissProgressAnimation);
                        }
                        else if (view != null && 
                              view.startsWith(ChunkSatellite.NAME_PREFIX))
                        {
                           ChunkSatellite satellite = new ChunkSatellite(view);
                           satellite.go(RootLayoutPanel.get(), 
                                 dismissProgressAnimation);
                        }
                        else
                        {
                           RStudioGinjector.INSTANCE.getApplication().go(
                                 RootLayoutPanel.get(),
                                 dismissProgressAnimation);
                        }
                     }
                  });
               }
            });
         }
      };

      GWT.runAsync(runCallback);
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
      ActionCenter.ensureStylesInjected();
      PackratResolveConflictDialog.ensureStylesInjected();
      PackratActionDialog.ensureStylesInjected();
      LocalRepositoriesWidget.ensureStylesInjected();
      CppCompletionResources.INSTANCE.styles().ensureInjected();
      RSConnectDeploy.RESOURCES.style().ensureInjected();
      NewConnectionShinyHost.ensureStylesInjected();
      NewConnectionSnippetHost.ensureStylesInjected();
      NewConnectionSnippetDialog.ensureStylesInjected();
      NewConnectionWizard.ensureStylesInjected();
      
      StyleInjector.inject(
            "button::-moz-focus-inner {border:0}");
   }
}
