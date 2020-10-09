/*
 * RStudio.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.LinkColumn;
import org.rstudio.core.client.files.filedialog.FileDialogResources;
import org.rstudio.core.client.layout.DelayFadeInHelper;
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
import org.rstudio.studio.client.application.ApplicationAction;
import org.rstudio.studio.client.application.ui.AboutDialogContents;
import org.rstudio.studio.client.application.ui.RTimeoutOptions;
import org.rstudio.studio.client.application.ui.LauncherSessionStatus;
import org.rstudio.studio.client.application.ui.appended.ApplicationEndedPopupPanel;
import org.rstudio.studio.client.application.ui.serializationprogress.ApplicationSerializationProgress;
import org.rstudio.studio.client.common.StudioResources;
import org.rstudio.studio.client.common.Timers;
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
import org.rstudio.studio.client.panmirror.PanmirrorResources;
import org.rstudio.studio.client.panmirror.command.PanmirrorToolbarResources;
import org.rstudio.studio.client.panmirror.dialogs.PanmirrorDialogsResources;
import org.rstudio.studio.client.plumber.PlumberAPISatellite;
import org.rstudio.studio.client.projects.ui.newproject.NewProjectResources;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesDialogResources;
import org.rstudio.studio.client.rmarkdown.RmdOutputSatellite;
import org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.shiny.ShinyApplicationSatellite;
import org.rstudio.studio.client.vcs.VCSApplication;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchResources;
import org.rstudio.studio.client.workbench.exportplot.ExportPlotResources;
import org.rstudio.studio.client.workbench.prefs.views.PreferencesDialog;
import org.rstudio.studio.client.workbench.prefs.views.zotero.ZoteroResources;
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
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.dialogs.VisualModeDialogsResources;
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

   private Command showProgress(Widget progressAction)
   {
      final Label background = new Label();
      ariaLoadingMessage_ = new Label();
      Roles.getAlertRole().set(ariaLoadingMessage_.getElement());
      setVisuallyHidden(ariaLoadingMessage_.getElement());

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
      str.append("<img alt src=\"");
      str.append(progressUrl);
      str.append("\"");
      if (BrowseCap.devicePixelRatio() > 1.0)
         str.append("width=24 height=24");
      str.append("/>");
      final SimplePanel progressPanel = new SimplePanel();
      final Element div = progressPanel.getElement();
      div.setInnerHTML(str.toString());
      div.getStyle().setProperty("textAlign", "center");
      ElementIds.assignElementId(div, ElementIds.LOADING_SPINNER);

      final VerticalPanel statusPanel = new VerticalPanel();
      final Element statusDiv = statusPanel.getElement();
      statusDiv.getStyle().setWidth(100, Style.Unit.PCT);
      statusDiv.getStyle().setMarginTop(200, Style.Unit.PX);
      statusDiv.getStyle().setProperty("textAlign", "center");
      statusDiv.getStyle().setZIndex(1000);

      statusPanel.add(progressPanel);
      statusPanel.add(ariaLoadingMessage_);

      if (progressAction != null)
      {
         statusPanel.add(progressAction);
         statusPanel.setCellHorizontalAlignment(progressAction, VerticalPanel.ALIGN_CENTER);
      }

      if (ApplicationAction.isLauncherSession())
      {
         sessionStatus_ = new LauncherSessionStatus();
         sessionStatus_.setVisible(false);
         statusPanel.add(sessionStatus_);

         // Wait a bit to keep things uncluttered for typical load,
         // then show message so they know things are happening, including
         // a link back to the home page
         showStatusTimer_ = new Timer()
         {
            public void run()
            {
               sessionStatus_.setVisible(true);
               ariaLoadingMessage_.setText(sessionStatus_.getMessage());
            }
         };
         showStatusTimer_.schedule(3000);
      }
      else
      {
         // for regular sessions, give screen-reader users a hint that something is happening
         // if the session is taking time to load
         showStatusTimer_ = new Timer()
         {
            public void run()
            {
               ariaLoadingMessage_.setText("Loading session...");
            }
         };
         showStatusTimer_.schedule(3000);
      }

      rootPanel.add(statusPanel);

      return () ->
      {
         try
         {
            if (showStatusTimer_ != null)
            {
               showStatusTimer_.cancel();
               showStatusTimer_ = null;
            }
            rootPanel.remove(statusPanel);
            rootPanel.remove(background);
         }
         catch (Exception e)
         {
            Debug.log(e.toString());
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
      // if we are loading the main window, and we're not a launcher session,
      // add buttons for bailing out
      String view = getSatelliteView();
      if (StringUtil.isNullOrEmpty(view) && !ApplicationAction.isLauncherSession())
      {
         rTimeoutOptions_ = new RTimeoutOptions();

         final DelayFadeInHelper reloadShowHelper = new DelayFadeInHelper(rTimeoutOptions_, 750, () ->
         {
            // after fade-in, another brief pause so screen readers have time to catch up with
            // new UI state
            Timers.singleShot(1000, () -> ariaLoadingMessage_.setText(rTimeoutOptions_.getMessage()));
         });
         reloadShowHelper.hide();
         Timer t = new Timer()
         {
            @Override
            public void run()
            {
               reloadShowHelper.beginShow();
            }
         };
         t.schedule(30000);
      }

      dismissProgressAnimation_ = showProgress(rTimeoutOptions_);

      final SerializedCommandQueue queue = new SerializedCommandQueue();

      // ensure Ace is loaded up front
      queue.addCommand(continuation -> AceEditor.load(continuation));

      // load the requested page
      queue.addCommand(continuation -> onDelayLoadApplication());

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
      else if (view != null && view.startsWith(
            ShinyApplicationSatellite.NAME_PREFIX))
      {
         ShinyApplicationSatellite satellite =
               new ShinyApplicationSatellite(view);
         satellite.go(RootLayoutPanel.get(), dismissProgressAnimation_);
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
         final ServerRequestCallback<String> connectionStatusCallback =
               new ServerRequestCallback<String>() {
                  @Override
                  public void onResponseReceived(String message)
                  {
                     if (sessionStatus_ != null)
                     {
                        sessionStatus_.setStatus(message);
                     }
                  }
                  @Override
                  public void onError(ServerError error)
                  {
                  }
               };

         RStudioGinjector.INSTANCE.getApplication().go(
               RootLayoutPanel.get(),
               rTimeoutOptions_,
               dismissProgressAnimation_,
               connectionStatusCallback);
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
      PanmirrorToolbarResources.INSTANCE.styles().ensureInjected();

      ProgressDialog.ensureStylesInjected();
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
      PanmirrorResources.INSTANCE.styles().ensureInjected();
      PanmirrorDialogsResources.INSTANCE.styles().ensureInjected();
      ZoteroResources.ensureStylesInjected();
      NewConnectionWizard.ensureStylesInjected();
      SecondaryReposWidget.ensureStylesInjected();
      SecondaryReposDialog.ensureStylesInjected();
      VisualModeDialogsResources.ensureStylesInjected();

      StyleInjector.inject(
            "button::-moz-focus-inner {border:0}");
   }

   /**
    * Make an element visually hidden (aka screen reader only). Don't use our shared
    * function A11y.setVisuallyHidden during boot screen because it relies on styles
    * being injected which aren't available in boot screen.
    */
   private void setVisuallyHidden(Element el)
   {
      // Keep in sync with themeStyles.css visuallyHidden
      el.getStyle().setPosition(Position.ABSOLUTE);
      el.getStyle().setProperty("clip", "rect(0 0 0 0)");
      el.getStyle().setBorderWidth(0, Unit.PX);
      el.getStyle().setWidth(1.0, Unit.PX);
      el.getStyle().setHeight(1.0, Unit.PX);
      el.getStyle().setMargin(-1.0, Unit.PX);
      el.getStyle().setOverflow(Overflow.HIDDEN);
      el.getStyle().setPadding(0.0, Unit.PX);
   }

   public final static String getSatelliteView()
   {
      return Window.Location.getParameter("view");
   }

   private Command dismissProgressAnimation_;
   private RTimeoutOptions rTimeoutOptions_;
   private Timer showStatusTimer_;
   private LauncherSessionStatus sessionStatus_;
   private Label ariaLoadingMessage_;
}
