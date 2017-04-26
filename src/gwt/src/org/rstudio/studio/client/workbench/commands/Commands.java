/*
 * Commands.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.commands;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBundle;
import org.rstudio.core.client.command.MenuCallback;

public abstract class
      Commands extends CommandBundle
{
   public abstract void mainMenu(MenuCallback callback);

   // Workbench
   public abstract AppCommand setWorkingDir();
   public abstract AppCommand switchFocusSourceConsole();
   
   // Source
   public abstract AppCommand reformatCode();
   public abstract AppCommand newSourceDoc();
   public abstract AppCommand newRNotebook();
   public abstract AppCommand newTextDoc();
   public abstract AppCommand newCppDoc();
   public abstract AppCommand newSweaveDoc();
   public abstract AppCommand newRMarkdownDoc();
   public abstract AppCommand newRShinyApp();
   public abstract AppCommand newRHTMLDoc();
   public abstract AppCommand newRDocumentationDoc();
   public abstract AppCommand newRPresentationDoc();
   public abstract AppCommand openSourceDoc();
   public abstract AppCommand reopenSourceDocWithEncoding();
   public abstract AppCommand saveSourceDoc();
   public abstract AppCommand saveSourceDocAs();
   public abstract AppCommand saveSourceDocWithEncoding();
   public abstract AppCommand saveAllSourceDocs();
   public abstract AppCommand closeSourceDoc();
   public abstract AppCommand closeOtherSourceDocs();
   public abstract AppCommand closeAllSourceDocs();
   public abstract AppCommand executeAllCode();
   public abstract AppCommand sourceFile();
   public abstract AppCommand sourceActiveDocument();
   public abstract AppCommand sourceActiveDocumentWithEcho();
   public abstract AppCommand executeCode();
   public abstract AppCommand executeCodeWithoutMovingCursor();
   public abstract AppCommand executeCodeWithoutFocus();
   public abstract AppCommand executeToCurrentLine();
   public abstract AppCommand executeFromCurrentLine();
   public abstract AppCommand executeCurrentFunction();
   public abstract AppCommand executeCurrentSection();
   public abstract AppCommand executeLastCode();
   public abstract AppCommand executeCurrentLine();
   public abstract AppCommand executeCurrentStatement();
   public abstract AppCommand executeCurrentParagraph();
   public abstract AppCommand insertChunk();
   public abstract AppCommand insertChunkR();
   public abstract AppCommand insertChunkBash();
   public abstract AppCommand insertChunkPython();
   public abstract AppCommand insertChunkRCPP();
   public abstract AppCommand insertChunkStan(); 
   public abstract AppCommand insertChunkSQL();
   public abstract AppCommand switchToChunkR();
   public abstract AppCommand switchToChunkBash();
   public abstract AppCommand switchToChunkPython();
   public abstract AppCommand switchToChunkRCPP();
   public abstract AppCommand switchToChunkStan(); 
   public abstract AppCommand switchToChunkSQL();
   public abstract AppCommand insertSection();
   public abstract AppCommand executePreviousChunks();
   public abstract AppCommand executeSubsequentChunks();
   public abstract AppCommand executeCurrentChunk();
   public abstract AppCommand executeNextChunk();
   public abstract AppCommand executeSetupChunk();
   public abstract AppCommand goToHelp();
   public abstract AppCommand goToFunctionDefinition();
   public abstract AppCommand sourceNavigateBack();
   public abstract AppCommand sourceNavigateForward();
   public abstract AppCommand markdownHelp();
   public abstract AppCommand openRStudioIDECheatSheet();
   public abstract AppCommand openDataVisualizationCheatSheet();
   public abstract AppCommand openDataImportCheatSheet();
   public abstract AppCommand openPackageDevelopmentCheatSheet();
   public abstract AppCommand openDataWranglingCheatSheet();
   public abstract AppCommand openDataTransformationCheatSheet();
   public abstract AppCommand openRMarkdownCheatSheet();
   public abstract AppCommand openRMarkdownReferenceGuide();
   public abstract AppCommand openShinyCheatSheet();
   public abstract AppCommand openRoxygenQuickReference();
   public abstract AppCommand openSparklyrCheatSheet();
   public abstract AppCommand knitDocument();
   public abstract AppCommand previewHTML();
   public abstract AppCommand publishHTML();
   public abstract AppCommand compilePDF();
   public abstract AppCommand compileNotebook();
   public abstract AppCommand synctexSearch();
   public abstract AppCommand activateSource();
   public abstract AppCommand layoutZoomSource();
   public abstract AppCommand printSourceDoc();
   public abstract AppCommand vcsFileLog();
   public abstract AppCommand vcsFileDiff();
   public abstract AppCommand vcsFileRevert();
   public abstract AppCommand popoutDoc();
   public abstract AppCommand returnDocToMain();
   public abstract AppCommand quickAddNext();
   public abstract AppCommand findReplace();
   public abstract AppCommand findNext();
   public abstract AppCommand findPrevious();
   public abstract AppCommand findSelectAll();
   public abstract AppCommand findFromSelection();
   public abstract AppCommand findAll();
   public abstract AppCommand replaceAndFind();
   public abstract AppCommand findInFiles();
   public abstract AppCommand fold();
   public abstract AppCommand unfold();
   public abstract AppCommand foldAll();
   public abstract AppCommand unfoldAll();
   public abstract AppCommand jumpToMatching();
   public abstract AppCommand selectToMatching();
   public abstract AppCommand expandToMatching();
   public abstract AppCommand addCursorAbove();
   public abstract AppCommand addCursorBelow();
   public abstract AppCommand splitIntoLines();
   public abstract AppCommand toggleDocumentOutline();
   public abstract AppCommand expandSelection();
   public abstract AppCommand shrinkSelection();
   public abstract AppCommand goToNextSection();
   public abstract AppCommand goToPrevSection();
   public abstract AppCommand goToNextChunk();
   public abstract AppCommand goToPrevChunk();
   public abstract AppCommand goToStartOfCurrentScope();
   public abstract AppCommand goToEndOfCurrentScope();
   public abstract AppCommand expandRaggedSelection();
   public abstract AppCommand extractFunction();
   public abstract AppCommand extractLocalVariable();
   public abstract AppCommand commentUncomment();
   public abstract AppCommand reindent();
   public abstract AppCommand reflowComment();
   public abstract AppCommand setWorkingDirToActiveDoc();
   public abstract AppCommand codeCompletion();
   public abstract AppCommand findUsages();
   public abstract AppCommand editRmdFormatOptions();
   public abstract AppCommand knitWithParameters();
   public abstract AppCommand clearKnitrCache();
   public abstract AppCommand clearPrerenderedOutput();
   public abstract AppCommand notebookExpandAllOutput();
   public abstract AppCommand notebookCollapseAllOutput();
   public abstract AppCommand notebookClearOutput();
   public abstract AppCommand notebookClearAllOutput();
   public abstract AppCommand notebookToggleExpansion();
   public abstract AppCommand renameInScope();
   public abstract AppCommand insertRoxygenSkeleton();
   public abstract AppCommand insertSnippet();
   public abstract AppCommand yankRegion();
   public abstract AppCommand yankBeforeCursor();
   public abstract AppCommand yankAfterCursor();
   public abstract AppCommand pasteLastYank();
   public abstract AppCommand insertAssignmentOperator();
   public abstract AppCommand insertPipeOperator();
 
   // Projects
   public abstract AppCommand newProject();
   public abstract AppCommand newProjectFromTemplate();
   public abstract AppCommand openProject();
   public abstract AppCommand openProjectInNewWindow();
   public abstract AppCommand shareProject();
   public abstract AppCommand openSharedProject();
   public abstract AppCommand projectMru0();
   public abstract AppCommand projectMru1();
   public abstract AppCommand projectMru2();
   public abstract AppCommand projectMru3();
   public abstract AppCommand projectMru4();
   public abstract AppCommand projectMru5();
   public abstract AppCommand projectMru6();
   public abstract AppCommand projectMru7();
   public abstract AppCommand projectMru8();
   public abstract AppCommand projectMru9();
   public abstract AppCommand projectMru10();
   public abstract AppCommand projectMru11();
   public abstract AppCommand projectMru12();
   public abstract AppCommand projectMru13();
   public abstract AppCommand projectMru14();
   public abstract AppCommand clearRecentProjects();
   public abstract AppCommand closeProject();
   public abstract AppCommand projectOptions();
   public abstract AppCommand projectSweaveOptions();
   public abstract AppCommand setWorkingDirToProjectDir();

   // Console
   public abstract AppCommand consoleClear();
   public abstract AppCommand interruptR();
   public abstract AppCommand restartR();
   public abstract AppCommand restartRClearOutput();
   public abstract AppCommand restartRRunAllChunks();
   public abstract AppCommand terminateR();
   public abstract AppCommand activateConsole();
   public abstract AppCommand activateConsolePane();
   public abstract AppCommand layoutZoomConsole();
   public abstract AppCommand layoutZoomConsolePane();
   public abstract AppCommand activateConsolePanePane();

   // Files
   public abstract AppCommand newFolder();
   public abstract AppCommand uploadFile();
   public abstract AppCommand copyFile();
   public abstract AppCommand copyFileTo();
   public abstract AppCommand moveFiles();
   public abstract AppCommand exportFiles();
   public abstract AppCommand renameFile();
   public abstract AppCommand deleteFiles();
   public abstract AppCommand refreshFiles();
   public abstract AppCommand activateFiles();
   public abstract AppCommand layoutZoomFiles();
   public abstract AppCommand goToWorkingDir();
   public abstract AppCommand setAsWorkingDir();
   public abstract AppCommand setWorkingDirToFilesPane();
   public abstract AppCommand showFolder();
 
   // VCS
   public abstract AppCommand vcsDiff();
   public abstract AppCommand vcsCommit();
   public abstract AppCommand vcsRevert();
   public abstract AppCommand vcsViewOnGitHub();
   public abstract AppCommand vcsBlameOnGitHub();
   public abstract AppCommand vcsShowHistory();
   public abstract AppCommand vcsRefresh();
   public abstract AppCommand vcsRefreshNoError();
   public abstract AppCommand vcsOpen();
   public abstract AppCommand vcsIgnore();
   public abstract AppCommand vcsPull();
   public abstract AppCommand vcsPush();
   public abstract AppCommand vcsCleanup();
   public abstract AppCommand vcsAddFiles();
   public abstract AppCommand vcsRemoveFiles();
   public abstract AppCommand activateVcs();
   public abstract AppCommand layoutZoomVcs();
   public abstract AppCommand vcsResolve();
   
   // PDF
   public abstract AppCommand showPdfExternal();
   
   // HTML preview
   public abstract AppCommand openHtmlExternal();
   public abstract AppCommand saveHtmlPreviewAsLocalFile();
   public abstract AppCommand saveHtmlPreviewAs();
   public abstract AppCommand refreshHtmlPreview();
   public abstract AppCommand showHtmlPreviewLog();
   
   // Presentation
   public abstract AppCommand refreshPresentation();
   public abstract AppCommand presentationFullscreen();
   public abstract AppCommand presentationHome();
   public abstract AppCommand presentationNext();
   public abstract AppCommand presentationPrev();
   public abstract AppCommand presentationEdit();
   public abstract AppCommand presentationViewInBrowser();
   public abstract AppCommand presentationSaveAsStandalone();
   public abstract AppCommand activatePresentation();
   public abstract AppCommand tutorialFeedback();
   public abstract AppCommand clearPresentationCache();
   
   // View
   public abstract AppCommand showToolbar();
   public abstract AppCommand hideToolbar();
   public abstract AppCommand toggleToolbar();
   public abstract AppCommand zoomActualSize();
   public abstract AppCommand zoomIn();
   public abstract AppCommand zoomOut();
   public abstract AppCommand jumpTo();
   public abstract AppCommand goToFileFunction();
   public abstract AppCommand switchToTab();
   public abstract AppCommand previousTab();
   public abstract AppCommand nextTab();
   public abstract AppCommand firstTab();
   public abstract AppCommand lastTab();
   public abstract AppCommand goToLine();
   public abstract AppCommand toggleFullScreen();
   public abstract AppCommand moveTabLeft();
   public abstract AppCommand moveTabRight();
   public abstract AppCommand moveTabToFirst();
   public abstract AppCommand moveTabToLast();

   // History
   public abstract AppCommand historySendToSource();
   public abstract AppCommand historySendToConsole();
   public abstract AppCommand searchHistory();
   public abstract AppCommand loadHistory();
   public abstract AppCommand saveHistory();
   public abstract AppCommand historyRemoveEntries();
   public abstract AppCommand clearHistory();
   public abstract AppCommand historyDismissResults();
   public abstract AppCommand historyShowContext();
   public abstract AppCommand historyDismissContext();
   public abstract AppCommand activateHistory();
   public abstract AppCommand layoutZoomHistory();

   // Connections
   public abstract AppCommand activateConnections();
   public abstract AppCommand layoutZoomConnections();
   
   // Workspace
   public abstract AppCommand clearWorkspace();
   public abstract AppCommand refreshWorkspace();
   public abstract AppCommand saveWorkspace();
   public abstract AppCommand loadWorkspace();
   public abstract AppCommand importDatasetFromFile();
   public abstract AppCommand importDatasetFromURL();
   public abstract AppCommand importDatasetFromCsv();
   public abstract AppCommand importDatasetFromCsvUsingReadr();
   public abstract AppCommand importDatasetFromCsvUsingBase();
   public abstract AppCommand importDatasetFromSAV();
   public abstract AppCommand importDatasetFromSAS();
   public abstract AppCommand importDatasetFromStata();
   public abstract AppCommand importDatasetFromXLS();
   public abstract AppCommand importDatasetFromXML();
   public abstract AppCommand importDatasetFromJSON();
   public abstract AppCommand importDatasetFromJDBC();
   public abstract AppCommand importDatasetFromODBC();
   public abstract AppCommand importDatasetFromMongo();

   // Environment
   public abstract AppCommand activateEnvironment();
   public abstract AppCommand layoutZoomEnvironment();
   public abstract AppCommand refreshEnvironment();
 
   // Plots
   public abstract AppCommand nextPlot();
   public abstract AppCommand previousPlot();
   public abstract AppCommand savePlotAsImage();
   public abstract AppCommand savePlotAsPdf();
   public abstract AppCommand copyPlotToClipboard();
   public abstract AppCommand zoomPlot();
   public abstract AppCommand removePlot();
   public abstract AppCommand clearPlots();
   public abstract AppCommand refreshPlot();
   public abstract AppCommand activatePlots();
   public abstract AppCommand layoutZoomPlots();
   public abstract AppCommand showManipulator();

   // Packages
   public abstract AppCommand installPackage();
   public abstract AppCommand updatePackages();
   public abstract AppCommand refreshPackages();
   public abstract AppCommand activatePackages();
   public abstract AppCommand layoutZoomPackages();
   
   // // packrat
   public abstract AppCommand packratBootstrap();
   public abstract AppCommand packratOptions();
   public abstract AppCommand packratBundle();
   public abstract AppCommand packratHelp();
   public abstract AppCommand packratClean();

   // Version control
   public abstract AppCommand versionControlHelp();
   public abstract AppCommand versionControlShowRsaKey();
   public abstract AppCommand versionControlProjectSetup();
   
   // Profiler
   public abstract AppCommand showProfiler();
   public abstract AppCommand startProfiler();
   public abstract AppCommand stopProfiler();
   public abstract AppCommand profileCode();
   public abstract AppCommand profileCodeWithoutFocus();
   public abstract AppCommand saveProfileAs();
   public abstract AppCommand openProfile();
   public abstract AppCommand profileHelp();
   public abstract AppCommand gotoProfileSource();
   
   // Tools
   public abstract AppCommand showShellDialog();
   public abstract AppCommand macPreferences();
   public abstract AppCommand showOptions();
   public abstract AppCommand modifyKeyboardShortcuts();
   
   // Terminal
   public abstract AppCommand newTerminal();
   public abstract AppCommand activateTerminal();
   public abstract AppCommand renameTerminal();
   public abstract AppCommand closeTerminal();
   public abstract AppCommand clearTerminalScrollbackBuffer();
   public abstract AppCommand previousTerminal();
   public abstract AppCommand nextTerminal();
   public abstract AppCommand showTerminalInfo();
    
   // Help
   public abstract AppCommand helpBack();
   public abstract AppCommand helpForward();
   public abstract AppCommand helpHome();
   public abstract AppCommand printHelp();
   public abstract AppCommand clearHelpHistory();
   public abstract AppCommand helpPopout();
   public abstract AppCommand refreshHelp();
   public abstract AppCommand raiseException();
   public abstract AppCommand raiseException2();
   public abstract AppCommand activateHelp();
   public abstract AppCommand layoutZoomHelp();
   public abstract AppCommand showAboutDialog();
   public abstract AppCommand checkForUpdates();
   public abstract AppCommand helpUsingRStudio();
   public abstract AppCommand helpKeyboardShortcuts();
   public abstract AppCommand showRequestLog();
   public abstract AppCommand logFocusedElement();
   public abstract AppCommand debugDumpContents();
   public abstract AppCommand debugImportDump();
   public abstract AppCommand refreshSuperDevMode();
   public abstract AppCommand viewShortcuts();
   
   // Viewer
   public abstract AppCommand activateViewer();
   public abstract AppCommand layoutZoomViewer();
   public abstract AppCommand viewerPopout();
   public abstract AppCommand viewerBack(); 
   public abstract AppCommand viewerForward();
   public abstract AppCommand viewerZoom();
   public abstract AppCommand viewerRefresh();
   public abstract AppCommand viewerSaveAllAndRefresh();
   public abstract AppCommand viewerStop();
   public abstract AppCommand viewerClear();
   public abstract AppCommand viewerClearAll();
   public abstract AppCommand viewerSaveAsImage();
   public abstract AppCommand viewerSaveAsWebPage();
   public abstract AppCommand viewerCopyToClipboard();

   // Application
   public abstract AppCommand newSession();
   public abstract AppCommand suspendSession();
   public abstract AppCommand quitSession();
   public abstract AppCommand updateCredentials();
   public abstract AppCommand diagnosticsReport();
   public abstract AppCommand showLogFiles();
   public abstract AppCommand rstudioSupport();
   public abstract AppCommand rstudioAgreement();

   public abstract AppCommand showWarningBar();
 
   // Build
   public abstract AppCommand buildAll();
   public abstract AppCommand devtoolsLoadAll();
   public abstract AppCommand rebuildAll();
   public abstract AppCommand cleanAll();
   public abstract AppCommand buildSourcePackage();
   public abstract AppCommand buildBinaryPackage();
   public abstract AppCommand roxygenizePackage();
   public abstract AppCommand checkPackage();
   public abstract AppCommand testPackage();
   public abstract AppCommand stopBuild();
   public abstract AppCommand buildToolsProjectSetup();
   public abstract AppCommand activateBuild();
   public abstract AppCommand layoutZoomBuild();
   
   // Connections
   public abstract AppCommand newConnection();
   public abstract AppCommand removeConnection();
   public abstract AppCommand disconnectConnection();
   public abstract AppCommand refreshConnection();
   
   // Clipboard placeholders
   public abstract AppCommand undoDummy();
   public abstract AppCommand redoDummy();
   public abstract AppCommand cutDummy();
   public abstract AppCommand copyDummy();
   public abstract AppCommand pasteDummy();

   public abstract AppCommand mru0();
   public abstract AppCommand mru1();
   public abstract AppCommand mru2();
   public abstract AppCommand mru3();
   public abstract AppCommand mru4();
   public abstract AppCommand mru5();
   public abstract AppCommand mru6();
   public abstract AppCommand mru7();
   public abstract AppCommand mru8();
   public abstract AppCommand mru9();
   public abstract AppCommand mru10();
   public abstract AppCommand mru11();
   public abstract AppCommand mru12();
   public abstract AppCommand mru13();
   public abstract AppCommand mru14();
   public abstract AppCommand clearRecentFiles();

   // Debugging
   public abstract AppCommand debugBreakpoint();
   public abstract AppCommand debugClearBreakpoints();
   public abstract AppCommand debugContinue();
   public abstract AppCommand debugStop();
   public abstract AppCommand debugStep();
   public abstract AppCommand debugStepInto();
   public abstract AppCommand debugFinish();
   public abstract AppCommand debugHelp();   
   public abstract AppCommand errorsMessage();
   public abstract AppCommand errorsTraceback();
   public abstract AppCommand errorsBreak();
   public abstract AppCommand showDiagnosticsActiveDocument();
   public abstract AppCommand showDiagnosticsProject();
   
   // Shiny IDE features
   public abstract AppCommand reloadShinyApp();
   public abstract AppCommand shinyRunInPane();
   public abstract AppCommand shinyRunInViewer();
   public abstract AppCommand shinyRunInBrowser();
   
   // RSConnect connectivity
   public abstract AppCommand rsconnectDeploy();
   public abstract AppCommand rsconnectConfigure();
   public abstract AppCommand rsconnectManageAccounts();
   
   // Addins
   public abstract AppCommand addinsMru0();
   public abstract AppCommand addinsMru1();
   public abstract AppCommand addinsMru2();
   public abstract AppCommand addinsMru3();
   public abstract AppCommand addinsMru4();
   public abstract AppCommand addinsMru5();
   public abstract AppCommand addinsMru6();
   public abstract AppCommand addinsMru7();
   public abstract AppCommand addinsMru8();
   public abstract AppCommand addinsMru9();
   public abstract AppCommand addinsMru10();
   public abstract AppCommand addinsMru11();
   public abstract AppCommand addinsMru12();
   public abstract AppCommand addinsMru13();
   public abstract AppCommand addinsMru14();
   public abstract AppCommand clearAddinsMruList();
   public abstract AppCommand browseAddins();

   // Other
   public abstract AppCommand checkSpelling();   
   public abstract AppCommand layoutZoomCurrentPane();
   public abstract AppCommand layoutEndZoom();
   public abstract AppCommand layoutConsoleOnLeft();
   public abstract AppCommand layoutConsoleOnRight();
   public abstract AppCommand paneLayout();
   public abstract AppCommand maximizeConsole();
   public abstract AppCommand toggleEditorTokenInfo();
   
   public static final String KEYBINDINGS_PATH =
         "~/.R/keybindings/rstudio_commands.json";
}
