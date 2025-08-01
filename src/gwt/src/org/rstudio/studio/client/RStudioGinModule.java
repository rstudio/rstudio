/*
 * RStudioGinModule.java
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
package org.rstudio.studio.client;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.assistedinject.GinFactoryModuleBuilder;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.VirtualConsole.PreferencesImpl;
import org.rstudio.core.client.VirtualConsoleFactory;
import org.rstudio.studio.client.application.ApplicationInterrupt;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.ApplicationView;
import org.rstudio.studio.client.application.ApplicationVisibility;
import org.rstudio.studio.client.application.DesktopInfo;
import org.rstudio.studio.client.application.events.FireEvents;
import org.rstudio.core.client.command.ApplicationCommandManager;
import org.rstudio.core.client.command.EditorCommandManager;
import org.rstudio.core.client.command.ShortcutViewer;
import org.rstudio.core.client.command.UserCommandManager;
import org.rstudio.core.client.dom.BrowserEventWorkarounds;
import org.rstudio.core.client.HtmlMessageListener;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.ui.ApplicationWindow;
import org.rstudio.studio.client.application.ui.CodeSearchLauncher;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.DefaultGlobalDisplay;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.compilepdf.model.CompilePdfServerOperations;
import org.rstudio.studio.client.common.console.ConsoleProcess.ConsoleProcessFactory;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.common.debugging.BreakpointManager;
import org.rstudio.studio.client.common.debugging.DebugCommander;
import org.rstudio.studio.client.common.debugging.DebuggingServerOperations;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.dependencies.model.DependencyServerOperations;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.latex.LatexProgramRegistry;
import org.rstudio.studio.client.common.mathjax.MathJaxLoader;
import org.rstudio.studio.client.common.mirrors.DefaultCRANMirror;
import org.rstudio.studio.client.common.mirrors.model.MirrorsServerOperations;
import org.rstudio.studio.client.common.plumber.model.PlumberServerOperations;
import org.rstudio.studio.client.common.r.roxygen.RoxygenServerOperations;
import org.rstudio.studio.client.common.rnw.RnwWeaveRegistry;
import org.rstudio.studio.client.common.rpubs.model.RPubsServerOperations;
import org.rstudio.studio.client.common.rstudioapi.RStudioAPI;
import org.rstudio.studio.client.common.vcs.AskPassManager;
import org.rstudio.studio.client.common.rstudioapi.AskSecretManager;
import org.rstudio.studio.client.common.rstudioapi.model.RStudioAPIServerOperations;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.shiny.model.ShinyServerOperations;
import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.common.synctex.model.SynctexServerOperations;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.SVNServerOperations;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.common.vcs.ignore.Ignore;
import org.rstudio.studio.client.common.vcs.ignore.IgnoreDialog;
import org.rstudio.studio.client.common.repos.model.SecondaryReposServerOperations;
import org.rstudio.studio.client.htmlpreview.HTMLPreview;
import org.rstudio.studio.client.htmlpreview.HTMLPreviewPresenter;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewServerOperations;
import org.rstudio.studio.client.htmlpreview.ui.HTMLPreviewApplicationView;
import org.rstudio.studio.client.htmlpreview.ui.HTMLPreviewApplicationWindow;
import org.rstudio.studio.client.htmlpreview.ui.HTMLPreviewPanel;
import org.rstudio.studio.client.packrat.model.PackratServerOperations;
import org.rstudio.studio.client.palette.CommandPaletteLauncher;
import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocServerOperations;
import org.rstudio.studio.client.panmirror.server.PanmirrorCrossrefServerOperations;
import org.rstudio.studio.client.panmirror.server.PanmirrorDOIServerOperations;
import org.rstudio.studio.client.panmirror.server.PanmirrorDataCiteServerOperations;
import org.rstudio.studio.client.panmirror.server.PanmirrorPubMedServerOperations;
import org.rstudio.studio.client.panmirror.server.PanmirrorXRefServerOperations;
import org.rstudio.studio.client.panmirror.server.PanmirrorZoteroServerOperations;
import org.rstudio.studio.client.pdfviewer.PDFViewer;
import org.rstudio.studio.client.plumber.PlumberAPI;
import org.rstudio.studio.client.plumber.PlumberAPIPresenter;
import org.rstudio.studio.client.plumber.ui.PlumberAPIPanel;
import org.rstudio.studio.client.plumber.ui.PlumberAPIView;
import org.rstudio.studio.client.plumber.ui.PlumberAPIWindow;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.model.ProjectTemplateRegistryProvider;
import org.rstudio.studio.client.projects.model.ProjectTemplateServerOperations;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.quarto.model.QuartoServerOperations;
import org.rstudio.studio.client.renv.model.RenvServerOperations;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.rmarkdown.RmdOutputView;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.ui.RmdOutputPanel;
import org.rstudio.studio.client.rmarkdown.ui.RmdOutputPresenter;
import org.rstudio.studio.client.rmarkdown.ui.RmdOutputWindow;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.model.PlotPublishMRUList;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.rsconnect.ui.RSAccountConnector;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.server.remote.RemoteServer;
import org.rstudio.studio.client.shiny.ShinyApplication;
import org.rstudio.studio.client.shiny.ShinyApplicationPresenter;
import org.rstudio.studio.client.shiny.ui.ShinyApplicationPanel;
import org.rstudio.studio.client.shiny.ui.ShinyApplicationView;
import org.rstudio.studio.client.shiny.ui.ShinyApplicationWindow;
import org.rstudio.studio.client.sql.model.SqlServerOperations;
import org.rstudio.studio.client.vcs.VCSApplicationView;
import org.rstudio.studio.client.vcs.ui.VCSApplicationWindow;
import org.rstudio.studio.client.workbench.ClientStateUpdater;
import org.rstudio.studio.client.workbench.ShowDOMElementIDs;
import org.rstudio.studio.client.workbench.UserInterfaceHighlighter;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.WorkbenchMainView;
import org.rstudio.studio.client.workbench.addins.AddinsServerOperations;
import org.rstudio.studio.client.workbench.codesearch.CodeSearch;
import org.rstudio.studio.client.workbench.codesearch.model.CodeSearchServerOperations;
import org.rstudio.studio.client.workbench.codesearch.ui.CodeSearchWidget;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.copilot.Copilot;
import org.rstudio.studio.client.workbench.copilot.server.CopilotServerOperations;
import org.rstudio.studio.client.workbench.model.MetaServerOperations;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchListsServerOperations;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.PrefsServerOperations;
import org.rstudio.studio.client.workbench.prefs.views.PythonServerOperations;
import org.rstudio.studio.client.workbench.snippets.SnippetServerOperations;
import org.rstudio.studio.client.workbench.ui.PaneManager;
import org.rstudio.studio.client.workbench.ui.WorkbenchScreen;
import org.rstudio.studio.client.workbench.ui.WorkbenchTab;
import org.rstudio.studio.client.workbench.views.buildtools.BuildPresenter;
import org.rstudio.studio.client.workbench.views.buildtools.BuildPane;
import org.rstudio.studio.client.workbench.views.buildtools.BuildTab;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;
import org.rstudio.studio.client.workbench.views.choosefile.ChooseFile;
import org.rstudio.studio.client.workbench.views.choosefile.model.ChooseFileServerOperations;
import org.rstudio.studio.client.workbench.views.connections.ConnectionsPresenter;
import org.rstudio.studio.client.workbench.views.connections.ConnectionsTab;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.ui.ConnectionsPane;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.console.shell.Shell;
import org.rstudio.studio.client.workbench.views.console.shell.ShellPane;
import org.rstudio.studio.client.workbench.views.console.shell.assist.HelpStrategy;
import org.rstudio.studio.client.workbench.views.edit.Edit;
import org.rstudio.studio.client.workbench.views.edit.model.EditServerOperations;
import org.rstudio.studio.client.workbench.views.edit.ui.EditView;
import org.rstudio.studio.client.workbench.views.environment.EnvironmentPane;
import org.rstudio.studio.client.workbench.views.environment.EnvironmentPresenter;
import org.rstudio.studio.client.workbench.views.environment.EnvironmentTab;
import org.rstudio.studio.client.workbench.views.environment.dataimport.DataImportPresenter;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentServerOperations;
import org.rstudio.studio.client.workbench.views.files.Files;
import org.rstudio.studio.client.workbench.views.files.FilesPane;
import org.rstudio.studio.client.workbench.views.files.FilesTab;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.jobs.LauncherJobsPresenter;
import org.rstudio.studio.client.workbench.views.jobs.LauncherJobsTab;
import org.rstudio.studio.client.workbench.views.jobs.view.JobItem;
import org.rstudio.studio.client.workbench.views.jobs.view.JobItemFactory;
import org.rstudio.studio.client.workbench.views.jobs.view.LauncherJobsPane;
import org.rstudio.studio.client.workbench.views.output.data.DataOutputTab;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputPane;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputPresenter;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputTab;
import org.rstudio.studio.client.workbench.views.output.find.model.FindInFilesServerOperations;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintServerOperations;
import org.rstudio.studio.client.workbench.views.output.markers.MarkersOutputPane;
import org.rstudio.studio.client.workbench.views.output.markers.MarkersOutputPresenter;
import org.rstudio.studio.client.workbench.views.output.markers.MarkersOutputTab;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersServerOperations;
import org.rstudio.studio.client.workbench.views.output.renderrmd.RenderRmdOutputTab;
import org.rstudio.studio.client.workbench.views.output.rsconnectdeploy.RSConnectDeployOutputTab;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.SourceCppOutputPane;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.SourceCppOutputPresenter;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.SourceCppOutputTab;
import org.rstudio.studio.client.workbench.views.output.tests.TestsOutputTab;
import org.rstudio.studio.client.workbench.views.help.Help;
import org.rstudio.studio.client.workbench.views.help.HelpPane;
import org.rstudio.studio.client.workbench.views.help.HelpTab;
import org.rstudio.studio.client.workbench.views.help.model.HelpServerOperations;
import org.rstudio.studio.client.workbench.views.help.search.HelpSearch;
import org.rstudio.studio.client.workbench.views.help.search.HelpSearchWidget;
import org.rstudio.studio.client.workbench.views.history.History;
import org.rstudio.studio.client.workbench.views.history.HistoryTab;
import org.rstudio.studio.client.workbench.views.history.model.HistoryServerOperations;
import org.rstudio.studio.client.workbench.views.history.view.HistoryPane;
import org.rstudio.studio.client.workbench.views.jobs.JobProgressPresenter;
import org.rstudio.studio.client.workbench.views.jobs.JobsPresenter;
import org.rstudio.studio.client.workbench.views.jobs.JobsTab;
import org.rstudio.studio.client.workbench.views.jobs.model.JobManager;
import org.rstudio.studio.client.workbench.views.jobs.model.JobsServerOperations;
import org.rstudio.studio.client.workbench.views.jobs.view.JobProgress;
import org.rstudio.studio.client.workbench.views.jobs.view.JobsPane;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPane;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPaneDisplay;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPaneFactory;
import org.rstudio.studio.client.workbench.views.output.compilepdf.CompilePdfOutputTab;
import org.rstudio.studio.client.workbench.views.output.data.DataOutputPane;
import org.rstudio.studio.client.workbench.views.output.data.DataOutputPresenter;
import org.rstudio.studio.client.workbench.views.packages.Packages;
import org.rstudio.studio.client.workbench.views.packages.PackagesPane;
import org.rstudio.studio.client.workbench.views.packages.PackagesTab;
import org.rstudio.studio.client.workbench.views.packages.model.PackageProvidedExtensions;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;
import org.rstudio.studio.client.workbench.views.plots.Plots;
import org.rstudio.studio.client.workbench.views.plots.PlotsPane;
import org.rstudio.studio.client.workbench.views.plots.PlotsTab;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.presentation.Presentation;
import org.rstudio.studio.client.workbench.views.presentation.PresentationPane;
import org.rstudio.studio.client.workbench.views.presentation.PresentationTab;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationServerOperations;
import org.rstudio.studio.client.workbench.views.presentation2.Presentation2;
import org.rstudio.studio.client.workbench.views.presentation2.Presentation2Pane;
import org.rstudio.studio.client.workbench.views.presentation2.Presentation2Tab;
import org.rstudio.studio.client.workbench.views.presentation2.model.Presentation2ServerOperations;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.source.SourcePane;
import org.rstudio.studio.client.workbench.views.source.SourceSatelliteView;
import org.rstudio.studio.client.workbench.views.source.SourceSatelliteWindow;
import org.rstudio.studio.client.workbench.views.source.SourceWindow;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.ObjectExplorerPresenter;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.ObjectExplorerServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.ProfilerPresenter;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkSatelliteView;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkSatelliteWindow;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorCommandDispatcher;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.model.ThemeServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.CppServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.TexServerOperations;
import org.rstudio.studio.client.workbench.views.terminal.TerminalPane;
import org.rstudio.studio.client.workbench.views.terminal.TerminalTab;
import org.rstudio.studio.client.workbench.views.terminal.TerminalTabPresenter;
import org.rstudio.studio.client.workbench.views.tutorial.TutorialPane;
import org.rstudio.studio.client.workbench.views.tutorial.TutorialPresenter;
import org.rstudio.studio.client.workbench.views.tutorial.TutorialServerOperations;
import org.rstudio.studio.client.workbench.views.tutorial.TutorialTab;
import org.rstudio.studio.client.workbench.views.vcs.VCSTab;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.LineTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.LineTableView;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPanel;
import org.rstudio.studio.client.workbench.views.vcs.dialog.HistoryPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.dialog.ReviewPresenterImpl;
import org.rstudio.studio.client.workbench.views.vcs.git.GitPane;
import org.rstudio.studio.client.workbench.views.vcs.git.GitPresenter;
import org.rstudio.studio.client.workbench.views.vcs.git.dialog.GitReviewPanel;
import org.rstudio.studio.client.workbench.views.vcs.git.dialog.GitReviewPresenter;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNPane;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNPresenter;
import org.rstudio.studio.client.workbench.views.vcs.svn.dialog.SVNReviewPanel;
import org.rstudio.studio.client.workbench.views.vcs.svn.dialog.SVNReviewPresenter;
import org.rstudio.studio.client.workbench.views.viewer.ViewerPane;
import org.rstudio.studio.client.workbench.views.viewer.ViewerPresenter;
import org.rstudio.studio.client.workbench.views.viewer.ViewerTab;
import org.rstudio.studio.client.workbench.views.viewer.model.ViewerServerOperations;

public class RStudioGinModule extends AbstractGinModule
{
   @Override
   protected void configure()
   {
      bind(EventBus.class).in(Singleton.class);
      bind(Session.class).in(Singleton.class);
      bind(Projects.class).in(Singleton.class);
      bind(Satellite.class).in(Singleton.class);
      bind(SatelliteManager.class).in(Singleton.class);
      bind(AskPassManager.class).in(Singleton.class);
      bind(ProfilerPresenter.class).in(Singleton.class);
      bind(ObjectExplorerPresenter.class).asEagerSingleton();
      bind(WorkbenchContext.class).asEagerSingleton();
      bind(PaneManager.class).in(Singleton.class);
      bind(DependencyManager.class).asEagerSingleton();
      bind(WorkbenchListManager.class).asEagerSingleton();
      bind(ApplicationQuit.class).asEagerSingleton();
      bind(ApplicationInterrupt.class).asEagerSingleton();
      bind(ApplicationVisibility.class).asEagerSingleton();
      bind(ClientStateUpdater.class).asEagerSingleton();
      bind(ConsoleProcessFactory.class).asEagerSingleton();
      bind(RnwWeaveRegistry.class).asEagerSingleton();
      bind(LatexProgramRegistry.class).asEagerSingleton();
      bind(Commands.class).in(Singleton.class);
      bind(UserInterfaceHighlighter.class).asEagerSingleton();
      bind(ShowDOMElementIDs.class).asEagerSingleton();
      bind(DefaultCRANMirror.class).in(Singleton.class);
      bind(ChooseFile.class).in(Singleton.class);
      bind(ConsoleDispatcher.class).in(Singleton.class);
      bind(FileTypeCommands.class).in(Singleton.class);
      bind(Synctex.class).in(Singleton.class);
      bind(PDFViewer.class).in(Singleton.class);
      bind(HTMLPreview.class).in(Singleton.class);      
      bind(ShinyApplication.class).in(Singleton.class);
      bind(PlumberAPI.class).in(Singleton.class);
      bind(BreakpointManager.class).asEagerSingleton();
      bind(DebugCommander.class).asEagerSingleton();
      bind(ShortcutViewer.class).asEagerSingleton();
      bind(RSConnect.class).asEagerSingleton();
      bind(RmdOutput.class).in(Singleton.class);
      bind(HelpStrategy.class).in(Singleton.class);
      bind(RSAccountConnector.class).in(Singleton.class);
      bind(PlotPublishMRUList.class).asEagerSingleton();
      bind(SourceWindowManager.class).in(Singleton.class);
      bind(SourceWindow.class).in(Singleton.class);
      bind(EditorCommandManager.class).in(Singleton.class);
      bind(UserCommandManager.class).in(Singleton.class);
      bind(ApplicationCommandManager.class).in(Singleton.class);
      bind(CodeSearchLauncher.class).in(Singleton.class);
      bind(AceEditorCommandDispatcher.class).asEagerSingleton();
      bind(DataImportPresenter.class).in(Singleton.class);
      bind(MathJaxLoader.class).asEagerSingleton();
      bind(ProjectTemplateRegistryProvider.class).in(Singleton.class);
      bind(PackageProvidedExtensions.class).asEagerSingleton();
      bind(JavaScriptEventHistory.class).asEagerSingleton();
      bind(JobManager.class).asEagerSingleton();
      bind(HtmlMessageListener.class).asEagerSingleton();
      bind(BrowserEventWorkarounds.class).asEagerSingleton();
      bind(CommandPaletteLauncher.class).asEagerSingleton();

      bind(ApplicationView.class).to(ApplicationWindow.class)
            .in(Singleton.class);
      
      bind(VCSApplicationView.class).to(VCSApplicationWindow.class)
            .in(Singleton.class);
      bind(ReviewPresenter.class).to(ReviewPresenterImpl.class);
      
      bind(HTMLPreviewApplicationView.class).to(HTMLPreviewApplicationWindow.class);
      bind(ShinyApplicationView.class).to(ShinyApplicationWindow.class);
      bind(PlumberAPIView.class).to(PlumberAPIWindow.class);
      bind(RmdOutputView.class).to(RmdOutputWindow.class);
      bind(SourceSatelliteView.class).to(SourceSatelliteWindow.class);
      
      bind(Server.class).to(RemoteServer.class);
      bind(WorkbenchServerOperations.class).to(RemoteServer.class);

      bind(EditingTargetSource.class).to(EditingTargetSource.Impl.class);

      // Bind workbench views
      bindPane(PaneManager.CONSOLE_PANE, ConsolePane.class); // eager loaded
      bind(Source.Display.class).to(SourcePane.class);
      bind(TerminalTabPresenter.Display.class).to(TerminalPane.class);
      bind(History.Display.class).to(HistoryPane.class);
      bind(Files.Display.class).to(FilesPane.class);
      bind(Plots.Display.class).to(PlotsPane.class);
      bind(Packages.Display.class).to(PackagesPane.class);
      bind(Help.Display.class).to(HelpPane.class);
      bind(Edit.Display.class).to(EditView.class);
      bind(GitPresenter.Display.class).to(GitPane.class);
      bind(SVNPresenter.Display.class).to(SVNPane.class);
      bind(TutorialPresenter.Display.class).to(TutorialPane.class);
      bind(BuildPresenter.Display.class).to(BuildPane.class);
      bind(Presentation.Display.class).to(PresentationPane.class);
      bind(Presentation2.Display.class).to(Presentation2Pane.class);
      bind(EnvironmentPresenter.Display.class).to(EnvironmentPane.class);
      bind(ViewerPresenter.Display.class).to(ViewerPane.class);
      bind(ConnectionsPresenter.Display.class).to(ConnectionsPane.class);
      bind(Ignore.Display.class).to(IgnoreDialog.class);
      bind(FindOutputPresenter.Display.class).to(FindOutputPane.class);
      bind(SourceCppOutputPresenter.Display.class).to(SourceCppOutputPane.class);
      bind(MarkersOutputPresenter.Display.class).to(MarkersOutputPane.class);
      bind(JobsPresenter.Display.class).to(JobsPane.class);
      bind(JobProgressPresenter.Display.class).to(JobProgress.class);
      bind(LauncherJobsPresenter.Display.class).to(LauncherJobsPane.class);
      bind(DataOutputPresenter.Display.class).to(DataOutputPane.class);
      bindTab(PaneManager.HISTORY_PANE, HistoryTab.class);
      bindTab(PaneManager.FILES_PANE, FilesTab.class);
      bindTab(PaneManager.PLOTS_PANE, PlotsTab.class);
      bindTab(PaneManager.PACKAGES_PANE, PackagesTab.class);
      bindTab(PaneManager.HELP_PANE, HelpTab.class);
      bindTab(PaneManager.VCS_PANE, VCSTab.class);
      bindTab(PaneManager.BUILD_PANE, BuildTab.class);
      bindTab(PaneManager.PRESENTATION_PANE, PresentationTab.class);
      bindTab(PaneManager.PRESENTATIONS_PANE, Presentation2Tab.class);
      bindTab(PaneManager.ENVIRONMENT_PANE, EnvironmentTab.class);
      bindTab(PaneManager.VIEWER_PANE, ViewerTab.class);
      bindTab(PaneManager.CONNECTIONS_PANE, ConnectionsTab.class);
      bindTab(PaneManager.COMPILE_PDF_PANE, CompilePdfOutputTab.class);
      bindTab(PaneManager.RMARKDOWN_PANE, RenderRmdOutputTab.class);
      bindTab(PaneManager.FIND_PANE, FindOutputTab.class);
      bindTab(PaneManager.SOURCE_CPP_PANE, SourceCppOutputTab.class);
      bindTab(PaneManager.DEPLOY_PANE, RSConnectDeployOutputTab.class);
      bindTab(PaneManager.MARKERS_PANE, MarkersOutputTab.class);
      bindTab(PaneManager.TERMINAL_PANE, TerminalTab.class);
      bindTab(PaneManager.TESTS_PANE, TestsOutputTab.class);
      bindTab(PaneManager.JOBS_PANE, JobsTab.class);
      bindTab(PaneManager.LAUNCHER_PANE, LauncherJobsTab.class);
      bindTab(PaneManager.DATA_OUTPUT_PANE, DataOutputTab.class);
      bindTab(PaneManager.TUTORIAL_PANE, TutorialTab.class);

      bind(Shell.Display.class).to(ShellPane.class);
           
      bind(HelpSearch.Display.class).to(HelpSearchWidget.class);
      bind(CodeSearch.Display.class).to(CodeSearchWidget.class);

      bind(GitReviewPresenter.Display.class).to(GitReviewPanel.class);
      bind(SVNReviewPresenter.Display.class).to(SVNReviewPanel.class);
      bind(LineTablePresenter.Display.class).to(LineTableView.class);
      bind(HistoryPresenter.DisplayBuilder.class).to(
                                                    HistoryPanel.Builder.class);
      
      bind(HTMLPreviewPresenter.Display.class).to(HTMLPreviewPanel.class);
      bind(ShinyApplicationPresenter.Display.class).to(ShinyApplicationPanel.class);
      bind(PlumberAPIPresenter.Display.class).to(PlumberAPIPanel.class);
      bind(RmdOutputPresenter.Display.class).to(RmdOutputPanel.class);
      
      bind(GlobalDisplay.class)
            .to(DefaultGlobalDisplay.class)
            .in(Singleton.class);

      bind(ApplicationServerOperations.class).to(RemoteServer.class);
      bind(ChooseFileServerOperations.class).to(RemoteServer.class);
      bind(CodeToolsServerOperations.class).to(RemoteServer.class);
      bind(ConsoleServerOperations.class).to(RemoteServer.class);
      bind(SourceServerOperations.class).to(RemoteServer.class);
      bind(FilesServerOperations.class).to(RemoteServer.class);
      bind(HistoryServerOperations.class).to(RemoteServer.class);
      bind(PlotsServerOperations.class).to(RemoteServer.class);
      bind(PackagesServerOperations.class).to(RemoteServer.class);
      bind(HelpServerOperations.class).to(RemoteServer.class);
      bind(EditServerOperations.class).to(RemoteServer.class);
      bind(MirrorsServerOperations.class).to(RemoteServer.class);
      bind(VCSServerOperations.class).to(RemoteServer.class);
      bind(GitServerOperations.class).to(RemoteServer.class);
      bind(SVNServerOperations.class).to(RemoteServer.class);
      bind(PrefsServerOperations.class).to(RemoteServer.class);
      bind(ProjectsServerOperations.class).to(RemoteServer.class);
      bind(CodeSearchServerOperations.class).to(RemoteServer.class);
      bind(WorkbenchListsServerOperations.class).to(RemoteServer.class);
      bind(CryptoServerOperations.class).to(RemoteServer.class);
      bind(TexServerOperations.class).to(RemoteServer.class);
      bind(SpellingServerOperations.class).to(RemoteServer.class);
      bind(CompilePdfServerOperations.class).to(RemoteServer.class);
      bind(FindInFilesServerOperations.class).to(RemoteServer.class);
      bind(SynctexServerOperations.class).to(RemoteServer.class);
      bind(HTMLPreviewServerOperations.class).to(RemoteServer.class);
      bind(SqlServerOperations.class).to(RemoteServer.class);
      bind(ShinyServerOperations.class).to(RemoteServer.class);
      bind(PlumberServerOperations.class).to(RemoteServer.class);
      bind(RSConnectServerOperations.class).to(RemoteServer.class);
      bind(RPubsServerOperations.class).to(RemoteServer.class);
      bind(BuildServerOperations.class).to(RemoteServer.class);
      bind(PresentationServerOperations.class).to(RemoteServer.class);
      bind(Presentation2ServerOperations.class).to(RemoteServer.class);
      bind(EnvironmentServerOperations.class).to(RemoteServer.class);
      bind(DebuggingServerOperations.class).to(RemoteServer.class);
      bind(MetaServerOperations.class).to(RemoteServer.class);
      bind(ViewerServerOperations.class).to(RemoteServer.class);
      bind(ConnectionsServerOperations.class).to(RemoteServer.class);
      bind(ProfilerServerOperations.class).to(RemoteServer.class);
      bind(RMarkdownServerOperations.class).to(RemoteServer.class);
      bind(PanmirrorPandocServerOperations.class).to(RemoteServer.class);
      bind(PanmirrorCrossrefServerOperations.class).to(RemoteServer.class);
      bind(PanmirrorDataCiteServerOperations.class).to(RemoteServer.class);
      bind(PanmirrorPubMedServerOperations.class).to(RemoteServer.class);
      bind(PanmirrorXRefServerOperations.class).to(RemoteServer.class);
      bind(PanmirrorDOIServerOperations.class).to(RemoteServer.class);
      bind(PanmirrorZoteroServerOperations.class).to(RemoteServer.class);
      bind(DependencyServerOperations.class).to(RemoteServer.class);
      bind(PackratServerOperations.class).to(RemoteServer.class);
      bind(RenvServerOperations.class).to(RemoteServer.class);
      bind(CppServerOperations.class).to(RemoteServer.class);
      bind(MarkersServerOperations.class).to(RemoteServer.class);
      bind(LintServerOperations.class).to(RemoteServer.class);
      bind(RoxygenServerOperations.class).to(RemoteServer.class);
      bind(SnippetServerOperations.class).to(RemoteServer.class);
      bind(AddinsServerOperations.class).to(RemoteServer.class);
      bind(ProjectTemplateServerOperations.class).to(RemoteServer.class);
      bind(ObjectExplorerServerOperations.class).to(RemoteServer.class);
      bind(JobsServerOperations.class).to(RemoteServer.class);
      bind(DesktopInfo.class).asEagerSingleton();
      bind(SecondaryReposServerOperations.class).to(RemoteServer.class);
      bind(ThemeServerOperations.class).to(RemoteServer.class);
      bind(TutorialServerOperations.class).to(RemoteServer.class);
      bind(PythonServerOperations.class).to(RemoteServer.class);
      bind(QuartoServerOperations.class).to(RemoteServer.class);
      bind(CopilotServerOperations.class).to(RemoteServer.class);

      bind(WorkbenchMainView.class).to(WorkbenchScreen.class);

      bind(DocDisplay.class).to(AceEditor.class);
      
      install(new GinFactoryModuleBuilder()
         .implement(CompileOutputPaneDisplay.class, CompileOutputPane.class)
         .build(CompileOutputPaneFactory.class));

      bind(ChunkWindowManager.class).in(Singleton.class);
      bind(ChunkSatelliteView.class).to(ChunkSatelliteWindow.class);
      bind(RStudioAPI.class).asEagerSingleton();
      bind(RStudioAPIServerOperations.class).to(RemoteServer.class);
      bind(Copilot.class).asEagerSingleton();

      bind(AskSecretManager.class).in(Singleton.class);
      bind(VirtualConsole.Preferences.class).to(PreferencesImpl.class);
      bind(JobItem.Preferences.class).to(JobItem.PreferencesImpl.class);
      install(new GinFactoryModuleBuilder().build(VirtualConsoleFactory.class));
      install(new GinFactoryModuleBuilder().build(JobItemFactory.class));
      bind(FireEvents.class).to(EventBus.class);
   }

   protected <T extends WorkbenchTab> void bindTab(String name, Class<T> clazz)
   {
      bind(WorkbenchTab.class).annotatedWith(Names.named(name)).to(clazz);
   }

   private <T extends Widget> void bindPane(String name, Class<T> clazz)
   {
      bind(Widget.class).annotatedWith(Names.named(name)).to(clazz);
   }
}
