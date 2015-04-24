/*
 * RStudioGinjector.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

import org.rstudio.core.client.command.ShortcutViewer;
import org.rstudio.core.client.files.filedialog.PathBreadcrumbWidget;
import org.rstudio.core.client.widget.CaptionWithHelp;
import org.rstudio.core.client.widget.LocalRepositoriesWidget;
import org.rstudio.studio.client.application.Application;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.ui.ProjectPopupMenu;
import org.rstudio.studio.client.application.ui.impl.DesktopApplicationHeader;
import org.rstudio.studio.client.application.ui.impl.WebApplicationHeader;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.compilepdf.dialog.CompilePdfProgressDialog;
import org.rstudio.studio.client.common.fileexport.FileExport;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.NewFileMenu;
import org.rstudio.studio.client.common.impl.DesktopFileDialogs;
import org.rstudio.studio.client.common.latex.LatexProgramRegistry;
import org.rstudio.studio.client.common.r.roxygen.RoxygenHelper;
import org.rstudio.studio.client.common.rnw.RnwWeaveRegistry;
import org.rstudio.studio.client.common.rnw.RnwWeaveSelectWidget;
import org.rstudio.studio.client.common.rpubs.ui.RPubsUploadDialog;
import org.rstudio.studio.client.common.spelling.SpellChecker;
import org.rstudio.studio.client.common.spelling.ui.SpellingCustomDictionariesWidget;
import org.rstudio.studio.client.dataviewer.DataViewerSatellite;
import org.rstudio.studio.client.htmlpreview.HTMLPreviewApplication;
import org.rstudio.studio.client.notebook.CompileNotebookOptionsDialog;
import org.rstudio.studio.client.projects.ui.newproject.CodeFilesList;
import org.rstudio.studio.client.projects.ui.prefs.ProjectPreferencesPane;
import org.rstudio.studio.client.projects.ui.prefs.buildtools.BuildToolsPackagePanel;
import org.rstudio.studio.client.rmarkdown.RmdOutputSatellite;
import org.rstudio.studio.client.rmarkdown.RmdTemplateDiscovery;
import org.rstudio.studio.client.rmarkdown.ui.RmdOutputFramePane;
import org.rstudio.studio.client.rmarkdown.ui.RmdOutputFrameSatellite;
import org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.shiny.ShinyApplication;
import org.rstudio.studio.client.shiny.ShinyApplicationSatellite;
import org.rstudio.studio.client.shiny.ui.ShinyViewerTypePopupMenu;
import org.rstudio.studio.client.vcs.VCSApplication;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.snippets.SnippetHelper;
import org.rstudio.studio.client.workbench.snippets.ui.EditSnippetsDialog;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester;
import org.rstudio.studio.client.workbench.views.console.shell.assist.HelpStrategy;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager;
import org.rstudio.studio.client.workbench.views.output.lint.LintManager;
import org.rstudio.studio.client.workbench.views.source.DocsMenu;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetCodeExecution;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetCompilePdfHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetCppHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetPresentationHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionRequest;
import org.rstudio.studio.client.workbench.views.source.editors.text.r.SignatureToolTipManager;
import org.rstudio.studio.client.workbench.views.vcs.svn.SVNCommandHandler;
import org.rstudio.studio.client.workbench.views.environment.ClearAllDialog;

@GinModules(RStudioGinModuleOverlay.class)
public interface RStudioGinjector extends Ginjector
{
   void injectMembers(NewFileMenu newFileMenu);
   void injectMembers(DocsMenu docsMenu);
   void injectMembers(DesktopApplicationHeader desktopApplicationHeader);
   void injectMembers(WebApplicationHeader webApplicationHeader);
   void injectMembers(AceEditor aceEditor);
   void injectMembers(DesktopFileDialogs desktopFileDialogs);
   void injectMembers(RCompletionManager rCompletionManager);
   void injectMembers(SVNCommandHandler svnCommandHandler);
   void injectMembers(CaptionWithHelp captionWithHelp);
   void injectMembers(RnwWeaveSelectWidget selectWidget);
   void injectMembers(CompilePdfProgressDialog compilePdfProgressDialog);
   void injectMembers(TextEditingTargetCompilePdfHelper compilePdfHelper);
   void injectMembers(SpellChecker spellChecker);
   void injectMembers(SpellingCustomDictionariesWidget widget);
   void injectMembers(FileExport fileExport);
   void injectMembers(RPubsUploadDialog uploadDialog);
   void injectMembers(CompileNotebookOptionsDialog notebookOptionsDialog);
   void injectMembers(ProjectPreferencesPane projectPrefsPane);
   void injectMembers(BuildToolsPackagePanel buildToolsPackagePanel);
   void injectMembers(CodeFilesList codeFilesList);
   void injectMembers(ProjectPopupMenu projectPopupMenu);
   void injectMembers(ClearAllDialog clearAllDialog);
   void injectMembers(TextEditingTargetPresentationHelper presHelper);
   void injectMembers(TextEditingTargetRMarkdownHelper rmarkdownHelper);
   void injectMembers(TextEditingTargetCppHelper cppHelper);
   void injectMembers(EditingTargetCodeExecution codeExecution);
   void injectMembers(LocalRepositoriesWidget localRepositoriesWidget);
   void injectMembers(CppCompletionRequest request);
   void injectMembers(CppCompletionManager completionManager);
   void injectMembers(SignatureToolTipManager manager);
   void injectMembers(PathBreadcrumbWidget pathBreadcrumbWidget);
   void injectMembers(LintManager manager);
   void injectMembers(CompletionRequester requester);
   void injectMembers(EditSnippetsDialog dialog);
   void injectMembers(RoxygenHelper helper);
   void injectMembers(SnippetHelper helper);
   void injectMembers(RSConnectPublishButton publishButton);
   void injectMembers(RSConnectDeploy deploy);
   
   public static final RStudioGinjector INSTANCE = GWT.create(RStudioGinjector.class);

   Application getApplication() ;
   VCSApplication getVCSApplication();
   HTMLPreviewApplication getHTMLPreviewApplication();
   ShinyApplicationSatellite getShinyApplicationSatellite();
   ShinyApplication getShinyApplication();
   ShinyViewerTypePopupMenu getShinyViewerTypePopupMenu();
   RmdOutputSatellite getRmdOutputSatellite();
   RmdOutputFramePane getRmdOutputFramePane();
   RmdOutputFrameSatellite getRmdOutputFrameSatellite();
   DataViewerSatellite getDataViewerSatellite();
   EventBus getEventBus();
   GlobalDisplay getGlobalDisplay();
   RemoteFileSystemContext getRemoteFileSystemContext();
   FileDialogs getFileDialogs();
   FileTypeRegistry getFileTypeRegistry();
   RnwWeaveRegistry getRnwWeaveRegistry();
   LatexProgramRegistry getLatexProgramRegistry();
   RmdTemplateDiscovery getRmdTemplateDiscovery();
   Commands getCommands();
   UIPrefs getUIPrefs();
   Session getSession();
   HelpStrategy getHelpStrategy();
   ShortcutViewer getShortcutViewer();
}
