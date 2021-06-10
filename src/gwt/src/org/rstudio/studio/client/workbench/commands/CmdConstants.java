package org.rstudio.studio.client.workbench.commands;
import com.google.gwt.i18n.client.Constants;

public interface CmdConstants extends Constants {
    // setWorkingDirToProjectDir
    @DefaultStringValue("Set Working Directory to Project Directory")
    String setWorkingDirToProjectDirLabel();
    @DefaultStringValue("")
    String setWorkingDirToProjectDirButtonLabel();
    @DefaultStringValue("To _Project Directory")
    String setWorkingDirToProjectDirMenuLabel();
    @DefaultStringValue("Change working directory to project root directory")
    String setWorkingDirToProjectDirDesc();
    
    // setWorkingDirToActiveDoc
    @DefaultStringValue("Set Working Directory to Current Document's Directory")
    String setWorkingDirToActiveDocLabel();
    @DefaultStringValue("")
    String setWorkingDirToActiveDocButtonLabel();
    @DefaultStringValue("To _Source File Location")
    String setWorkingDirToActiveDocMenuLabel();
    @DefaultStringValue("Change working directory to path of active document")
    String setWorkingDirToActiveDocDesc();
    
    // setWorkingDirToFilesPane
    @DefaultStringValue("Set Working Directory to Directory in Files Pane")
    String setWorkingDirToFilesPaneLabel();
    @DefaultStringValue("")
    String setWorkingDirToFilesPaneButtonLabel();
    @DefaultStringValue("To _Files Pane Location")
    String setWorkingDirToFilesPaneMenuLabel();
    @DefaultStringValue("Change working directory to location of Files pane")
    String setWorkingDirToFilesPaneDesc();
    
    // setWorkingDir
    @DefaultStringValue("Set Working Directory...")
    String setWorkingDirLabel();
    @DefaultStringValue("")
    String setWorkingDirButtonLabel();
    @DefaultStringValue("_Choose Directory...")
    String setWorkingDirMenuLabel();
    @DefaultStringValue("Select and change to a new working directory")
    String setWorkingDirDesc();
    
    // newSourceDoc
    @DefaultStringValue("Create a New R Script")
    String newSourceDocLabel();
    @DefaultStringValue("")
    String newSourceDocButtonLabel();
    @DefaultStringValue("_R Script")
    String newSourceDocMenuLabel();
    @DefaultStringValue("Create a new R script")
    String newSourceDocDesc();
    
    // newRNotebook
    @DefaultStringValue("R _Notebook")
    String newRNotebookMenuLabel();
    @DefaultStringValue("Create a new R Markdown notebook")
    String newRNotebookDesc();
    
    // newTextDoc
    @DefaultStringValue("_Text File")
    String newTextDocMenuLabel();
    @DefaultStringValue("Create a new text file")
    String newTextDocDesc();
    
    // newCDoc
    @DefaultStringValue("_C File")
    String newCDocMenuLabel();
    @DefaultStringValue("Create a new C file")
    String newCDocDesc();
    
    // newCppDoc
    @DefaultStringValue("_C++ File")
    String newCppDocMenuLabel();
    @DefaultStringValue("Create a new C++ file")
    String newCppDocDesc();
    
    // newHeaderDoc
    @DefaultStringValue("_Header File")
    String newHeaderDocMenuLabel();
    @DefaultStringValue("Create a new header file")
    String newHeaderDocDesc();
    
    // newMarkdownDoc
    @DefaultStringValue("_Markdown File")
    String newMarkdownDocMenuLabel();
    @DefaultStringValue("Create a new Markdown document")
    String newMarkdownDocDesc();
    
    // newPythonDoc
    @DefaultStringValue("_Python Script")
    String newPythonDocMenuLabel();
    @DefaultStringValue("Create a new Python script")
    String newPythonDocDesc();
    
    // newShellDoc
    @DefaultStringValue("_Shell Script")
    String newShellDocMenuLabel();
    @DefaultStringValue("Create a new shell script")
    String newShellDocDesc();
    
    // newStanDoc
    @DefaultStringValue("_Stan File")
    String newStanDocMenuLabel();
    @DefaultStringValue("Create a new Stan program")
    String newStanDocDesc();
    
    // newHtmlDoc
    @DefaultStringValue("_HTML File")
    String newHtmlDocMenuLabel();
    @DefaultStringValue("Create a new HTML file")
    String newHtmlDocDesc();
    
    // newJavaScriptDoc
    @DefaultStringValue("_JavaScript File")
    String newJavaScriptDocMenuLabel();
    @DefaultStringValue("Create a new JavaScript file")
    String newJavaScriptDocDesc();
    
    // newCssDoc
    @DefaultStringValue("_CSS File")
    String newCssDocMenuLabel();
    @DefaultStringValue("Create a new CSS file")
    String newCssDocDesc();
    
    // newD3Doc
    @DefaultStringValue("_D3 Script")
    String newD3DocMenuLabel();
    @DefaultStringValue("Create a new D3 Script")
    String newD3DocDesc();
    
    // newRPlumberDoc
    @DefaultStringValue("Plumber _API...")
    String newRPlumberDocMenuLabel();
    @DefaultStringValue("Create a new Plumber API")
    String newRPlumberDocDesc();
    
    // rcppHelp
    @DefaultStringValue("Help on using Rcpp")
    String rcppHelpDesc();
    
    // printCppCompletions
    @DefaultStringValue("Print C++ Completions")
    String printCppCompletionsDesc();
    
    // newSweaveDoc
    @DefaultStringValue("R _Sweave")
    String newSweaveDocMenuLabel();
    @DefaultStringValue("Create a new R Sweave document")
    String newSweaveDocDesc();
    
    // newRMarkdownDoc
    @DefaultStringValue("R _Markdown...")
    String newRMarkdownDocMenuLabel();
    @DefaultStringValue("Create a new R Markdown document")
    String newRMarkdownDocDesc();
    
    // newRShinyApp
    @DefaultStringValue("Shiny _Web App...")
    String newRShinyAppMenuLabel();
    @DefaultStringValue("Create a new Shiny web application")
    String newRShinyAppDesc();
    
    // newRHTMLDoc
    @DefaultStringValue("R _HTML")
    String newRHTMLDocMenuLabel();
    @DefaultStringValue("Create a new R HTML document")
    String newRHTMLDocDesc();
    
    // newRPresentationDoc
    @DefaultStringValue("R _Presentation")
    String newRPresentationDocMenuLabel();
    @DefaultStringValue("Create a new R presentation")
    String newRPresentationDocDesc();
    
    // newRDocumentationDoc
    @DefaultStringValue("R Doc_umentation...")
    String newRDocumentationDocMenuLabel();
    @DefaultStringValue("Create a new Rd documentation file")
    String newRDocumentationDocDesc();
    
    // newSqlDoc
    @DefaultStringValue("S_QL Script")
    String newSqlDocMenuLabel();
    @DefaultStringValue("Create a new SQL script")
    String newSqlDocDesc();
    
    // openSourceDoc
    @DefaultStringValue("Open File...")
    String openSourceDocLabel();
    @DefaultStringValue("")
    String openSourceDocButtonLabel();
    @DefaultStringValue("_Open File...")
    String openSourceDocMenuLabel();
    @DefaultStringValue("Open an existing file")
    String openSourceDocDesc();
    
    // openSourceDocNewColumn
    @DefaultStringValue("Open File in New Column...")
    String openSourceDocNewColumnLabel();
    @DefaultStringValue("")
    String openSourceDocNewColumnButtonLabel();
    @DefaultStringValue("Open File in New C_olumn...")
    String openSourceDocNewColumnMenuLabel();
    @DefaultStringValue("Open an existing file in a new column")
    String openSourceDocNewColumnDesc();
    
    // reopenSourceDocWithEncoding
    @DefaultStringValue("Reopen Current Document with Encoding...")
    String reopenSourceDocWithEncodingLabel();
    @DefaultStringValue("")
    String reopenSourceDocWithEncodingButtonLabel();
    @DefaultStringValue("Reopen with _Encoding...")
    String reopenSourceDocWithEncodingMenuLabel();
    @DefaultStringValue("Reopen the current file with a different encoding")
    String reopenSourceDocWithEncodingDesc();
    
    // saveSourceDoc
    @DefaultStringValue("Save Current Document")
    String saveSourceDocLabel();
    @DefaultStringValue("")
    String saveSourceDocButtonLabel();
    @DefaultStringValue("_Save")
    String saveSourceDocMenuLabel();
    @DefaultStringValue("Save current document")
    String saveSourceDocDesc();
    
    // renameSourceDoc
    @DefaultStringValue("Rename Current Document")
    String renameSourceDocLabel();
    @DefaultStringValue("")
    String renameSourceDocButtonLabel();
    @DefaultStringValue("_Rename")
    String renameSourceDocMenuLabel();
    @DefaultStringValue("Rename current document")
    String renameSourceDocDesc();
    
    // copySourceDocPath
    @DefaultStringValue("Copy Document Path")
    String copySourceDocPathLabel();
    @DefaultStringValue("")
    String copySourceDocPathButtonLabel();
    @DefaultStringValue("Copy Path")
    String copySourceDocPathMenuLabel();
    @DefaultStringValue("Copy current document path")
    String copySourceDocPathDesc();
    
    // saveSourceDocAs
    @DefaultStringValue("Save Current Document As...")
    String saveSourceDocAsLabel();
    @DefaultStringValue("Save as")
    String saveSourceDocAsButtonLabel();
    @DefaultStringValue("Save _As...")
    String saveSourceDocAsMenuLabel();
    @DefaultStringValue("Save current file to a specific path")
    String saveSourceDocAsDesc();
    
    // saveAllSourceDocs
    @DefaultStringValue("Save All Source Documents")
    String saveAllSourceDocsLabel();
    @DefaultStringValue("")
    String saveAllSourceDocsButtonLabel();
    @DefaultStringValue("Sa_ve All")
    String saveAllSourceDocsMenuLabel();
    @DefaultStringValue("Save all open documents")
    String saveAllSourceDocsDesc();
    
    // saveSourceDocWithEncoding
    @DefaultStringValue("Save Current Document with Encoding...")
    String saveSourceDocWithEncodingLabel();
    @DefaultStringValue("Save wit_h Encoding...")
    String saveSourceDocWithEncodingMenuLabel();
    @DefaultStringValue("Save the current file with a different encoding")
    String saveSourceDocWithEncodingDesc();
    
    // closeSourceDoc
    @DefaultStringValue("Close Current Document")
    String closeSourceDocLabel();
    @DefaultStringValue("_Close")
    String closeSourceDocMenuLabel();
    
    // closeAllSourceDocs
    @DefaultStringValue("Close All Documents")
    String closeAllSourceDocsLabel();
    @DefaultStringValue("C_lose All")
    String closeAllSourceDocsMenuLabel();
    
    // closeOtherSourceDocs
    @DefaultStringValue("Close Other Documents")
    String closeOtherSourceDocsLabel();
    @DefaultStringValue("Close All E_xcept Current")
    String closeOtherSourceDocsMenuLabel();
    
    // vcsFileDiff
    @DefaultStringValue("Show Differences for File")
    String vcsFileDiffLabel();
    @DefaultStringValue("_Diff of")
    String vcsFileDiffMenuLabel();
    @DefaultStringValue("Show differences for the file")
    String vcsFileDiffDesc();
    
    // vcsFileLog
    @DefaultStringValue("Show Changelog for File")
    String vcsFileLogLabel();
    @DefaultStringValue("_Log of")
    String vcsFileLogMenuLabel();
    @DefaultStringValue("Show log of changes to the file")
    String vcsFileLogDesc();
    
    // vcsFileRevert
    @DefaultStringValue("Revert Changes to File")
    String vcsFileRevertLabel();
    @DefaultStringValue("_Revert")
    String vcsFileRevertMenuLabel();
    @DefaultStringValue("Revert changes to the file")
    String vcsFileRevertDesc();
    
    // vcsViewOnGitHub
    @DefaultStringValue("View file on GitHub")
    String vcsViewOnGitHubLabel();
    @DefaultStringValue("_View FILE on GitHub")
    String vcsViewOnGitHubMenuLabel();
    @DefaultStringValue("View this file on Github")
    String vcsViewOnGitHubDesc();
    
    // vcsBlameOnGitHub
    @DefaultStringValue("View 'git blame' on GitHub")
    String vcsBlameOnGitHubLabel();
    @DefaultStringValue("_Blame FILE on GitHub")
    String vcsBlameOnGitHubMenuLabel();
    @DefaultStringValue("Blame view for this file on Github")
    String vcsBlameOnGitHubDesc();
    
    // printSourceDoc
    @DefaultStringValue("")
    String printSourceDocButtonLabel();
    @DefaultStringValue("Pr_int...")
    String printSourceDocMenuLabel();
    @DefaultStringValue("Print the current file")
    String printSourceDocDesc();
    
    // popoutDoc
    @DefaultStringValue("Show Document in New Window")
    String popoutDocLabel();
    @DefaultStringValue("")
    String popoutDocButtonLabel();
    @DefaultStringValue("Show in new window")
    String popoutDocDesc();
    
    // returnDocToMain
    @DefaultStringValue("Return Document to Main Window")
    String returnDocToMainLabel();
    @DefaultStringValue("")
    String returnDocToMainButtonLabel();
    @DefaultStringValue("Return to main window")
    String returnDocToMainDesc();
    
    // mru0
    
    // mru1
    
    // mru2
    
    // mru3
    
    // mru4
    
    // mru5
    
    // mru6
    
    // mru7
    
    // mru8
    
    // mru9
    
    // mru10
    
    // mru11
    
    // mru12
    
    // mru13
    
    // mru14
    
    // clearRecentFiles
    @DefaultStringValue("_Clear List")
    String clearRecentFilesMenuLabel();
    
    // newProject
    @DefaultStringValue("Create a New Project...")
    String newProjectLabel();
    @DefaultStringValue("")
    String newProjectButtonLabel();
    @DefaultStringValue("New _Project...")
    String newProjectMenuLabel();
    @DefaultStringValue("Create a project")
    String newProjectDesc();
    
    // openProject
    @DefaultStringValue("Open Project...")
    String openProjectLabel();
    @DefaultStringValue("")
    String openProjectButtonLabel();
    @DefaultStringValue("Ope_n Project...")
    String openProjectMenuLabel();
    @DefaultStringValue("Open a project")
    String openProjectDesc();
    
    // openProjectInNewWindow
    @DefaultStringValue("Open Project with New R Session")
    String openProjectInNewWindowLabel();
    @DefaultStringValue("")
    String openProjectInNewWindowButtonLabel();
    @DefaultStringValue("Open Project in Ne_w Session...")
    String openProjectInNewWindowMenuLabel();
    @DefaultStringValue("Open project in a new R session")
    String openProjectInNewWindowDesc();
    
    // shareProject
    @DefaultStringValue("Share Project...")
    String shareProjectLabel();
    @DefaultStringValue("")
    String shareProjectButtonLabel();
    @DefaultStringValue("S_hare Project...")
    String shareProjectMenuLabel();
    @DefaultStringValue("Share this project with others")
    String shareProjectDesc();
    
    // openSharedProject
    @DefaultStringValue("Open Shared Project")
    String openSharedProjectLabel();
    @DefaultStringValue("More...")
    String openSharedProjectMenuLabel();
    @DefaultStringValue("Open a project shared with you")
    String openSharedProjectDesc();
    
    // projectMru0
    
    // projectMru1
    
    // projectMru2
    
    // projectMru3
    
    // projectMru4
    
    // projectMru5
    
    // projectMru6
    
    // projectMru7
    
    // projectMru8
    
    // projectMru9
    
    // projectMru10
    
    // projectMru11
    
    // projectMru12
    
    // projectMru13
    
    // projectMru14
    
    // clearRecentProjects
    @DefaultStringValue("_Clear Project List")
    String clearRecentProjectsMenuLabel();
    
    // closeProject
    @DefaultStringValue("Close Current Project")
    String closeProjectLabel();
    @DefaultStringValue("")
    String closeProjectButtonLabel();
    @DefaultStringValue("Close Projec_t")
    String closeProjectMenuLabel();
    @DefaultStringValue("Close the currently open project")
    String closeProjectDesc();
    
    // projectOptions
    @DefaultStringValue("Edit Project Options...")
    String projectOptionsLabel();
    @DefaultStringValue("")
    String projectOptionsButtonLabel();
    @DefaultStringValue("_Project Options...")
    String projectOptionsMenuLabel();
    @DefaultStringValue("Edit options for the current project")
    String projectOptionsDesc();
    
    // projectSweaveOptions
    @DefaultStringValue("")
    String projectSweaveOptionsButtonLabel();
    @DefaultStringValue("")
    String projectSweaveOptionsMenuLabel();
    @DefaultStringValue("")
    String projectSweaveOptionsDesc();
    
    // showToolbar
    @DefaultStringValue("Show _Toolbar")
    String showToolbarMenuLabel();
    
    // hideToolbar
    @DefaultStringValue("Hide _Toolbar")
    String hideToolbarMenuLabel();
    
    // toggleToolbar
    @DefaultStringValue("Toggle Visibility of Toolbar")
    String toggleToolbarLabel();
    @DefaultStringValue("Toggle Toolbar")
    String toggleToolbarMenuLabel();
    
    // zoomActualSize
    @DefaultStringValue("Actual _Size")
    String zoomActualSizeMenuLabel();
    
    // zoomIn
    @DefaultStringValue("_Zoom In")
    String zoomInMenuLabel();
    
    // zoomOut
    @DefaultStringValue("Zoom O_ut")
    String zoomOutMenuLabel();
    
    // goToFileFunction
    @DefaultStringValue("Go To File/Function...")
    String goToFileFunctionLabel();
    @DefaultStringValue("Go To File/F_unction...")
    String goToFileFunctionMenuLabel();
    
    // switchFocusSourceConsole
    @DefaultStringValue("Switch Focus between Source/Console")
    String switchFocusSourceConsoleLabel();
    
    // activateSource
    @DefaultStringValue("Move Focus to Source")
    String activateSourceLabel();
    @DefaultStringValue("Move Focus to Sou_rce")
    String activateSourceMenuLabel();
    
    // activateConsolePane
    @DefaultStringValue("Move Focus to Console Panel")
    String activateConsolePaneLabel();
    @DefaultStringValue("Move Focus to _Console Panel")
    String activateConsolePaneMenuLabel();
    
    // activateConsole
    @DefaultStringValue("Move Focus to Console")
    String activateConsoleLabel();
    @DefaultStringValue("Move Focus to _Console")
    String activateConsoleMenuLabel();
    
    // activateEnvironment
    @DefaultStringValue("Show Environment Pane")
    String activateEnvironmentLabel();
    @DefaultStringValue("Show _Environment")
    String activateEnvironmentMenuLabel();
    
    // activateData
    @DefaultStringValue("Show Data Pane")
    String activateDataLabel();
    @DefaultStringValue("Show _Data")
    String activateDataMenuLabel();
    
    // activateHistory
    @DefaultStringValue("Show History Pane")
    String activateHistoryLabel();
    @DefaultStringValue("Show Histor_y")
    String activateHistoryMenuLabel();
    
    // activateFiles
    @DefaultStringValue("Show Files Pane")
    String activateFilesLabel();
    @DefaultStringValue("Show F_iles")
    String activateFilesMenuLabel();
    
    // activatePlots
    @DefaultStringValue("Show Plots Pane")
    String activatePlotsLabel();
    @DefaultStringValue("Show Pl_ots")
    String activatePlotsMenuLabel();
    
    // activatePackages
    @DefaultStringValue("Show Packages Pane")
    String activatePackagesLabel();
    @DefaultStringValue("Show Pac_kages")
    String activatePackagesMenuLabel();
    
    // activateHelp
    @DefaultStringValue("Show Help Pane")
    String activateHelpLabel();
    @DefaultStringValue("Move Focus to _Help")
    String activateHelpMenuLabel();
    
    // activateVcs
    @DefaultStringValue("Show VCS Pane")
    String activateVcsLabel();
    @DefaultStringValue("Show _Vcs")
    String activateVcsMenuLabel();
    
    // activateBuild
    @DefaultStringValue("Show Build Pane")
    String activateBuildLabel();
    @DefaultStringValue("Show _Build")
    String activateBuildMenuLabel();
    
    // activateViewer
    @DefaultStringValue("Show Viewer Pane")
    String activateViewerLabel();
    @DefaultStringValue("Show Vie_wer")
    String activateViewerMenuLabel();
    
    // activatePresentation
    @DefaultStringValue("Show Presentation Pane")
    String activatePresentationLabel();
    @DefaultStringValue("Show Prese_ntation")
    String activatePresentationMenuLabel();
    
    // activateConnections
    @DefaultStringValue("Show Connections Pane")
    String activateConnectionsLabel();
    @DefaultStringValue("Show Co_nnections")
    String activateConnectionsMenuLabel();
    
    // activateTutorial
    @DefaultStringValue("Show Tutorial Pane")
    String activateTutorialLabel();
    @DefaultStringValue("Show _Tutorial")
    String activateTutorialMenuLabel();
    
    // activateJobs
    @DefaultStringValue("Show Jobs Pane")
    String activateJobsLabel();
    @DefaultStringValue("Show _Jobs")
    String activateJobsMenuLabel();
    
    // activateLauncherJobs
    @DefaultStringValue("Show Launcher Pane")
    String activateLauncherJobsLabel();
    @DefaultStringValue("Show _Launcher")
    String activateLauncherJobsMenuLabel();
    
    // activateCompilePDF
    @DefaultStringValue("Show Compile PDF Pane")
    String activateCompilePDFLabel();
    @DefaultStringValue("Show Compile _PDF")
    String activateCompilePDFMenuLabel();
    
    // activateFindInFiles
    @DefaultStringValue("Show Find in Files")
    String activateFindInFilesLabel();
    @DefaultStringValue("Show _Find in Files")
    String activateFindInFilesMenuLabel();
    
    // activateSourceCpp
    @DefaultStringValue("Show Source Cpp Pane")
    String activateSourceCppLabel();
    @DefaultStringValue("Show Sou_rce Cpp")
    String activateSourceCppMenuLabel();
    
    // activateRMarkdown
    @DefaultStringValue("Show R Markdown Pane")
    String activateRMarkdownLabel();
    @DefaultStringValue("Show _R Markdown")
    String activateRMarkdownMenuLabel();
    
    // activateDeployContent
    @DefaultStringValue("Show Deploy Content Pane")
    String activateDeployContentLabel();
    @DefaultStringValue("Show Deploy _Content")
    String activateDeployContentMenuLabel();
    
    // activateMarkers
    @DefaultStringValue("Show Markers Pane")
    String activateMarkersLabel();
    @DefaultStringValue("Show _Markers")
    String activateMarkersMenuLabel();
    
    // activateSQLResults
    @DefaultStringValue("Show SQL Results Pane")
    String activateSQLResultsLabel();
    @DefaultStringValue("Show S_QL Results")
    String activateSQLResultsMenuLabel();
    
    // layoutZoomSource
    @DefaultStringValue("Zoom Source")
    String layoutZoomSourceLabel();
    @DefaultStringValue("Zoom Sou_rce")
    String layoutZoomSourceMenuLabel();
    
    // layoutZoomConsolePane
    @DefaultStringValue("Zoom Console Pane")
    String layoutZoomConsolePaneLabel();
    @DefaultStringValue("Zoom Console Pane")
    String layoutZoomConsolePaneMenuLabel();
    
    // layoutZoomConsole
    @DefaultStringValue("Zoom Console")
    String layoutZoomConsoleLabel();
    @DefaultStringValue("Zoom _Console")
    String layoutZoomConsoleMenuLabel();
    
    // layoutZoomEnvironment
    @DefaultStringValue("Zoom Environment")
    String layoutZoomEnvironmentLabel();
    @DefaultStringValue("Zoom _Environment")
    String layoutZoomEnvironmentMenuLabel();
    
    // layoutZoomHistory
    @DefaultStringValue("Zoom History")
    String layoutZoomHistoryLabel();
    @DefaultStringValue("Zoom Histor_y")
    String layoutZoomHistoryMenuLabel();
    
    // layoutZoomFiles
    @DefaultStringValue("Zoom Files")
    String layoutZoomFilesLabel();
    @DefaultStringValue("Zoom F_iles")
    String layoutZoomFilesMenuLabel();
    
    // layoutZoomPlots
    @DefaultStringValue("Zoom Plots")
    String layoutZoomPlotsLabel();
    @DefaultStringValue("Zoom Pl_ots")
    String layoutZoomPlotsMenuLabel();
    
    // layoutZoomPackages
    @DefaultStringValue("Zoom Packages")
    String layoutZoomPackagesLabel();
    @DefaultStringValue("Zoom P_ackages")
    String layoutZoomPackagesMenuLabel();
    
    // layoutZoomHelp
    @DefaultStringValue("Zoom Help")
    String layoutZoomHelpLabel();
    @DefaultStringValue("Zoom _Help")
    String layoutZoomHelpMenuLabel();
    
    // layoutZoomVcs
    @DefaultStringValue("Zoom VCS")
    String layoutZoomVcsLabel();
    @DefaultStringValue("Zoom _VCS")
    String layoutZoomVcsMenuLabel();
    
    // layoutZoomTutorial
    @DefaultStringValue("Zoom Tutorial")
    String layoutZoomTutorialLabel();
    @DefaultStringValue("Zoom _Tutorial")
    String layoutZoomTutorialMenuLabel();
    
    // layoutZoomBuild
    @DefaultStringValue("Zoom Build")
    String layoutZoomBuildLabel();
    @DefaultStringValue("Zoom _Build")
    String layoutZoomBuildMenuLabel();
    
    // layoutZoomViewer
    @DefaultStringValue("Zoom Viewer")
    String layoutZoomViewerLabel();
    @DefaultStringValue("Zoom Vie_wer")
    String layoutZoomViewerMenuLabel();
    
    // layoutZoomConnections
    @DefaultStringValue("Zoom Connections")
    String layoutZoomConnectionsLabel();
    @DefaultStringValue("Zoom Co_nnections")
    String layoutZoomConnectionsMenuLabel();
    
    // layoutZoomCurrentPane
    @DefaultStringValue("Toggle Zoom for Current Pane")
    String layoutZoomCurrentPaneLabel();
    
    // layoutEndZoom
    @DefaultStringValue("Show All Panes")
    String layoutEndZoomLabel();
    @DefaultStringValue("_Show All Panes")
    String layoutEndZoomMenuLabel();
    
    // newSourceColumn
    @DefaultStringValue("Add Source Column")
    String newSourceColumnLabel();
    @DefaultStringValue("_Add Source Column")
    String newSourceColumnMenuLabel();
    
    // layoutConsoleOnLeft
    @DefaultStringValue("Console on Left")
    String layoutConsoleOnLeftLabel();
    @DefaultStringValue("Console on _Left")
    String layoutConsoleOnLeftMenuLabel();
    
    // layoutConsoleOnRight
    @DefaultStringValue("Console on Right")
    String layoutConsoleOnRightLabel();
    @DefaultStringValue("Console on _Right")
    String layoutConsoleOnRightMenuLabel();
    
    // layoutZoomLeftColumn
    @DefaultStringValue("Zoom Left / Center Column")
    String layoutZoomLeftColumnLabel();
    @DefaultStringValue("_Zoom Left / Center Column")
    String layoutZoomLeftColumnMenuLabel();
    
    // layoutZoomRightColumn
    @DefaultStringValue("Zoom Right Column")
    String layoutZoomRightColumnLabel();
    @DefaultStringValue("Zoo_m Right Column")
    String layoutZoomRightColumnMenuLabel();
    
    // jumpTo
    @DefaultStringValue("Jump To...")
    String jumpToLabel();
    @DefaultStringValue("_Jump To...")
    String jumpToMenuLabel();
    
    // switchToTab
    @DefaultStringValue("Switch to Tab...")
    String switchToTabLabel();
    @DefaultStringValue("Switch to Ta_b...")
    String switchToTabMenuLabel();
    
    // previousTab
    @DefaultStringValue("Open Previous Tab")
    String previousTabLabel();
    @DefaultStringValue("_Previous Tab")
    String previousTabMenuLabel();
    
    // nextTab
    @DefaultStringValue("Open Next Tab")
    String nextTabLabel();
    @DefaultStringValue("_Next Tab")
    String nextTabMenuLabel();
    
    // firstTab
    @DefaultStringValue("Open First Tab")
    String firstTabLabel();
    @DefaultStringValue("_First Tab")
    String firstTabMenuLabel();
    
    // lastTab
    @DefaultStringValue("Open Last Tab")
    String lastTabLabel();
    @DefaultStringValue("_Last Tab")
    String lastTabMenuLabel();
    
    // moveTabLeft
    @DefaultStringValue("Move Tab Left")
    String moveTabLeftLabel();
    @DefaultStringValue("Move Tab Lef_t")
    String moveTabLeftMenuLabel();
    
    // moveTabRight
    @DefaultStringValue("Move Tab Right")
    String moveTabRightLabel();
    @DefaultStringValue("Move Tab _Right")
    String moveTabRightMenuLabel();
    
    // moveTabToFirst
    @DefaultStringValue("Move Tab to First")
    String moveTabToFirstLabel();
    @DefaultStringValue("Move Tab to _First")
    String moveTabToFirstMenuLabel();
    
    // moveTabToLast
    @DefaultStringValue("Move Tab to Last")
    String moveTabToLastLabel();
    @DefaultStringValue("Move Tab to La_st")
    String moveTabToLastMenuLabel();
    
    // goToLine
    @DefaultStringValue("Go to Line...")
    String goToLineLabel();
    @DefaultStringValue("_Go to Line...")
    String goToLineMenuLabel();
    
    // toggleFullScreen
    @DefaultStringValue("Toggle _Full Screen")
    String toggleFullScreenMenuLabel();
    
    // findFromSelection
    @DefaultStringValue("_Use Selection for Find")
    String findFromSelectionMenuLabel();
    
    // quickAddNext
    @DefaultStringValue("Find and Add Next")
    String quickAddNextLabel();
    @DefaultStringValue("Add")
    String quickAddNextButtonLabel();
    @DefaultStringValue("Find and Add Next")
    String quickAddNextMenuLabel();
    @DefaultStringValue("Find and add next occurence")
    String quickAddNextDesc();
    
    // findAll
    @DefaultStringValue("Find All")
    String findAllLabel();
    
    // findReplace
    @DefaultStringValue("Find / Replace Text...")
    String findReplaceLabel();
    @DefaultStringValue("_Find...")
    String findReplaceMenuLabel();
    
    // findNext
    @DefaultStringValue("Find Next Occurence")
    String findNextLabel();
    @DefaultStringValue("Next")
    String findNextButtonLabel();
    @DefaultStringValue("Find _Next")
    String findNextMenuLabel();
    @DefaultStringValue("Find next occurrence")
    String findNextDesc();
    
    // findPrevious
    @DefaultStringValue("Find Previous Occurence")
    String findPreviousLabel();
    @DefaultStringValue("Prev")
    String findPreviousButtonLabel();
    @DefaultStringValue("Find Pre_vious")
    String findPreviousMenuLabel();
    @DefaultStringValue("Find previous occurrence")
    String findPreviousDesc();
    
    // findSelectAll
    @DefaultStringValue("Find and Select All")
    String findSelectAllLabel();
    @DefaultStringValue("All")
    String findSelectAllButtonLabel();
    @DefaultStringValue("Find And Select All")
    String findSelectAllMenuLabel();
    @DefaultStringValue("Find and select all matches")
    String findSelectAllDesc();
    
    // replaceAndFind
    @DefaultStringValue("Replace and Find Next")
    String replaceAndFindLabel();
    @DefaultStringValue("Replace")
    String replaceAndFindButtonLabel();
    @DefaultStringValue("_Replace and Find")
    String replaceAndFindMenuLabel();
    @DefaultStringValue("Replace and find next occurrence")
    String replaceAndFindDesc();
    
    // findInFiles
    @DefaultStringValue("Find _in Files...")
    String findInFilesMenuLabel();
    
    // fold
    @DefaultStringValue("Collapse Fold")
    String foldLabel();
    @DefaultStringValue("_Collapse")
    String foldMenuLabel();
    
    // unfold
    @DefaultStringValue("Expand Fold")
    String unfoldLabel();
    @DefaultStringValue("E_xpand")
    String unfoldMenuLabel();
    
    // foldAll
    @DefaultStringValue("Collapse All Folds")
    String foldAllLabel();
    @DefaultStringValue("Collapse _All")
    String foldAllMenuLabel();
    
    // unfoldAll
    @DefaultStringValue("Expand All Folds")
    String unfoldAllLabel();
    @DefaultStringValue("Ex_pand All")
    String unfoldAllMenuLabel();
    
    // jumpToMatching
    @DefaultStringValue("Jump to Matching Bracket")
    String jumpToMatchingLabel();
    @DefaultStringValue("Jump To _Matching")
    String jumpToMatchingMenuLabel();
    @DefaultStringValue("Jump to matching bracket")
    String jumpToMatchingDesc();
    
    // expandToMatching
    @DefaultStringValue("Expand to Matching Bracket")
    String expandToMatchingLabel();
    @DefaultStringValue("Expand To _Matching")
    String expandToMatchingMenuLabel();
    @DefaultStringValue("Expand selection to matching bracket")
    String expandToMatchingDesc();
    
    // addCursorAbove
    @DefaultStringValue("Add Cursor Above Current Cursor")
    String addCursorAboveMenuLabel();
    
    // addCursorBelow
    @DefaultStringValue("Add Cursor Below Current Cursor")
    String addCursorBelowMenuLabel();
    
    // moveLinesUp
    @DefaultStringValue("Move Lines Up")
    String moveLinesUpMenuLabel();
    
    // moveLinesDown
    @DefaultStringValue("Move Lines Down")
    String moveLinesDownMenuLabel();
    
    // expandToLine
    @DefaultStringValue("Expand Selection to Line")
    String expandToLineMenuLabel();
    
    // copyLinesDown
    @DefaultStringValue("Copy Lines Down")
    String copyLinesDownMenuLabel();
    
    // joinLines
    @DefaultStringValue("Join Lines")
    String joinLinesMenuLabel();
    
    // removeLine
    @DefaultStringValue("Remove Line")
    String removeLineMenuLabel();
    
    // splitIntoLines
    @DefaultStringValue("Split Into Lines")
    String splitIntoLinesMenuLabel();
    @DefaultStringValue("Create a new cursor on each line in current selection")
    String splitIntoLinesDesc();
    
    // editLinesFromStart
    @DefaultStringValue("Edit Lines from Start")
    String editLinesFromStartMenuLabel();
    @DefaultStringValue("Create a new cursor at start of each line in selection")
    String editLinesFromStartDesc();
    
    // executeAllCode
    @DefaultStringValue("Run All Code in Current Source File")
    String executeAllCodeLabel();
    @DefaultStringValue("")
    String executeAllCodeButtonLabel();
    @DefaultStringValue("Run _All")
    String executeAllCodeMenuLabel();
    @DefaultStringValue("Run all of the code in the source file")
    String executeAllCodeDesc();
    
    // executeCode
    @DefaultStringValue("Run Current Line or Selection")
    String executeCodeLabel();
    @DefaultStringValue("Run")
    String executeCodeButtonLabel();
    @DefaultStringValue("Run Selected _Line(s)")
    String executeCodeMenuLabel();
    @DefaultStringValue("Run the current line or selection")
    String executeCodeDesc();
    
    // executeCodeWithoutMovingCursor
    @DefaultStringValue("Run Current Line or Selection (Without Moving Cursor)")
    String executeCodeWithoutMovingCursorLabel();
    @DefaultStringValue("Run")
    String executeCodeWithoutMovingCursorButtonLabel();
    @DefaultStringValue("Run _Line(s) without moving cursor")
    String executeCodeWithoutMovingCursorMenuLabel();
    @DefaultStringValue("Run the current line or selection without moving the cursor")
    String executeCodeWithoutMovingCursorDesc();
    
    // executeCodeWithoutFocus
    
    // executeToCurrentLine
    @DefaultStringValue("Execute Code up to Current Line")
    String executeToCurrentLineLabel();
    @DefaultStringValue("Run From _Beginning To Line")
    String executeToCurrentLineMenuLabel();
    @DefaultStringValue("Run from the beginning of the source file up through the current line")
    String executeToCurrentLineDesc();
    
    // executeFromCurrentLine
    @DefaultStringValue("Execute Code From Current Line to End of Document")
    String executeFromCurrentLineLabel();
    @DefaultStringValue("Run From Line to _End")
    String executeFromCurrentLineMenuLabel();
    @DefaultStringValue("Run from the current line through the end of the source file")
    String executeFromCurrentLineDesc();
    
    // executeCurrentFunction
    @DefaultStringValue("Run Current Function Definition")
    String executeCurrentFunctionLabel();
    @DefaultStringValue("Run _Function Definition")
    String executeCurrentFunctionMenuLabel();
    @DefaultStringValue("Run the top-level function definition, if any, that contains the cursor")
    String executeCurrentFunctionDesc();
    
    // executeCurrentSection
    @DefaultStringValue("Execute Current Code Section")
    String executeCurrentSectionLabel();
    @DefaultStringValue("Run Section")
    String executeCurrentSectionButtonLabel();
    @DefaultStringValue("Run Code _Section")
    String executeCurrentSectionMenuLabel();
    @DefaultStringValue("Run the code section that contains the cursor")
    String executeCurrentSectionDesc();
    
    // executeLastCode
    @DefaultStringValue("Re-Run Previous Code Execution")
    String executeLastCodeLabel();
    @DefaultStringValue("")
    String executeLastCodeButtonLabel();
    @DefaultStringValue("Re-Run _Previous")
    String executeLastCodeMenuLabel();
    @DefaultStringValue("Re-run the previous code region")
    String executeLastCodeDesc();
    
    // executeCurrentLine
    @DefaultStringValue("Execute Current Line")
    String executeCurrentLineLabel();
    @DefaultStringValue("Execute Current _Line")
    String executeCurrentLineMenuLabel();
    @DefaultStringValue("Execute the line which contains the cursor")
    String executeCurrentLineDesc();
    
    // executeCurrentStatement
    @DefaultStringValue("Execute Current Statement")
    String executeCurrentStatementLabel();
    @DefaultStringValue("Execute Current _Statement")
    String executeCurrentStatementMenuLabel();
    @DefaultStringValue("Execute the entire R statement which contains the cursor.")
    String executeCurrentStatementDesc();
    
    // executeCurrentParagraph
    @DefaultStringValue("Execute Current Paragraph")
    String executeCurrentParagraphLabel();
    @DefaultStringValue("Execute Current _Paragraph")
    String executeCurrentParagraphMenuLabel();
    @DefaultStringValue("Execute the current paragraph of code, delimited by blank lines.")
    String executeCurrentParagraphDesc();
    
    // insertChunk
    @DefaultStringValue("_Insert Chunk")
    String insertChunkMenuLabel();
    @DefaultStringValue("Insert a new code chunk")
    String insertChunkDesc();
    
    // insertChunkR
    @DefaultStringValue("R")
    String insertChunkRMenuLabel();
    @DefaultStringValue("Insert a new R chunk")
    String insertChunkRDesc();
    
    // insertChunkBash
    @DefaultStringValue("Bash")
    String insertChunkBashMenuLabel();
    @DefaultStringValue("Insert a new Bash chunk")
    String insertChunkBashDesc();
    
    // insertChunkD3
    @DefaultStringValue("D3")
    String insertChunkD3MenuLabel();
    @DefaultStringValue("Insert a new D3 chunk")
    String insertChunkD3Desc();
    
    // insertChunkPython
    @DefaultStringValue("Python")
    String insertChunkPythonMenuLabel();
    @DefaultStringValue("Insert a new Python chunk")
    String insertChunkPythonDesc();
    
    // insertChunkRCPP
    @DefaultStringValue("Rcpp")
    String insertChunkRCPPMenuLabel();
    @DefaultStringValue("Insert a new Rcpp chunk")
    String insertChunkRCPPDesc();
    
    // insertChunkStan
    @DefaultStringValue("Stan")
    String insertChunkStanMenuLabel();
    @DefaultStringValue("Insert a new Stan chunk")
    String insertChunkStanDesc();
    
    // insertChunkSQL
    @DefaultStringValue("SQL")
    String insertChunkSQLMenuLabel();
    @DefaultStringValue("Insert a new SQL chunk")
    String insertChunkSQLDesc();
    
    // switchToChunkR
    @DefaultStringValue("R")
    String switchToChunkRMenuLabel();
    @DefaultStringValue("Switch chunk to R")
    String switchToChunkRDesc();
    
    // switchToChunkBash
    @DefaultStringValue("Bash")
    String switchToChunkBashMenuLabel();
    @DefaultStringValue("Switch chunk to Bash")
    String switchToChunkBashDesc();
    
    // switchToChunkPython
    @DefaultStringValue("Python")
    String switchToChunkPythonMenuLabel();
    @DefaultStringValue("Switch chunk to Python")
    String switchToChunkPythonDesc();
    
    // switchToChunkRCPP
    @DefaultStringValue("Rcpp")
    String switchToChunkRCPPMenuLabel();
    @DefaultStringValue("Switch chunk to Rcpp")
    String switchToChunkRCPPDesc();
    
    // switchToChunkStan
    @DefaultStringValue("Stan")
    String switchToChunkStanMenuLabel();
    @DefaultStringValue("Switch chunk to Stan")
    String switchToChunkStanDesc();
    
    // switchToChunkSQL
    @DefaultStringValue("SQL")
    String switchToChunkSQLMenuLabel();
    @DefaultStringValue("Switch chunk to SQL")
    String switchToChunkSQLDesc();
    
    // insertSection
    @DefaultStringValue("_Insert Section...")
    String insertSectionMenuLabel();
    @DefaultStringValue("Insert a new code section")
    String insertSectionDesc();
    
    // executePreviousChunks
    @DefaultStringValue("_Run All Chunks Above")
    String executePreviousChunksMenuLabel();
    @DefaultStringValue("Run all chunks above the current one")
    String executePreviousChunksDesc();
    
    // executeSubsequentChunks
    @DefaultStringValue("Run All C_hunks Below")
    String executeSubsequentChunksMenuLabel();
    @DefaultStringValue("Run all chunks below the current one")
    String executeSubsequentChunksDesc();
    
    // executeCurrentChunk
    @DefaultStringValue("Run _Current Chunk")
    String executeCurrentChunkMenuLabel();
    @DefaultStringValue("Run the current code chunk")
    String executeCurrentChunkDesc();
    
    // executeNextChunk
    @DefaultStringValue("Run _Next Chunk")
    String executeNextChunkMenuLabel();
    @DefaultStringValue("Run the next code chunk")
    String executeNextChunkDesc();
    
    // executeSetupChunk
    @DefaultStringValue("Run _Setup Chunk")
    String executeSetupChunkMenuLabel();
    @DefaultStringValue("Run the initial setup chunk")
    String executeSetupChunkDesc();
    
    // goToHelp
    @DefaultStringValue("Show Help for Current Function")
    String goToHelpLabel();
    @DefaultStringValue("Go To _Help")
    String goToHelpMenuLabel();
    @DefaultStringValue("Go to help for the currently selected function")
    String goToHelpDesc();
    
    // goToDefinition
    @DefaultStringValue("_Go To Function Definition")
    String goToDefinitionMenuLabel();
    @DefaultStringValue("Go to to the definition of the currently selected function")
    String goToDefinitionDesc();
    
    // codeCompletion
    @DefaultStringValue("Retrieve Completions")
    String codeCompletionLabel();
    @DefaultStringValue("Code Completion")
    String codeCompletionMenuLabel();
    @DefaultStringValue("Show code completions at the current cursor location")
    String codeCompletionDesc();
    
    // sourceNavigateBack
    @DefaultStringValue("Bac_k")
    String sourceNavigateBackMenuLabel();
    @DefaultStringValue("Go back to the previous source location")
    String sourceNavigateBackDesc();
    
    // sourceNavigateForward
    @DefaultStringValue("For_ward")
    String sourceNavigateForwardMenuLabel();
    @DefaultStringValue("Go forward to the next source location")
    String sourceNavigateForwardDesc();
    
    // extractFunction
    @DefaultStringValue("E_xtract Function")
    String extractFunctionMenuLabel();
    @DefaultStringValue("Turn the current selection into a function")
    String extractFunctionDesc();
    
    // extractLocalVariable
    @DefaultStringValue("Extract _Variable")
    String extractLocalVariableMenuLabel();
    @DefaultStringValue("Extract a variable out of the current selection")
    String extractLocalVariableDesc();
    
    // findUsages
    @DefaultStringValue("Find _Usages")
    String findUsagesMenuLabel();
    @DefaultStringValue("Find source locations where this symbol is used")
    String findUsagesDesc();
    
    // sourceFile
    @DefaultStringValue("")
    String sourceFileButtonLabel();
    @DefaultStringValue("Source _File...")
    String sourceFileMenuLabel();
    @DefaultStringValue("Source the contents of an R file")
    String sourceFileDesc();
    
    // previewJS
    @DefaultStringValue("Preview")
    String previewJSButtonLabel();
    @DefaultStringValue("Preview J_S")
    String previewJSMenuLabel();
    @DefaultStringValue("Preview the active JavaScript document")
    String previewJSDesc();
    
    // previewSql
    @DefaultStringValue("Preview")
    String previewSqlButtonLabel();
    @DefaultStringValue("Preview S_QL")
    String previewSqlMenuLabel();
    @DefaultStringValue("Preview the active SQL document")
    String previewSqlDesc();
    
    // sourceActiveDocument
    @DefaultStringValue("Source")
    String sourceActiveDocumentButtonLabel();
    @DefaultStringValue("_Source")
    String sourceActiveDocumentMenuLabel();
    @DefaultStringValue("Source the contents of the active document")
    String sourceActiveDocumentDesc();
    
    // sourceActiveDocumentWithEcho
    @DefaultStringValue("")
    String sourceActiveDocumentWithEchoButtonLabel();
    @DefaultStringValue("Source with _Echo")
    String sourceActiveDocumentWithEchoMenuLabel();
    @DefaultStringValue("Source the contents of the active document (with echo)")
    String sourceActiveDocumentWithEchoDesc();
    
    // commentUncomment
    @DefaultStringValue("Comment / Uncomment Selection")
    String commentUncommentLabel();
    @DefaultStringValue("_Comment/Uncomment Lines")
    String commentUncommentMenuLabel();
    @DefaultStringValue("Comment or uncomment the current line/selection")
    String commentUncommentDesc();
    
    // reformatCode
    @DefaultStringValue("Reformat Current Selection")
    String reformatCodeLabel();
    @DefaultStringValue("Re_format Code")
    String reformatCodeMenuLabel();
    @DefaultStringValue("Reformat the current line/selection")
    String reformatCodeDesc();
    
    // showDiagnosticsActiveDocument
    @DefaultStringValue("Show Diagnostics for Current Document")
    String showDiagnosticsActiveDocumentLabel();
    @DefaultStringValue("Show _Diagnostics")
    String showDiagnosticsActiveDocumentMenuLabel();
    @DefaultStringValue("Show diagnostics for the active document")
    String showDiagnosticsActiveDocumentDesc();
    
    // showDiagnosticsProject
    @DefaultStringValue("Show Diagnostics for Current Project")
    String showDiagnosticsProjectLabel();
    @DefaultStringValue("Show Diagnostics (Projec_t)")
    String showDiagnosticsProjectMenuLabel();
    @DefaultStringValue("Show diagnostics for all source files in the current project")
    String showDiagnosticsProjectDesc();
    
    // reindent
    @DefaultStringValue("Reindent Selection")
    String reindentLabel();
    @DefaultStringValue("_Reindent Lines")
    String reindentMenuLabel();
    @DefaultStringValue("Reindent the current line/selection")
    String reindentDesc();
    
    // reflowComment
    @DefaultStringValue("Reflow Co_mment")
    String reflowCommentMenuLabel();
    @DefaultStringValue("Reflow selected comment lines so they wrap evenly")
    String reflowCommentDesc();
    
    // renameInScope
    @DefaultStringValue("Rename Symbol in Scope")
    String renameInScopeLabel();
    @DefaultStringValue("Ren_ame in Scope")
    String renameInScopeMenuLabel();
    @DefaultStringValue("Rename symbol in current scope")
    String renameInScopeDesc();
    
    // insertSnippet
    @DefaultStringValue("Insert Snippet")
    String insertSnippetMenuLabel();
    @DefaultStringValue("Expand snippet at cursor")
    String insertSnippetDesc();
    
    // insertRoxygenSkeleton
    @DefaultStringValue("Insert Ro_xygen Skeleton")
    String insertRoxygenSkeletonMenuLabel();
    @DefaultStringValue("Insert a roxygen comment for the current function")
    String insertRoxygenSkeletonDesc();
    
    // expandSelection
    @DefaultStringValue("Expand Selection")
    String expandSelectionMenuLabel();
    @DefaultStringValue("Expand selection")
    String expandSelectionDesc();
    
    // shrinkSelection
    @DefaultStringValue("Shrink Selection")
    String shrinkSelectionMenuLabel();
    @DefaultStringValue("Shrink selection")
    String shrinkSelectionDesc();
    
    // goToNextSection
    @DefaultStringValue("Go to Next Section")
    String goToNextSectionLabel();
    @DefaultStringValue("")
    String goToNextSectionButtonLabel();
    @DefaultStringValue("Go to next section/chunk")
    String goToNextSectionDesc();
    
    // goToPrevSection
    @DefaultStringValue("Go to Previous Section")
    String goToPrevSectionLabel();
    @DefaultStringValue("")
    String goToPrevSectionButtonLabel();
    @DefaultStringValue("Go to previous section/chunk")
    String goToPrevSectionDesc();
    
    // goToStartOfCurrentScope
    
    // goToEndOfCurrentScope
    
    // goToNextChunk
    @DefaultStringValue("Go to Next Chunk")
    String goToNextChunkLabel();
    @DefaultStringValue("Go to next chunk")
    String goToNextChunkDesc();
    
    // goToPrevChunk
    @DefaultStringValue("Go to Previous Chunk")
    String goToPrevChunkLabel();
    @DefaultStringValue("Go to previous chunk")
    String goToPrevChunkDesc();
    
    // expandRaggedSelection
    @DefaultStringValue("Expand Ragged Selection")
    String expandRaggedSelectionLabel();
    
    // markdownHelp
    @DefaultStringValue("Open Markdown Quick Reference")
    String markdownHelpLabel();
    @DefaultStringValue("_Markdown Quick Reference")
    String markdownHelpMenuLabel();
    @DefaultStringValue("Markdown quick reference")
    String markdownHelpDesc();
    
    // openRoxygenQuickReference
    @DefaultStringValue("_Roxygen Quick Reference")
    String openRoxygenQuickReferenceMenuLabel();
    @DefaultStringValue("Roxygen quick reference")
    String openRoxygenQuickReferenceDesc();
    
    // toggleDocumentOutline
    @DefaultStringValue("Toggle Document Outline")
    String toggleDocumentOutlineLabel();
    @DefaultStringValue("_Show Document Outline")
    String toggleDocumentOutlineMenuLabel();
    @DefaultStringValue("Show document outline")
    String toggleDocumentOutlineDesc();
    
    // toggleRmdVisualMode
    @DefaultStringValue("Toggle Visual Markdown Editor")
    String toggleRmdVisualModeLabel();
    @DefaultStringValue("_Use Visual Editor")
    String toggleRmdVisualModeMenuLabel();
    @DefaultStringValue("Toggle visual markdown editor")
    String toggleRmdVisualModeDesc();
    
    // enableProsemirrorDevTools
    @DefaultStringValue("Enable Prosemirror DevTools")
    String enableProsemirrorDevToolsLabel();
    @DefaultStringValue("_Prosemirror DevTools")
    String enableProsemirrorDevToolsMenuLabel();
    @DefaultStringValue("Enable Prosemirror DevTools")
    String enableProsemirrorDevToolsDesc();
    
    // usingRMarkdownHelp
    @DefaultStringValue("_Using R Markdown")
    String usingRMarkdownHelpMenuLabel();
    @DefaultStringValue("Guide to using R Markdown")
    String usingRMarkdownHelpDesc();
    
    // authoringRPresentationsHelp
    @DefaultStringValue("_Authoring R Presentations")
    String authoringRPresentationsHelpMenuLabel();
    @DefaultStringValue("Guide to using R Markdown")
    String authoringRPresentationsHelpDesc();
    
    // openRStudioIDECheatSheet
    @DefaultStringValue("_RStudio IDE Cheat Sheet")
    String openRStudioIDECheatSheetMenuLabel();
    @DefaultStringValue("RStudio IDE cheat sheet")
    String openRStudioIDECheatSheetDesc();
    
    // openDataVisualizationCheatSheet
    @DefaultStringValue("Data Visualization with _ggplot2")
    String openDataVisualizationCheatSheetMenuLabel();
    @DefaultStringValue("Data visualization with ggplot2")
    String openDataVisualizationCheatSheetDesc();
    
    // openPurrrCheatSheet
    @DefaultStringValue("List manipulation with _purrr")
    String openPurrrCheatSheetMenuLabel();
    @DefaultStringValue("List manipulation with purrr")
    String openPurrrCheatSheetDesc();
    
    // openPackageDevelopmentCheatSheet
    @DefaultStringValue("Package De_velopment with devtools")
    String openPackageDevelopmentCheatSheetMenuLabel();
    @DefaultStringValue("Package development with devtools")
    String openPackageDevelopmentCheatSheetDesc();
    
    // openDataImportCheatSheet
    @DefaultStringValue("_Import Data with readr")
    String openDataImportCheatSheetMenuLabel();
    @DefaultStringValue("Import data with readr")
    String openDataImportCheatSheetDesc();
    
    // openDataWranglingCheatSheet
    @DefaultStringValue("Data Manipulation with dplyr, tid_yr")
    String openDataWranglingCheatSheetMenuLabel();
    @DefaultStringValue("Data manipulation with dplyr and tidyr")
    String openDataWranglingCheatSheetDesc();
    
    // openDataTransformationCheatSheet
    @DefaultStringValue("Data Transformation with _dplyr")
    String openDataTransformationCheatSheetMenuLabel();
    @DefaultStringValue("Data transformation with dplyr")
    String openDataTransformationCheatSheetDesc();
    
    // openSparklyrCheatSheet
    @DefaultStringValue("Interfacing Spar_k with sparklyr")
    String openSparklyrCheatSheetMenuLabel();
    @DefaultStringValue("Interfacing Apache Spark with sparklyr")
    String openSparklyrCheatSheetDesc();
    
    // openRMarkdownCheatSheet
    @DefaultStringValue("R _Markdown Cheat Sheet")
    String openRMarkdownCheatSheetMenuLabel();
    @DefaultStringValue("R Markdown cheat sheet")
    String openRMarkdownCheatSheetDesc();
    
    // openRMarkdownReferenceGuide
    @DefaultStringValue("R Markdo_wn Reference Guide")
    String openRMarkdownReferenceGuideMenuLabel();
    @DefaultStringValue("R Markdown reference guide")
    String openRMarkdownReferenceGuideDesc();
    
    // openShinyCheatSheet
    @DefaultStringValue("Web Applications with _shiny")
    String openShinyCheatSheetMenuLabel();
    @DefaultStringValue("Build web applications with Shiny")
    String openShinyCheatSheetDesc();
    
    // browseCheatSheets
    @DefaultStringValue("_Browse Cheat Sheets...")
    String browseCheatSheetsMenuLabel();
    @DefaultStringValue("Browse available cheat sheets in your web browser")
    String browseCheatSheetsDesc();
    
    // knitDocument
    @DefaultStringValue("Knit Current Document")
    String knitDocumentLabel();
    @DefaultStringValue("Knit")
    String knitDocumentButtonLabel();
    @DefaultStringValue("_Knit Document")
    String knitDocumentMenuLabel();
    @DefaultStringValue("Knit the current document")
    String knitDocumentDesc();
    
    // previewHTML
    @DefaultStringValue("Preview Document as HTML")
    String previewHTMLLabel();
    @DefaultStringValue("Preview")
    String previewHTMLButtonLabel();
    @DefaultStringValue("Previe_w")
    String previewHTMLMenuLabel();
    @DefaultStringValue("Show a preview of the current document as HTML")
    String previewHTMLDesc();
    
    // publishHTML
    @DefaultStringValue("Publish to RPubs...")
    String publishHTMLLabel();
    @DefaultStringValue("Publish")
    String publishHTMLButtonLabel();
    @DefaultStringValue("P_ublish to RPubs...")
    String publishHTMLMenuLabel();
    @DefaultStringValue("Publish the current document")
    String publishHTMLDesc();
    
    // compilePDF
    @DefaultStringValue("Compile to PDF...")
    String compilePDFLabel();
    @DefaultStringValue("Compile PDF")
    String compilePDFButtonLabel();
    @DefaultStringValue("_Compile PDF")
    String compilePDFMenuLabel();
    @DefaultStringValue("Compile a PDF from the current LaTeX or Sweave document")
    String compilePDFDesc();
    
    // compileNotebook
    @DefaultStringValue("_Compile Report...")
    String compileNotebookMenuLabel();
    @DefaultStringValue("Compile a report from the current R script")
    String compileNotebookDesc();
    
    // editRmdFormatOptions
    @DefaultStringValue("_Output Options...")
    String editRmdFormatOptionsMenuLabel();
    @DefaultStringValue("Edit the R Markdown format options for the current file")
    String editRmdFormatOptionsDesc();
    
    // knitWithParameters
    @DefaultStringValue("Knit with Parameters...")
    String knitWithParametersLabel();
    @DefaultStringValue("Knit _with Parameters...")
    String knitWithParametersMenuLabel();
    @DefaultStringValue("Knit the document with a set of custom parameters")
    String knitWithParametersDesc();
    
    // clearKnitrCache
    @DefaultStringValue("Clear Knitr Cache...")
    String clearKnitrCacheMenuLabel();
    @DefaultStringValue("Clear the knitr cache for the current document")
    String clearKnitrCacheDesc();
    
    // clearPrerenderedOutput
    @DefaultStringValue("Clear Prerendered Output...")
    String clearPrerenderedOutputMenuLabel();
    @DefaultStringValue("Clear the prerendered output for the current document")
    String clearPrerenderedOutputDesc();
    
    // notebookExpandAllOutput
    @DefaultStringValue("")
    String notebookExpandAllOutputButtonLabel();
    @DefaultStringValue("E_xpand All Output")
    String notebookExpandAllOutputMenuLabel();
    @DefaultStringValue("Expand all code chunk output in the current file")
    String notebookExpandAllOutputDesc();
    
    // notebookToggleExpansion
    @DefaultStringValue("")
    String notebookToggleExpansionButtonLabel();
    @DefaultStringValue("Toggle Chunk Output Expansion")
    String notebookToggleExpansionMenuLabel();
    @DefaultStringValue("Expand or collapse the output of the current notebook chunk")
    String notebookToggleExpansionDesc();
    
    // notebookCollapseAllOutput
    @DefaultStringValue("")
    String notebookCollapseAllOutputButtonLabel();
    @DefaultStringValue("Collapse All _Output")
    String notebookCollapseAllOutputMenuLabel();
    @DefaultStringValue("Collapse all code chunk output in the current file")
    String notebookCollapseAllOutputDesc();
    
    // notebookClearOutput
    @DefaultStringValue("")
    String notebookClearOutputButtonLabel();
    @DefaultStringValue("Cl_ear Output")
    String notebookClearOutputMenuLabel();
    @DefaultStringValue("Clear the output of the current notebook chunk")
    String notebookClearOutputDesc();
    
    // notebookClearAllOutput
    @DefaultStringValue("")
    String notebookClearAllOutputButtonLabel();
    @DefaultStringValue("Clear A_ll Output")
    String notebookClearAllOutputMenuLabel();
    @DefaultStringValue("Remove all code chunk output in the current file")
    String notebookClearAllOutputDesc();
    
    // synctexSearch
    @DefaultStringValue("")
    String synctexSearchButtonLabel();
    @DefaultStringValue("S_ync PDF View to Editor")
    String synctexSearchMenuLabel();
    @DefaultStringValue("Sync PDF view to editor location (Ctrl+Click)")
    String synctexSearchDesc();
    
    // checkSpelling
    @DefaultStringValue("Check _Spelling...")
    String checkSpellingMenuLabel();
    @DefaultStringValue("Check spelling in document")
    String checkSpellingDesc();
    
    // wordCount
    @DefaultStringValue("_Word Count")
    String wordCountMenuLabel();
    @DefaultStringValue("Count words in selection or document")
    String wordCountDesc();
    
    // newFolder
    @DefaultStringValue("Create a New Folder...")
    String newFolderLabel();
    @DefaultStringValue("New Folder")
    String newFolderButtonLabel();
    @DefaultStringValue("Folder...")
    String newFolderMenuLabel();
    @DefaultStringValue("Create a new folder")
    String newFolderDesc();
    
    // uploadFile
    @DefaultStringValue("Upload Files...")
    String uploadFileLabel();
    @DefaultStringValue("Upload")
    String uploadFileButtonLabel();
    @DefaultStringValue("Upload Files...")
    String uploadFileMenuLabel();
    @DefaultStringValue("Upload files to server")
    String uploadFileDesc();
    
    // copyFile
    @DefaultStringValue("Copy Files...")
    String copyFileLabel();
    @DefaultStringValue("Copy")
    String copyFileButtonLabel();
    @DefaultStringValue("Copy...")
    String copyFileMenuLabel();
    @DefaultStringValue("Copy selected file or folder")
    String copyFileDesc();
    
    // copyFileTo
    @DefaultStringValue("Copy Files To...")
    String copyFileToLabel();
    @DefaultStringValue("Copy To")
    String copyFileToButtonLabel();
    @DefaultStringValue("Copy To...")
    String copyFileToMenuLabel();
    @DefaultStringValue("Copy selected file or folder to another folder")
    String copyFileToDesc();
    
    // moveFiles
    @DefaultStringValue("Move Files...")
    String moveFilesLabel();
    @DefaultStringValue("Move")
    String moveFilesButtonLabel();
    @DefaultStringValue("Move...")
    String moveFilesMenuLabel();
    @DefaultStringValue("Move selected files or folders")
    String moveFilesDesc();
    
    // exportFiles
    @DefaultStringValue("Export Files...")
    String exportFilesLabel();
    @DefaultStringValue("Export")
    String exportFilesButtonLabel();
    @DefaultStringValue("Export...")
    String exportFilesMenuLabel();
    @DefaultStringValue("Export selected files or folders")
    String exportFilesDesc();
    
    // renameFile
    @DefaultStringValue("Rename Current File...")
    String renameFileLabel();
    @DefaultStringValue("Rename")
    String renameFileButtonLabel();
    @DefaultStringValue("Rename selected file or folder")
    String renameFileDesc();
    
    // deleteFiles
    @DefaultStringValue("Delete Files...")
    String deleteFilesLabel();
    @DefaultStringValue("Delete")
    String deleteFilesButtonLabel();
    @DefaultStringValue("Delete selected files or folders")
    String deleteFilesDesc();
    
    // refreshFiles
    @DefaultStringValue("Refresh")
    String refreshFilesMenuLabel();
    @DefaultStringValue("Refresh file listing")
    String refreshFilesDesc();
    
    // goToWorkingDir
    @DefaultStringValue("")
    String goToWorkingDirButtonLabel();
    @DefaultStringValue("Go To Working Directory")
    String goToWorkingDirMenuLabel();
    @DefaultStringValue("View the current working directory")
    String goToWorkingDirDesc();
    
    // setAsWorkingDir
    @DefaultStringValue("Set As Working Directory")
    String setAsWorkingDirLabel();
    
    // copyFilesPaneCurrentDirectory
    @DefaultStringValue("Copy Folder Path to Clipboard")
    String copyFilesPaneCurrentDirectoryLabel();
    
    // showFolder
    @DefaultStringValue("Show Folder in New Window")
    String showFolderLabel();
    
    // vcsAddFiles
    @DefaultStringValue("Add Files or Folders")
    String vcsAddFilesLabel();
    @DefaultStringValue("Add")
    String vcsAddFilesButtonLabel();
    @DefaultStringValue("Add")
    String vcsAddFilesMenuLabel();
    @DefaultStringValue("Add the selected files or folders")
    String vcsAddFilesDesc();
    
    // vcsRemoveFiles
    @DefaultStringValue("Remove Files or Folders")
    String vcsRemoveFilesLabel();
    @DefaultStringValue("Delete")
    String vcsRemoveFilesButtonLabel();
    @DefaultStringValue("Delete")
    String vcsRemoveFilesMenuLabel();
    @DefaultStringValue("Delete the selected files or folders")
    String vcsRemoveFilesDesc();
    
    // vcsDiff
    @DefaultStringValue("Diff Selected Files")
    String vcsDiffLabel();
    @DefaultStringValue("Diff")
    String vcsDiffButtonLabel();
    @DefaultStringValue("Diff")
    String vcsDiffMenuLabel();
    @DefaultStringValue("Diff selected file(s)")
    String vcsDiffDesc();
    
    // vcsCommit
    @DefaultStringValue("Commit Pending Changes")
    String vcsCommitLabel();
    @DefaultStringValue("Commit")
    String vcsCommitButtonLabel();
    @DefaultStringValue("_Commit...")
    String vcsCommitMenuLabel();
    @DefaultStringValue("Commit pending changes")
    String vcsCommitDesc();
    
    // vcsRevert
    @DefaultStringValue("Revert Changes")
    String vcsRevertLabel();
    @DefaultStringValue("Revert")
    String vcsRevertButtonLabel();
    @DefaultStringValue("Revert...")
    String vcsRevertMenuLabel();
    @DefaultStringValue("Revert selected changes")
    String vcsRevertDesc();
    
    // vcsShowHistory
    @DefaultStringValue("View History of Previous Commits")
    String vcsShowHistoryLabel();
    @DefaultStringValue("History")
    String vcsShowHistoryButtonLabel();
    @DefaultStringValue("_History")
    String vcsShowHistoryMenuLabel();
    @DefaultStringValue("View history of previous commits")
    String vcsShowHistoryDesc();
    
    // vcsRefresh
    @DefaultStringValue("Refresh File List from Source Control")
    String vcsRefreshLabel();
    @DefaultStringValue("Refresh listing")
    String vcsRefreshDesc();
    
    // vcsRefreshNoError
    
    // vcsOpen
    @DefaultStringValue("Open Selected Files(s)")
    String vcsOpenLabel();
    @DefaultStringValue("Open File")
    String vcsOpenMenuLabel();
    @DefaultStringValue("Open selected file(s)")
    String vcsOpenDesc();
    
    // vcsIgnore
    @DefaultStringValue("Ignore Files or Folders")
    String vcsIgnoreLabel();
    @DefaultStringValue("Ignore")
    String vcsIgnoreButtonLabel();
    @DefaultStringValue("Ignore...")
    String vcsIgnoreMenuLabel();
    @DefaultStringValue("Ignore the selected files or folders")
    String vcsIgnoreDesc();
    
    // vcsPull
    @DefaultStringValue("Pull")
    String vcsPullButtonLabel();
    @DefaultStringValue("_Pull Branches")
    String vcsPullMenuLabel();
    
    // vcsPullRebase
    @DefaultStringValue("Pull with Rebase")
    String vcsPullRebaseButtonLabel();
    @DefaultStringValue("_Pull with Rebase")
    String vcsPullRebaseMenuLabel();
    
    // vcsPush
    @DefaultStringValue("Push")
    String vcsPushButtonLabel();
    @DefaultStringValue("P_ush Branch")
    String vcsPushMenuLabel();
    
    // vcsCleanup
    @DefaultStringValue("Cleanup")
    String vcsCleanupButtonLabel();
    @DefaultStringValue("Cleanu_p")
    String vcsCleanupMenuLabel();
    @DefaultStringValue("Recursively clean up the working copy (removing locks, etc)")
    String vcsCleanupDesc();
    
    // vcsResolve
    @DefaultStringValue("Resolve")
    String vcsResolveButtonLabel();
    @DefaultStringValue("Resolve...")
    String vcsResolveMenuLabel();
    @DefaultStringValue("Resolve conflicts in the selected files or folders")
    String vcsResolveDesc();
    
    // consoleClear
    @DefaultStringValue("Clear Console")
    String consoleClearLabel();
    @DefaultStringValue("")
    String consoleClearButtonLabel();
    @DefaultStringValue("Cle_ar Console")
    String consoleClearMenuLabel();
    @DefaultStringValue("Clear console")
    String consoleClearDesc();
    
    // clearBuild
    @DefaultStringValue("Clear Build Pane Output")
    String clearBuildLabel();
    @DefaultStringValue("")
    String clearBuildButtonLabel();
    @DefaultStringValue("Clear build")
    String clearBuildDesc();
    
    // interruptR
    @DefaultStringValue("Interrupt R Session")
    String interruptRLabel();
    @DefaultStringValue("")
    String interruptRButtonLabel();
    @DefaultStringValue("_Interrupt R")
    String interruptRMenuLabel();
    @DefaultStringValue("Interrupt R")
    String interruptRDesc();
    
    // restartR
    @DefaultStringValue("Restart R Session")
    String restartRLabel();
    @DefaultStringValue("_Restart R")
    String restartRMenuLabel();
    @DefaultStringValue("Restart R")
    String restartRDesc();
    
    // restartRClearOutput
    @DefaultStringValue("Restart R Session and Clear Chunk Output")
    String restartRClearOutputLabel();
    @DefaultStringValue("Restart R and Clear _Output")
    String restartRClearOutputMenuLabel();
    @DefaultStringValue("Restart R session and clear chunk output")
    String restartRClearOutputDesc();
    
    // restartRRunAllChunks
    @DefaultStringValue("Restart R Session and Run All Chunks")
    String restartRRunAllChunksLabel();
    @DefaultStringValue("Restart R and Run _All Chunks")
    String restartRRunAllChunksMenuLabel();
    @DefaultStringValue("Restart R session and run all chunks")
    String restartRRunAllChunksDesc();
    
    // terminateR
    @DefaultStringValue("Terminate R Session")
    String terminateRLabel();
    @DefaultStringValue("_Terminate R...")
    String terminateRMenuLabel();
    @DefaultStringValue("Forcibly terminate R session")
    String terminateRDesc();
    
    // showPdfExternal
    @DefaultStringValue("Show PDF in External Viewer")
    String showPdfExternalLabel();
    @DefaultStringValue("Show PDF in External Viewer")
    String showPdfExternalMenuLabel();
    @DefaultStringValue("Show in an external PDF viewer window")
    String showPdfExternalDesc();
    
    // openHtmlExternal
    @DefaultStringValue("Open Page with Web Browser")
    String openHtmlExternalLabel();
    @DefaultStringValue("")
    String openHtmlExternalButtonLabel();
    @DefaultStringValue("View the page with the system web browser")
    String openHtmlExternalDesc();
    
    // saveHtmlPreviewAsLocalFile
    @DefaultStringValue("File on Local Computer...")
    String saveHtmlPreviewAsLocalFileMenuLabel();
    @DefaultStringValue("Download the page to a local file")
    String saveHtmlPreviewAsLocalFileDesc();
    
    // saveHtmlPreviewAs
    @DefaultStringValue("Save As")
    String saveHtmlPreviewAsButtonLabel();
    @DefaultStringValue("File on RStudio Server...")
    String saveHtmlPreviewAsMenuLabel();
    @DefaultStringValue("Save the page to another location")
    String saveHtmlPreviewAsDesc();
    
    // showHtmlPreviewLog
    @DefaultStringValue("Log")
    String showHtmlPreviewLogButtonLabel();
    @DefaultStringValue("Show the compilation log for this document")
    String showHtmlPreviewLogDesc();
    
    // refreshHtmlPreview
    @DefaultStringValue("Refresh the preview")
    String refreshHtmlPreviewDesc();
    
    // refreshPresentation
    @DefaultStringValue("Refresh the presentation")
    String refreshPresentationDesc();
    
    // presentationFullscreen
    @DefaultStringValue("Show presentation in full screen mode")
    String presentationFullscreenDesc();
    
    // presentationHome
    @DefaultStringValue("Go to the first slide")
    String presentationHomeDesc();
    
    // presentationNext
    @DefaultStringValue("Go to the next slide")
    String presentationNextDesc();
    
    // presentationPrev
    @DefaultStringValue("Go to the previous slide")
    String presentationPrevDesc();
    
    // presentationEdit
    @DefaultStringValue("Edit this slide of the presentation")
    String presentationEditDesc();
    
    // presentationViewInBrowser
    @DefaultStringValue("_View in Browser")
    String presentationViewInBrowserMenuLabel();
    @DefaultStringValue("View the presentation in an external web browser")
    String presentationViewInBrowserDesc();
    
    // presentationSaveAsStandalone
    @DefaultStringValue("_Save As Web Page...")
    String presentationSaveAsStandaloneMenuLabel();
    @DefaultStringValue("Save the presentation as a standalone web page")
    String presentationSaveAsStandaloneDesc();
    
    // clearPresentationCache
    @DefaultStringValue("Clear Knitr Cache...")
    String clearPresentationCacheMenuLabel();
    @DefaultStringValue("Clear knitr cache for this presentation")
    String clearPresentationCacheDesc();
    
    // historySendToSource
    @DefaultStringValue("Insert Command into Document")
    String historySendToSourceLabel();
    @DefaultStringValue("To Source")
    String historySendToSourceButtonLabel();
    @DefaultStringValue("Insert into _Source")
    String historySendToSourceMenuLabel();
    @DefaultStringValue("Insert the selected commands into the current document (Shift+Enter)")
    String historySendToSourceDesc();
    
    // historySendToConsole
    @DefaultStringValue("Send Command to Console")
    String historySendToConsoleLabel();
    @DefaultStringValue("To Console")
    String historySendToConsoleButtonLabel();
    @DefaultStringValue("Send to _Console")
    String historySendToConsoleMenuLabel();
    @DefaultStringValue("Send the selected commands to the R console (Enter)")
    String historySendToConsoleDesc();
    
    // loadHistory
    @DefaultStringValue("")
    String loadHistoryButtonLabel();
    @DefaultStringValue("_Load History...")
    String loadHistoryMenuLabel();
    @DefaultStringValue("Load history from an existing file")
    String loadHistoryDesc();
    
    // saveHistory
    @DefaultStringValue("")
    String saveHistoryButtonLabel();
    @DefaultStringValue("Sa_ve History As...")
    String saveHistoryMenuLabel();
    @DefaultStringValue("Save history into a file")
    String saveHistoryDesc();
    
    // historyRemoveEntries
    @DefaultStringValue("")
    String historyRemoveEntriesButtonLabel();
    @DefaultStringValue("_Remove Entries...")
    String historyRemoveEntriesMenuLabel();
    @DefaultStringValue("Remove the selected history entries")
    String historyRemoveEntriesDesc();
    
    // clearHistory
    @DefaultStringValue("")
    String clearHistoryButtonLabel();
    @DefaultStringValue("Clear _All...")
    String clearHistoryMenuLabel();
    @DefaultStringValue("Clear all history entries")
    String clearHistoryDesc();
    
    // historyDismissResults
    @DefaultStringValue("Dismiss History Results")
    String historyDismissResultsLabel();
    @DefaultStringValue("Done")
    String historyDismissResultsButtonLabel();
    
    // historyShowContext
    @DefaultStringValue("Show In Context")
    String historyShowContextLabel();
    
    // historyDismissContext
    @DefaultStringValue("Dismiss History Context")
    String historyDismissContextLabel();
    @DefaultStringValue("« Back")
    String historyDismissContextButtonLabel();
    
    // nextPlot
    @DefaultStringValue("Show Next Plot")
    String nextPlotLabel();
    @DefaultStringValue("")
    String nextPlotButtonLabel();
    @DefaultStringValue("_Next Plot")
    String nextPlotMenuLabel();
    @DefaultStringValue("Next plot")
    String nextPlotDesc();
    
    // previousPlot
    @DefaultStringValue("Show Previous Plot")
    String previousPlotLabel();
    @DefaultStringValue("")
    String previousPlotButtonLabel();
    @DefaultStringValue("_Previous Plot")
    String previousPlotMenuLabel();
    @DefaultStringValue("Previous plot")
    String previousPlotDesc();
    
    // savePlotAsImage
    @DefaultStringValue("Save Plot As Image...")
    String savePlotAsImageLabel();
    @DefaultStringValue("Save as _Image...")
    String savePlotAsImageMenuLabel();
    @DefaultStringValue("Save the current plot as an image file")
    String savePlotAsImageDesc();
    
    // savePlotAsPdf
    @DefaultStringValue("Save Plot as PDF...")
    String savePlotAsPdfLabel();
    @DefaultStringValue("Save as P_DF...")
    String savePlotAsPdfMenuLabel();
    @DefaultStringValue("Save the current plot as a PDF file")
    String savePlotAsPdfDesc();
    
    // copyPlotToClipboard
    @DefaultStringValue("Copy Current Plot to Clipboard...")
    String copyPlotToClipboardLabel();
    @DefaultStringValue("Cop_y to Clipboard...")
    String copyPlotToClipboardMenuLabel();
    @DefaultStringValue("Copy the current plot to the clipboard")
    String copyPlotToClipboardDesc();
    
    // zoomPlot
    @DefaultStringValue("Zoom")
    String zoomPlotButtonLabel();
    @DefaultStringValue("_Zoom Plot...")
    String zoomPlotMenuLabel();
    @DefaultStringValue("View a larger version of the plot in a new window")
    String zoomPlotDesc();
    
    // removePlot
    @DefaultStringValue("Remove Current Plot...")
    String removePlotLabel();
    @DefaultStringValue("")
    String removePlotButtonLabel();
    @DefaultStringValue("_Remove Plot...")
    String removePlotMenuLabel();
    @DefaultStringValue("Remove the current plot")
    String removePlotDesc();
    
    // clearPlots
    @DefaultStringValue("Clear All Plots...")
    String clearPlotsLabel();
    @DefaultStringValue("")
    String clearPlotsButtonLabel();
    @DefaultStringValue("_Clear All...")
    String clearPlotsMenuLabel();
    @DefaultStringValue("Clear all Plots")
    String clearPlotsDesc();
    
    // refreshPlot
    @DefaultStringValue("Refresh Current Plot")
    String refreshPlotLabel();
    @DefaultStringValue("")
    String refreshPlotButtonLabel();
    @DefaultStringValue("Refresh")
    String refreshPlotMenuLabel();
    @DefaultStringValue("Refresh current plot")
    String refreshPlotDesc();
    
    // showManipulator
    @DefaultStringValue("Show Manipulator for Current Plot")
    String showManipulatorLabel();
    @DefaultStringValue("")
    String showManipulatorButtonLabel();
    @DefaultStringValue("Show _Manipulator")
    String showManipulatorMenuLabel();
    @DefaultStringValue("Show the manipulator for this plot")
    String showManipulatorDesc();
    
    // clearWorkspace
    @DefaultStringValue("_Clear Workspace...")
    String clearWorkspaceMenuLabel();
    @DefaultStringValue("Clear objects from the workspace")
    String clearWorkspaceDesc();
    
    // loadWorkspace
    @DefaultStringValue("_Load Workspace...")
    String loadWorkspaceMenuLabel();
    @DefaultStringValue("Load workspace")
    String loadWorkspaceDesc();
    
    // saveWorkspace
    @DefaultStringValue("_Save Workspace As...")
    String saveWorkspaceMenuLabel();
    @DefaultStringValue("Save workspace as")
    String saveWorkspaceDesc();
    
    // importDatasetFromFile
    @DefaultStringValue("Import Dataset from File...")
    String importDatasetFromFileLabel();
    @DefaultStringValue("From _Local File...")
    String importDatasetFromFileMenuLabel();
    
    // importDatasetFromURL
    @DefaultStringValue("Import Dataset from URL...")
    String importDatasetFromURLLabel();
    @DefaultStringValue("From _Web URL...")
    String importDatasetFromURLMenuLabel();
    
    // importDatasetFromCsv
    @DefaultStringValue("From CSV")
    String importDatasetFromCsvLabel();
    @DefaultStringValue("From _CSV")
    String importDatasetFromCsvMenuLabel();
    
    // importDatasetFromCsvUsingReadr
    @DefaultStringValue("From Text (readr)...")
    String importDatasetFromCsvUsingReadrLabel();
    @DefaultStringValue("From Text (_readr)...")
    String importDatasetFromCsvUsingReadrMenuLabel();
    
    // importDatasetFromCsvUsingBase
    @DefaultStringValue("From Text (base)...")
    String importDatasetFromCsvUsingBaseLabel();
    @DefaultStringValue("From Text (_base)...")
    String importDatasetFromCsvUsingBaseMenuLabel();
    
    // importDatasetFromSAV
    @DefaultStringValue("Import Dataset from SPSS...")
    String importDatasetFromSAVLabel();
    @DefaultStringValue("From _SPSS...")
    String importDatasetFromSAVMenuLabel();
    
    // importDatasetFromSAS
    @DefaultStringValue("Import Dataset from SAS...")
    String importDatasetFromSASLabel();
    @DefaultStringValue("From S_AS...")
    String importDatasetFromSASMenuLabel();
    
    // importDatasetFromStata
    @DefaultStringValue("Import Dataset from Stata...")
    String importDatasetFromStataLabel();
    @DefaultStringValue("From S_tata...")
    String importDatasetFromStataMenuLabel();
    
    // importDatasetFromXLS
    @DefaultStringValue("Import Dataset from Excel...")
    String importDatasetFromXLSLabel();
    @DefaultStringValue("From _Excel...")
    String importDatasetFromXLSMenuLabel();
    
    // refreshWorkspace
    @DefaultStringValue("")
    String refreshWorkspaceButtonLabel();
    @DefaultStringValue("Refresh")
    String refreshWorkspaceMenuLabel();
    @DefaultStringValue("Refresh Workspace")
    String refreshWorkspaceDesc();
    
    // installPackage
    @DefaultStringValue("Install Packages...")
    String installPackageLabel();
    @DefaultStringValue("Install")
    String installPackageButtonLabel();
    @DefaultStringValue("Install Pac_kages...")
    String installPackageMenuLabel();
    @DefaultStringValue("Install R packages")
    String installPackageDesc();
    
    // updatePackages
    @DefaultStringValue("Update Packages...")
    String updatePackagesLabel();
    @DefaultStringValue("Update")
    String updatePackagesButtonLabel();
    @DefaultStringValue("Check for Package _Updates...")
    String updatePackagesMenuLabel();
    @DefaultStringValue("Check for package updates")
    String updatePackagesDesc();
    
    // refreshPackages
    @DefaultStringValue("Refresh Packages Pane")
    String refreshPackagesLabel();
    @DefaultStringValue("")
    String refreshPackagesButtonLabel();
    @DefaultStringValue("Refresh Package listing")
    String refreshPackagesDesc();
    
    // packratBootstrap
    @DefaultStringValue("Packrat")
    String packratBootstrapButtonLabel();
    @DefaultStringValue("_Initialize Packrat...")
    String packratBootstrapMenuLabel();
    @DefaultStringValue("Use packrat with this project")
    String packratBootstrapDesc();
    
    // packratClean
    @DefaultStringValue("_Clean Unused Packages...")
    String packratCleanMenuLabel();
    @DefaultStringValue("Remove unused packages from your packrat library")
    String packratCleanDesc();
    
    // packratHelp
    @DefaultStringValue("Using Packrat")
    String packratHelpMenuLabel();
    @DefaultStringValue("Help on using packrat with R projects")
    String packratHelpDesc();
    
    // packratOptions
    @DefaultStringValue("Options")
    String packratOptionsButtonLabel();
    @DefaultStringValue("Packrat _Options...")
    String packratOptionsMenuLabel();
    @DefaultStringValue("Configure packrat options for this project")
    String packratOptionsDesc();
    
    // packratBundle
    @DefaultStringValue("Bundle")
    String packratBundleButtonLabel();
    @DefaultStringValue("Export Project _Bundle...")
    String packratBundleMenuLabel();
    @DefaultStringValue("Bundle a Packrat Project")
    String packratBundleDesc();
    
    // packratCheckStatus
    @DefaultStringValue("Check Library _Status...")
    String packratCheckStatusMenuLabel();
    @DefaultStringValue("Check the status of the Packrat library")
    String packratCheckStatusDesc();
    
    // renvHelp
    @DefaultStringValue("Introduction to renv")
    String renvHelpMenuLabel();
    @DefaultStringValue("Learn how to use renv")
    String renvHelpDesc();
    
    // renvSnapshot
    @DefaultStringValue("Snapshot Library...")
    String renvSnapshotMenuLabel();
    @DefaultStringValue("Snapshot the state of your project library")
    String renvSnapshotDesc();
    
    // renvRestore
    @DefaultStringValue("Restore Library...")
    String renvRestoreMenuLabel();
    @DefaultStringValue("Restore your project library from renv.lock")
    String renvRestoreDesc();
    
    // versionControlOptions
    @DefaultStringValue("_Options...")
    String versionControlOptionsMenuLabel();
    @DefaultStringValue("Configure version control options")
    String versionControlOptionsDesc();
    
    // versionControlHelp
    @DefaultStringValue("_Using Version Control")
    String versionControlHelpMenuLabel();
    @DefaultStringValue("Help on using version control with RStudio")
    String versionControlHelpDesc();
    
    // versionControlShowRsaKey
    @DefaultStringValue("Show Public Key...")
    String versionControlShowRsaKeyLabel();
    @DefaultStringValue("Show Public Key...")
    String versionControlShowRsaKeyMenuLabel();
    @DefaultStringValue("Show RSA public key")
    String versionControlShowRsaKeyDesc();
    
    // versionControlProjectSetup
    @DefaultStringValue("Project _Setup...")
    String versionControlProjectSetupMenuLabel();
    @DefaultStringValue("Setup version control for the current project")
    String versionControlProjectSetupDesc();
    
    // showShellDialog
    @DefaultStringValue("Open Shell")
    String showShellDialogLabel();
    @DefaultStringValue("_Shell...")
    String showShellDialogMenuLabel();
    @DefaultStringValue("Execute shell commands")
    String showShellDialogDesc();
    
    // newTerminal
    @DefaultStringValue("New Terminal")
    String newTerminalLabel();
    @DefaultStringValue("_New Terminal")
    String newTerminalMenuLabel();
    @DefaultStringValue("Create a new terminal")
    String newTerminalDesc();
    
    // activateTerminal
    @DefaultStringValue("Move Focus to Terminal")
    String activateTerminalLabel();
    @DefaultStringValue("_Move Focus to Terminal")
    String activateTerminalMenuLabel();
    
    // renameTerminal
    @DefaultStringValue("Rename Terminal")
    String renameTerminalLabel();
    @DefaultStringValue("_Rename Terminal")
    String renameTerminalMenuLabel();
    @DefaultStringValue("Change terminal session name")
    String renameTerminalDesc();
    
    // closeTerminal
    @DefaultStringValue("Close Terminal")
    String closeTerminalLabel();
    @DefaultStringValue("")
    String closeTerminalButtonLabel();
    @DefaultStringValue("Cl_ose Terminal")
    String closeTerminalMenuLabel();
    @DefaultStringValue("Close current terminal session")
    String closeTerminalDesc();
    
    // closeAllTerminals
    @DefaultStringValue("Close All Terminals")
    String closeAllTerminalsLabel();
    @DefaultStringValue("")
    String closeAllTerminalsButtonLabel();
    @DefaultStringValue("Close _All Terminals")
    String closeAllTerminalsMenuLabel();
    
    // clearTerminalScrollbackBuffer
    @DefaultStringValue("Clear Terminal Buffer")
    String clearTerminalScrollbackBufferLabel();
    @DefaultStringValue("")
    String clearTerminalScrollbackBufferButtonLabel();
    @DefaultStringValue("_Clear Terminal Buffer")
    String clearTerminalScrollbackBufferMenuLabel();
    @DefaultStringValue("Clear terminal")
    String clearTerminalScrollbackBufferDesc();
    
    // previousTerminal
    @DefaultStringValue("Previous Terminal")
    String previousTerminalLabel();
    @DefaultStringValue("")
    String previousTerminalButtonLabel();
    @DefaultStringValue("_Previous Terminal")
    String previousTerminalMenuLabel();
    @DefaultStringValue("Show previous terminal")
    String previousTerminalDesc();
    
    // nextTerminal
    @DefaultStringValue("Next Terminal")
    String nextTerminalLabel();
    @DefaultStringValue("")
    String nextTerminalButtonLabel();
    @DefaultStringValue("Ne_xt Terminal")
    String nextTerminalMenuLabel();
    @DefaultStringValue("Show next terminal")
    String nextTerminalDesc();
    
    // showTerminalInfo
    @DefaultStringValue("Terminal Diagnostics...")
    String showTerminalInfoLabel();
    @DefaultStringValue("")
    String showTerminalInfoButtonLabel();
    @DefaultStringValue("Terminal _Diagnostics...")
    String showTerminalInfoMenuLabel();
    @DefaultStringValue("Show info on current terminal")
    String showTerminalInfoDesc();
    
    // interruptTerminal
    @DefaultStringValue("Send Interrupt")
    String interruptTerminalLabel();
    @DefaultStringValue("")
    String interruptTerminalButtonLabel();
    @DefaultStringValue("_Interrupt Current Terminal")
    String interruptTerminalMenuLabel();
    @DefaultStringValue("Send Ctrl+C to Current Terminal")
    String interruptTerminalDesc();
    
    // sendTerminalToEditor
    @DefaultStringValue("Copy Terminal to Editor")
    String sendTerminalToEditorLabel();
    @DefaultStringValue("")
    String sendTerminalToEditorButtonLabel();
    @DefaultStringValue("Copy Terminal to _Editor")
    String sendTerminalToEditorMenuLabel();
    @DefaultStringValue("Copy current terminal's buffer to a new editor buffer")
    String sendTerminalToEditorDesc();
    
    // sendToTerminal
    @DefaultStringValue("Send Selection to Terminal")
    String sendToTerminalLabel();
    @DefaultStringValue("")
    String sendToTerminalButtonLabel();
    @DefaultStringValue("Send to _Terminal")
    String sendToTerminalMenuLabel();
    @DefaultStringValue("Send the current line or selection to terminal")
    String sendToTerminalDesc();
    
    // sendFilenameToTerminal
    @DefaultStringValue("Send _Filename to Terminal")
    String sendFilenameToTerminalMenuLabel();
    
    // openNewTerminalAtEditorLocation
    @DefaultStringValue("_Open New Terminal at File Location")
    String openNewTerminalAtEditorLocationMenuLabel();
    
    // openNewTerminalAtFilePaneLocation
    @DefaultStringValue("Open New Terminal Here")
    String openNewTerminalAtFilePaneLocationMenuLabel();
    
    // setTerminalToCurrentDirectory
    @DefaultStringValue("_Go to Current Directory")
    String setTerminalToCurrentDirectoryMenuLabel();
    
    // browseAddins
    @DefaultStringValue("Browse Addins")
    String browseAddinsLabel();
    @DefaultStringValue("_Browse Addins...")
    String browseAddinsMenuLabel();
    @DefaultStringValue("Browse addins")
    String browseAddinsDesc();
    
    // macPreferences
    @DefaultStringValue("_Preferences...")
    String macPreferencesMenuLabel();
    
    // showOptions
    @DefaultStringValue("_Global Options...")
    String showOptionsMenuLabel();
    
    // showCodeOptions
    @DefaultStringValue("Code Options...")
    String showCodeOptionsLabel();
    
    // showConsoleOptions
    @DefaultStringValue("Console Options...")
    String showConsoleOptionsLabel();
    
    // showAppearanceOptions
    @DefaultStringValue("Appearance Options...")
    String showAppearanceOptionsLabel();
    
    // paneLayout
    @DefaultStringValue("Pane Layout Options...")
    String paneLayoutLabel();
    @DefaultStringValue("Pane Layo_ut...")
    String paneLayoutMenuLabel();
    
    // showPackagesOptions
    @DefaultStringValue("Packages Options...")
    String showPackagesOptionsLabel();
    
    // showRMarkdownOptions
    @DefaultStringValue("R Markdown Options...")
    String showRMarkdownOptionsLabel();
    
    // showSweaveOptions
    @DefaultStringValue("Sweave Options...")
    String showSweaveOptionsLabel();
    
    // showSpellingOptions
    @DefaultStringValue("Spelling Options...")
    String showSpellingOptionsLabel();
    
    // showVcsOptions
    @DefaultStringValue("Git/SVN Version Control Options...")
    String showVcsOptionsLabel();
    
    // showPublishingOptions
    @DefaultStringValue("Publishing Options...")
    String showPublishingOptionsLabel();
    
    // showTerminalOptions
    @DefaultStringValue("_Terminal Options...")
    String showTerminalOptionsMenuLabel();
    
    // showAccessibilityOptions
    @DefaultStringValue("Accessibility _Options...")
    String showAccessibilityOptionsMenuLabel();
    
    // showPythonOptions
    @DefaultStringValue("Python Options...")
    String showPythonOptionsLabel();
    
    // modifyKeyboardShortcuts
    @DefaultStringValue("_Modify Keyboard Shortcuts...")
    String modifyKeyboardShortcutsMenuLabel();
    @DefaultStringValue("Modify keyboard shortcuts")
    String modifyKeyboardShortcutsDesc();
    
    // checkForUpdates
    @DefaultStringValue("Check for RStudio Updates")
    String checkForUpdatesLabel();
    @DefaultStringValue("Check for _Updates")
    String checkForUpdatesMenuLabel();
    
    // helpUsingRStudio
    @DefaultStringValue("RStudio _Docs")
    String helpUsingRStudioMenuLabel();
    
    // helpKeyboardShortcuts
    @DefaultStringValue("_Keyboard Shortcuts Help")
    String helpKeyboardShortcutsMenuLabel();
    
    // helpBack
    @DefaultStringValue("Previous Help Topic")
    String helpBackLabel();
    @DefaultStringValue("")
    String helpBackButtonLabel();
    @DefaultStringValue("Previous topic")
    String helpBackDesc();
    
    // helpForward
    @DefaultStringValue("Next Help Topic")
    String helpForwardLabel();
    @DefaultStringValue("")
    String helpForwardButtonLabel();
    @DefaultStringValue("Next topic")
    String helpForwardDesc();
    
    // helpHome
    @DefaultStringValue("Show R Help")
    String helpHomeLabel();
    @DefaultStringValue("")
    String helpHomeButtonLabel();
    @DefaultStringValue("R _Help")
    String helpHomeMenuLabel();
    @DefaultStringValue("Show R Help")
    String helpHomeDesc();
    
    // helpSearch
    @DefaultStringValue("Search R Hel_p")
    String helpSearchMenuLabel();
    
    // printHelp
    @DefaultStringValue("Print Help Topic")
    String printHelpLabel();
    @DefaultStringValue("")
    String printHelpButtonLabel();
    @DefaultStringValue("Print topic")
    String printHelpDesc();
    
    // clearHelpHistory
    @DefaultStringValue("Clear Help History")
    String clearHelpHistoryLabel();
    @DefaultStringValue("Clear history")
    String clearHelpHistoryMenuLabel();
    @DefaultStringValue("Clear history")
    String clearHelpHistoryDesc();
    
    // helpPopout
    @DefaultStringValue("Show Help in New Window")
    String helpPopoutLabel();
    @DefaultStringValue("")
    String helpPopoutButtonLabel();
    @DefaultStringValue("Show in new window")
    String helpPopoutDesc();
    
    // refreshHelp
    @DefaultStringValue("Refresh Help Topic")
    String refreshHelpLabel();
    @DefaultStringValue("Refresh")
    String refreshHelpMenuLabel();
    @DefaultStringValue("Refresh topic")
    String refreshHelpDesc();
    
    // tutorialPopout
    @DefaultStringValue("")
    String tutorialPopoutButtonLabel();
    @DefaultStringValue("Show in new window")
    String tutorialPopoutDesc();
    
    // tutorialBack
    @DefaultStringValue("")
    String tutorialBackButtonLabel();
    @DefaultStringValue("Go back")
    String tutorialBackDesc();
    
    // tutorialForward
    @DefaultStringValue("")
    String tutorialForwardButtonLabel();
    @DefaultStringValue("Go forward")
    String tutorialForwardDesc();
    
    // tutorialZoom
    @DefaultStringValue("Zoom")
    String tutorialZoomButtonLabel();
    @DefaultStringValue("View a larger version in a new window")
    String tutorialZoomDesc();
    
    // tutorialRefresh
    @DefaultStringValue("Refresh tutorial")
    String tutorialRefreshDesc();
    
    // tutorialStop
    @DefaultStringValue("")
    String tutorialStopButtonLabel();
    @DefaultStringValue("Stop tutorial")
    String tutorialStopDesc();
    
    // tutorialHome
    @DefaultStringValue("")
    String tutorialHomeButtonLabel();
    @DefaultStringValue("Return to home")
    String tutorialHomeDesc();
    
    // viewerPopout
    @DefaultStringValue("")
    String viewerPopoutButtonLabel();
    @DefaultStringValue("Show in new window")
    String viewerPopoutDesc();
    
    // viewerBack
    @DefaultStringValue("")
    String viewerBackButtonLabel();
    @DefaultStringValue("Go back")
    String viewerBackDesc();
    
    // viewerForward
    @DefaultStringValue("")
    String viewerForwardButtonLabel();
    @DefaultStringValue("Go forward")
    String viewerForwardDesc();
    
    // viewerZoom
    @DefaultStringValue("Zoom")
    String viewerZoomButtonLabel();
    @DefaultStringValue("View a larger version in a new window")
    String viewerZoomDesc();
    
    // viewerRefresh
    @DefaultStringValue("Refresh viewer")
    String viewerRefreshDesc();
    
    // viewerSaveAllAndRefresh
    @DefaultStringValue("Save source files and refresh viewer")
    String viewerSaveAllAndRefreshDesc();
    
    // viewerStop
    @DefaultStringValue("")
    String viewerStopButtonLabel();
    @DefaultStringValue("Stop application")
    String viewerStopDesc();
    
    // viewerClear
    @DefaultStringValue("")
    String viewerClearButtonLabel();
    @DefaultStringValue("Remove current viewer item")
    String viewerClearDesc();
    
    // viewerClearAll
    @DefaultStringValue("")
    String viewerClearAllButtonLabel();
    @DefaultStringValue("Clear all viewer items")
    String viewerClearAllDesc();
    
    // viewerSaveAsImage
    @DefaultStringValue("Save as Image...")
    String viewerSaveAsImageMenuLabel();
    @DefaultStringValue("Save as an image file")
    String viewerSaveAsImageDesc();
    
    // viewerSaveAsWebPage
    @DefaultStringValue("Save as Web Page...")
    String viewerSaveAsWebPageMenuLabel();
    @DefaultStringValue("Save as a standalone web page")
    String viewerSaveAsWebPageDesc();
    
    // viewerCopyToClipboard
    @DefaultStringValue("Copy to Clipboard...")
    String viewerCopyToClipboardMenuLabel();
    @DefaultStringValue("Copy to the system clipboard")
    String viewerCopyToClipboardDesc();
    
    // raiseException
    @DefaultStringValue("Raise E_xception")
    String raiseExceptionMenuLabel();
    
    // raiseException2
    @DefaultStringValue("Raise Exception _JS")
    String raiseException2MenuLabel();
    
    // showWarningBar
    @DefaultStringValue("Show warning bar")
    String showWarningBarMenuLabel();
    
    // showRequestLog
    @DefaultStringValue("_Request Log")
    String showRequestLogMenuLabel();
    @DefaultStringValue("Show internal request log")
    String showRequestLogDesc();
    
    // diagnosticsReport
    @DefaultStringValue("_Write Diagnostics Report")
    String diagnosticsReportMenuLabel();
    
    // openDeveloperConsole
    @DefaultStringValue("_Open Developer Console")
    String openDeveloperConsoleMenuLabel();
    
    // reloadUi
    @DefaultStringValue("Reload _UI")
    String reloadUiMenuLabel();
    
    // logFocusedElement
    @DefaultStringValue("Log focused element")
    String logFocusedElementMenuLabel();
    
    // debugDumpContents
    @DefaultStringValue("_Dump Editor Contents...")
    String debugDumpContentsMenuLabel();
    
    // debugImportDump
    @DefaultStringValue("_Import Editor Contents...")
    String debugImportDumpMenuLabel();
    
    // refreshSuperDevMode
    
    // newSession
    @DefaultStringValue("Open a New R Session")
    String newSessionLabel();
    @DefaultStringValue("")
    String newSessionButtonLabel();
    @DefaultStringValue("_New Session")
    String newSessionMenuLabel();
    @DefaultStringValue("Open a new R session")
    String newSessionDesc();
    
    // suspendSession
    @DefaultStringValue("Suspend R Session")
    String suspendSessionLabel();
    @DefaultStringValue("_Suspend R Session")
    String suspendSessionMenuLabel();
    
    // quitSession
    @DefaultStringValue("Quit the Current R Session")
    String quitSessionLabel();
    @DefaultStringValue("")
    String quitSessionButtonLabel();
    @DefaultStringValue("_Quit Session...")
    String quitSessionMenuLabel();
    @DefaultStringValue("Quit the current R session")
    String quitSessionDesc();
    
    // forceQuitSession
    @DefaultStringValue("Quit the Current R Session Even if Busy")
    String forceQuitSessionLabel();
    @DefaultStringValue("")
    String forceQuitSessionButtonLabel();
    @DefaultStringValue("Force Quit Session...")
    String forceQuitSessionMenuLabel();
    @DefaultStringValue("Quit the current R session even if busy")
    String forceQuitSessionDesc();
    
    // showSessionServerOptionsDialog
    @DefaultStringValue("Session Server Settings...")
    String showSessionServerOptionsDialogLabel();
    @DefaultStringValue("")
    String showSessionServerOptionsDialogButtonLabel();
    @DefaultStringValue("_Session Server Settings...")
    String showSessionServerOptionsDialogMenuLabel();
    @DefaultStringValue("Configure available session servers")
    String showSessionServerOptionsDialogDesc();
    
    // showAboutDialog
    @DefaultStringValue("About RStudio...")
    String showAboutDialogLabel();
    @DefaultStringValue("A_bout RStudio")
    String showAboutDialogMenuLabel();
    
    // showLicenseDialog
    @DefaultStringValue("Manage License...")
    String showLicenseDialogLabel();
    @DefaultStringValue("Ma_nage License...")
    String showLicenseDialogMenuLabel();
    
    // showLogFiles
    @DefaultStringValue("_Show Log Files")
    String showLogFilesMenuLabel();
    
    // updateCredentials
    @DefaultStringValue("_Update Credentials")
    String updateCredentialsMenuLabel();
    
    // rstudioCommunityForum
    @DefaultStringValue("RStudio Community _Forum")
    String rstudioCommunityForumMenuLabel();
    
    // rstudioSupport
    @DefaultStringValue("RStudio _Support")
    String rstudioSupportMenuLabel();
    
    // rstudioLicense
    @DefaultStringValue("RStudio _License")
    String rstudioLicenseMenuLabel();
    
    // buildAll
    @DefaultStringValue("Install and Restart")
    String buildAllLabel();
    @DefaultStringValue("Install and Restart")
    String buildAllButtonLabel();
    @DefaultStringValue("_Install and Restart")
    String buildAllMenuLabel();
    @DefaultStringValue("Install the package and restart R")
    String buildAllDesc();
    
    // rebuildAll
    @DefaultStringValue("Clean and Rebuild")
    String rebuildAllLabel();
    @DefaultStringValue("Clean and _Rebuild")
    String rebuildAllMenuLabel();
    @DefaultStringValue("Clean previous output and rebuild all")
    String rebuildAllDesc();
    
    // cleanAll
    @DefaultStringValue("Clean All")
    String cleanAllLabel();
    @DefaultStringValue("Clean")
    String cleanAllButtonLabel();
    @DefaultStringValue("_Clean All")
    String cleanAllMenuLabel();
    @DefaultStringValue("Clean all")
    String cleanAllDesc();
    
    // buildSourcePackage
    @DefaultStringValue("Build _Source Package")
    String buildSourcePackageMenuLabel();
    @DefaultStringValue("Build a source package")
    String buildSourcePackageDesc();
    
    // buildBinaryPackage
    @DefaultStringValue("Build Binar_y Package")
    String buildBinaryPackageMenuLabel();
    @DefaultStringValue("Build a binary package")
    String buildBinaryPackageDesc();
    
    // devtoolsLoadAll
    @DefaultStringValue("Execute devtools::load_all()")
    String devtoolsLoadAllLabel();
    @DefaultStringValue("_Load All")
    String devtoolsLoadAllMenuLabel();
    @DefaultStringValue("Execute devtools::load_all")
    String devtoolsLoadAllDesc();
    
    // roxygenizePackage
    @DefaultStringValue("Build Package Documentation")
    String roxygenizePackageLabel();
    @DefaultStringValue("_Document")
    String roxygenizePackageMenuLabel();
    @DefaultStringValue("Build package documentation")
    String roxygenizePackageDesc();
    
    // checkPackage
    @DefaultStringValue("Check")
    String checkPackageButtonLabel();
    @DefaultStringValue("_Check Package")
    String checkPackageMenuLabel();
    @DefaultStringValue("R CMD check")
    String checkPackageDesc();
    
    // testPackage
    @DefaultStringValue("_Test Package")
    String testPackageMenuLabel();
    @DefaultStringValue("Run tests for package")
    String testPackageDesc();
    
    // testTestthatFile
    @DefaultStringValue("Run testthat Tests")
    String testTestthatFileLabel();
    @DefaultStringValue("Run Tests")
    String testTestthatFileButtonLabel();
    @DefaultStringValue("Run tests for file")
    String testTestthatFileMenuLabel();
    @DefaultStringValue("Run tests using the testthat package")
    String testTestthatFileDesc();
    
    // testShinytestFile
    @DefaultStringValue("Run shinytest Test")
    String testShinytestFileLabel();
    @DefaultStringValue("Run Test")
    String testShinytestFileButtonLabel();
    @DefaultStringValue("Run test for file")
    String testShinytestFileMenuLabel();
    @DefaultStringValue("Run test using the shinytest package")
    String testShinytestFileDesc();
    
    // stopBuild
    @DefaultStringValue("Sto_p Build")
    String stopBuildMenuLabel();
    @DefaultStringValue("Stop the current build")
    String stopBuildDesc();
    
    // buildToolsProjectSetup
    @DefaultStringValue("Configure Build Tools...")
    String buildToolsProjectSetupLabel();
    @DefaultStringValue("Configure Build _Tools...")
    String buildToolsProjectSetupMenuLabel();
    @DefaultStringValue("Configure build tools")
    String buildToolsProjectSetupDesc();
    
    // refreshEnvironment
    @DefaultStringValue("_Refresh Environment")
    String refreshEnvironmentMenuLabel();
    @DefaultStringValue("Refresh the list of objects in the environment")
    String refreshEnvironmentDesc();
    
    // undoDummy
    @DefaultStringValue("_Undo")
    String undoDummyMenuLabel();
    
    // redoDummy
    @DefaultStringValue("Re_do")
    String redoDummyMenuLabel();
    
    // cutDummy
    @DefaultStringValue("Cu_t")
    String cutDummyMenuLabel();
    
    // copyDummy
    @DefaultStringValue("_Copy")
    String copyDummyMenuLabel();
    
    // pasteDummy
    @DefaultStringValue("_Paste")
    String pasteDummyMenuLabel();
    
    // pasteWithIndentDummy
    @DefaultStringValue("Pa_ste with Indent")
    String pasteWithIndentDummyMenuLabel();
    
    // yankBeforeCursor
    @DefaultStringValue("Yank Before Cursor")
    String yankBeforeCursorLabel();
    
    // yankAfterCursor
    @DefaultStringValue("Yank After Cursor")
    String yankAfterCursorLabel();
    
    // pasteLastYank
    @DefaultStringValue("Paste Last Yank")
    String pasteLastYankLabel();
    
    // insertAssignmentOperator
    @DefaultStringValue("Insert Assignment Operator")
    String insertAssignmentOperatorLabel();
    
    // insertPipeOperator
    @DefaultStringValue("Insert Pipe Operator")
    String insertPipeOperatorLabel();
    
    // openNextFileOnFilesystem
    @DefaultStringValue("Open Next File on Filesystem")
    String openNextFileOnFilesystemLabel();
    
    // openPreviousFileOnFilesystem
    @DefaultStringValue("Open Previous File on Filesystem")
    String openPreviousFileOnFilesystemLabel();
    
    // toggleSoftWrapMode
    @DefaultStringValue("Toggle Soft Wrap Mode")
    String toggleSoftWrapModeLabel();
    @DefaultStringValue("Soft _Wrap Long Lines")
    String toggleSoftWrapModeMenuLabel();
    
    // toggleRainbowParens
    @DefaultStringValue("Toggle Rainbow Parentheses Mode")
    String toggleRainbowParensLabel();
    @DefaultStringValue("Rain_bow Parentheses")
    String toggleRainbowParensMenuLabel();
    
    // maximizeConsole
    @DefaultStringValue("Maximize Console")
    String maximizeConsoleMenuLabel();
    
    // debugBreakpoint
    @DefaultStringValue("Toggle Breakpoint on Current Line")
    String debugBreakpointLabel();
    @DefaultStringValue("Toggle _Breakpoint")
    String debugBreakpointMenuLabel();
    @DefaultStringValue("Set or remove a breakpoint on the current line of code")
    String debugBreakpointDesc();
    
    // debugClearBreakpoints
    @DefaultStringValue("Clear All Breakpoints...")
    String debugClearBreakpointsLabel();
    @DefaultStringValue("Clear _All Breakpoints...")
    String debugClearBreakpointsMenuLabel();
    @DefaultStringValue("Remove all the breakpoints in the current project")
    String debugClearBreakpointsDesc();
    
    // debugContinue
    @DefaultStringValue("Continue Execution")
    String debugContinueLabel();
    @DefaultStringValue("Continue")
    String debugContinueButtonLabel();
    @DefaultStringValue("_Continue")
    String debugContinueMenuLabel();
    @DefaultStringValue("Continue execution until the next breakpoint is encountered")
    String debugContinueDesc();
    
    // debugStop
    @DefaultStringValue("Stop Debugging")
    String debugStopLabel();
    @DefaultStringValue("Stop")
    String debugStopButtonLabel();
    @DefaultStringValue("_Stop Debugging")
    String debugStopMenuLabel();
    @DefaultStringValue("Exit debug mode")
    String debugStopDesc();
    
    // debugStep
    @DefaultStringValue("Execute Next Line")
    String debugStepLabel();
    @DefaultStringValue("Next")
    String debugStepButtonLabel();
    @DefaultStringValue("E_xecute Next Line")
    String debugStepMenuLabel();
    @DefaultStringValue("Execute the next line of code")
    String debugStepDesc();
    
    // debugStepInto
    @DefaultStringValue("Step Into Function")
    String debugStepIntoLabel();
    @DefaultStringValue("")
    String debugStepIntoButtonLabel();
    @DefaultStringValue("Step _Into Function")
    String debugStepIntoMenuLabel();
    @DefaultStringValue("Step into the current function call")
    String debugStepIntoDesc();
    
    // debugFinish
    @DefaultStringValue("Finish Function/Loop")
    String debugFinishLabel();
    @DefaultStringValue("")
    String debugFinishButtonLabel();
    @DefaultStringValue("_Finish Function/Loop")
    String debugFinishMenuLabel();
    @DefaultStringValue("Execute the remainder of the current function or loop")
    String debugFinishDesc();
    
    // debugHelp
    @DefaultStringValue("Show Guide on Debugging with RStudio")
    String debugHelpLabel();
    @DefaultStringValue("Debugging _Help")
    String debugHelpMenuLabel();
    @DefaultStringValue("Guide to debugging features")
    String debugHelpDesc();
    
    // errorsMessage
    @DefaultStringValue("_Message Only")
    String errorsMessageMenuLabel();
    @DefaultStringValue("Print the error message when an unhandled error occurs")
    String errorsMessageDesc();
    
    // errorsTraceback
    @DefaultStringValue("_Error Inspector")
    String errorsTracebackMenuLabel();
    @DefaultStringValue("Show the error inspector when an unhandled error occurs")
    String errorsTracebackDesc();
    
    // errorsBreak
    @DefaultStringValue("_Break in Code")
    String errorsBreakMenuLabel();
    @DefaultStringValue("Break when any unhandled error occurs")
    String errorsBreakDesc();
    
    // startProfiler
    @DefaultStringValue("_Start Profiling")
    String startProfilerMenuLabel();
    @DefaultStringValue("Start profiling R code")
    String startProfilerDesc();
    
    // stopProfiler
    @DefaultStringValue("Stop Profiling")
    String stopProfilerButtonLabel();
    @DefaultStringValue("Stop Profilin_g")
    String stopProfilerMenuLabel();
    @DefaultStringValue("Stop profiling R code")
    String stopProfilerDesc();
    
    // profileCode
    @DefaultStringValue("Profile Current Line or Selection")
    String profileCodeLabel();
    @DefaultStringValue("")
    String profileCodeButtonLabel();
    @DefaultStringValue("_Profile Selected Line(s)")
    String profileCodeMenuLabel();
    @DefaultStringValue("Profile the current line or selection")
    String profileCodeDesc();
    
    // gotoProfileSource
    @DefaultStringValue("Go To Profile Sources")
    String gotoProfileSourceLabel();
    @DefaultStringValue("")
    String gotoProfileSourceButtonLabel();
    @DefaultStringValue("Open sources associated with the selection")
    String gotoProfileSourceDesc();
    
    // profileCodeWithoutFocus
    @DefaultStringValue("Profile Current Line or Selection Without Focus")
    String profileCodeWithoutFocusLabel();
    
    // openProfile
    @DefaultStringValue("Open Profile...")
    String openProfileLabel();
    @DefaultStringValue("_Open Profile...")
    String openProfileMenuLabel();
    @DefaultStringValue("Opens a profile from a file")
    String openProfileDesc();
    
    // saveProfileAs
    @DefaultStringValue("Save Profile As...")
    String saveProfileAsLabel();
    @DefaultStringValue("")
    String saveProfileAsButtonLabel();
    @DefaultStringValue("_Save Profile As...")
    String saveProfileAsMenuLabel();
    @DefaultStringValue("Saves current profile into a file")
    String saveProfileAsDesc();
    
    // openProfileInBrowser
    @DefaultStringValue("Open Profile in Browser...")
    String openProfileInBrowserLabel();
    @DefaultStringValue("")
    String openProfileInBrowserButtonLabel();
    @DefaultStringValue("_Open Profile in Browser")
    String openProfileInBrowserMenuLabel();
    @DefaultStringValue("Opens current profile in a web browser")
    String openProfileInBrowserDesc();
    
    // profileHelp
    @DefaultStringValue("Show Guide on Profiling with RStudio")
    String profileHelpLabel();
    @DefaultStringValue("Profiling _Help")
    String profileHelpMenuLabel();
    @DefaultStringValue("Guide to profiling features")
    String profileHelpDesc();
    
    // reloadShinyApp
    @DefaultStringValue("Reload Shiny Application")
    String reloadShinyAppLabel();
    @DefaultStringValue("")
    String reloadShinyAppButtonLabel();
    @DefaultStringValue("Reload")
    String reloadShinyAppMenuLabel();
    @DefaultStringValue("Reload the Shiny application")
    String reloadShinyAppDesc();
    
    // shinyRunInPane
    @DefaultStringValue("Run Shiny Application in New Pane")
    String shinyRunInPaneLabel();
    @DefaultStringValue("Run in Viewer Pane")
    String shinyRunInPaneMenuLabel();
    @DefaultStringValue("Run the Shiny application in an RStudio pane")
    String shinyRunInPaneDesc();
    
    // shinyRunInViewer
    @DefaultStringValue("Run Shiny Application in RStudio Viewer")
    String shinyRunInViewerLabel();
    @DefaultStringValue("Run in Window")
    String shinyRunInViewerMenuLabel();
    @DefaultStringValue("Run the Shiny application in an RStudio viewer window")
    String shinyRunInViewerDesc();
    
    // shinyRunInBrowser
    @DefaultStringValue("Run Shiny Application in Web Browser")
    String shinyRunInBrowserLabel();
    @DefaultStringValue("Run External")
    String shinyRunInBrowserMenuLabel();
    @DefaultStringValue("Run the Shiny application in the system's default Web browser")
    String shinyRunInBrowserDesc();
    
    // shinyRecordTest
    @DefaultStringValue("Record a test for Shiny")
    String shinyRecordTestLabel();
    @DefaultStringValue("Record Test")
    String shinyRecordTestButtonLabel();
    @DefaultStringValue("Record Test")
    String shinyRecordTestMenuLabel();
    @DefaultStringValue("Record test for Shiny application")
    String shinyRecordTestDesc();
    
    // shinyRunAllTests
    @DefaultStringValue("Run tests for Shiny application")
    String shinyRunAllTestsLabel();
    @DefaultStringValue("Run Tests")
    String shinyRunAllTestsButtonLabel();
    @DefaultStringValue("Run Tests")
    String shinyRunAllTestsMenuLabel();
    @DefaultStringValue("Run tests for Shiny application")
    String shinyRunAllTestsDesc();
    
    // shinyCompareTest
    @DefaultStringValue("Compare test results for Shiny application")
    String shinyCompareTestLabel();
    @DefaultStringValue("Compare Results")
    String shinyCompareTestButtonLabel();
    @DefaultStringValue("Compare Results")
    String shinyCompareTestMenuLabel();
    @DefaultStringValue("Compare test results for Shiny application")
    String shinyCompareTestDesc();
    
    // reloadPlumberAPI
    @DefaultStringValue("Reload Plumber API")
    String reloadPlumberAPILabel();
    @DefaultStringValue("")
    String reloadPlumberAPIButtonLabel();
    @DefaultStringValue("Reload")
    String reloadPlumberAPIMenuLabel();
    @DefaultStringValue("Reload the Plumber API")
    String reloadPlumberAPIDesc();
    
    // plumberRunInPane
    @DefaultStringValue("Run Plumber API in New Pane")
    String plumberRunInPaneLabel();
    @DefaultStringValue("Run in Viewer Pane")
    String plumberRunInPaneMenuLabel();
    @DefaultStringValue("Run the Plumber API in an RStudio pane")
    String plumberRunInPaneDesc();
    
    // plumberRunInViewer
    @DefaultStringValue("Run Plumber API in RStudio Viewer")
    String plumberRunInViewerLabel();
    @DefaultStringValue("Run in Window")
    String plumberRunInViewerMenuLabel();
    @DefaultStringValue("Run the Plumber API in an RStudio viewer window")
    String plumberRunInViewerDesc();
    
    // plumberRunInBrowser
    @DefaultStringValue("Run Plumber API in Web Browser")
    String plumberRunInBrowserLabel();
    @DefaultStringValue("Run External")
    String plumberRunInBrowserMenuLabel();
    @DefaultStringValue("Run the Plumber API in the system's default Web browser")
    String plumberRunInBrowserDesc();
    
    // rsconnectDeploy
    @DefaultStringValue("P_ublish...")
    String rsconnectDeployMenuLabel();
    @DefaultStringValue("Publish the application or document")
    String rsconnectDeployDesc();
    
    // rsconnectConfigure
    @DefaultStringValue("_Configure Application...")
    String rsconnectConfigureMenuLabel();
    @DefaultStringValue("Configure the application")
    String rsconnectConfigureDesc();
    
    // rsconnectManageAccounts
    @DefaultStringValue("_Manage Accounts...")
    String rsconnectManageAccountsMenuLabel();
    @DefaultStringValue("Connect or disconnect accounts")
    String rsconnectManageAccountsDesc();
    
    // showGpuDiagnostics
    @DefaultStringValue("Show _GPU Diagnostics")
    String showGpuDiagnosticsMenuLabel();
    
    // toggleEditorTokenInfo
    @DefaultStringValue("_Toggle Editor Token Information")
    String toggleEditorTokenInfoMenuLabel();
    
    // showDomElements
    @DefaultStringValue("_Show DOM Elements")
    String showDomElementsMenuLabel();
    
    // newConnection
    @DefaultStringValue("New Connection")
    String newConnectionButtonLabel();
    @DefaultStringValue("New Connection...")
    String newConnectionMenuLabel();
    @DefaultStringValue("Create a new connection")
    String newConnectionDesc();
    
    // removeConnection
    @DefaultStringValue("")
    String removeConnectionButtonLabel();
    @DefaultStringValue("Remove Connection...")
    String removeConnectionMenuLabel();
    @DefaultStringValue("Remove connection from the connection history")
    String removeConnectionDesc();
    
    // disconnectConnection
    @DefaultStringValue("Disconnect")
    String disconnectConnectionMenuLabel();
    @DefaultStringValue("Disconnect from a connection")
    String disconnectConnectionDesc();
    
    // refreshConnection
    @DefaultStringValue("Refresh Connection Data")
    String refreshConnectionLabel();
    @DefaultStringValue("Refresh")
    String refreshConnectionMenuLabel();
    @DefaultStringValue("Refresh data")
    String refreshConnectionDesc();
    
    // sparkLog
    @DefaultStringValue("View Spark Log")
    String sparkLogLabel();
    @DefaultStringValue("Log")
    String sparkLogButtonLabel();
    @DefaultStringValue("Spark Log")
    String sparkLogMenuLabel();
    @DefaultStringValue("View the log for the Spark connection")
    String sparkLogDesc();
    
    // sparkUI
    @DefaultStringValue("SparkUI")
    String sparkUIButtonLabel();
    @DefaultStringValue("SparkUI")
    String sparkUIMenuLabel();
    @DefaultStringValue("View the browser UI for the Spark connection")
    String sparkUIDesc();
    
    // sparkHelp
    @DefaultStringValue("")
    String sparkHelpButtonLabel();
    @DefaultStringValue("Using Spark with RStudio")
    String sparkHelpMenuLabel();
    @DefaultStringValue("Help on using Spark with RStudio")
    String sparkHelpDesc();
    
    // startJob
    @DefaultStringValue("Start Local Job")
    String startJobButtonLabel();
    @DefaultStringValue("_Start Local Job...")
    String startJobMenuLabel();
    @DefaultStringValue("Run a background local job")
    String startJobDesc();
    
    // sourceAsJob
    @DefaultStringValue("Source as Local Job...")
    String sourceAsJobMenuLabel();
    @DefaultStringValue("Run the current R script as a local job")
    String sourceAsJobDesc();
    
    // clearJobs
    @DefaultStringValue("_Clear Local Jobs")
    String clearJobsMenuLabel();
    @DefaultStringValue("Clean up all completed local jobs")
    String clearJobsDesc();
    
    // runSelectionAsJob
    @DefaultStringValue("Ru_n Selection as Local Job")
    String runSelectionAsJobMenuLabel();
    @DefaultStringValue("Run the selected code as a local job")
    String runSelectionAsJobDesc();
    
    // startLauncherJob
    @DefaultStringValue("Start Launcher Job")
    String startLauncherJobButtonLabel();
    @DefaultStringValue("Start Launcher _Job...")
    String startLauncherJobMenuLabel();
    @DefaultStringValue("Run a background job on a cluster")
    String startLauncherJobDesc();
    
    // sourceAsLauncherJob
    @DefaultStringValue("Source as Launcher Job...")
    String sourceAsLauncherJobMenuLabel();
    @DefaultStringValue("Run the current R script on a cluster")
    String sourceAsLauncherJobDesc();
    
    // runSelectionAsLauncherJob
    @DefaultStringValue("Run Selection as _Launcher Job")
    String runSelectionAsLauncherJobMenuLabel();
    @DefaultStringValue("Run the selected code as a launcher job")
    String runSelectionAsLauncherJobDesc();
    
    // sortLauncherJobsRecorded
    @DefaultStringValue("Sort by Submission Time")
    String sortLauncherJobsRecordedMenuLabel();
    @DefaultStringValue("Sort jobs by time submitted")
    String sortLauncherJobsRecordedDesc();
    
    // sortLauncherJobsState
    @DefaultStringValue("Sort by Job State")
    String sortLauncherJobsStateMenuLabel();
    @DefaultStringValue("Sort jobs by current state")
    String sortLauncherJobsStateDesc();
    
    // showFileMenu
    @DefaultStringValue("Show File Menu")
    String showFileMenuMenuLabel();
    
    // showEditMenu
    @DefaultStringValue("Show Edit Menu")
    String showEditMenuMenuLabel();
    
    // showCodeMenu
    @DefaultStringValue("Show Code Menu")
    String showCodeMenuMenuLabel();
    
    // showViewMenu
    @DefaultStringValue("Show View Menu")
    String showViewMenuMenuLabel();
    
    // showPlotsMenu
    @DefaultStringValue("Show Plots Menu")
    String showPlotsMenuMenuLabel();
    
    // showSessionMenu
    @DefaultStringValue("Show Session Menu")
    String showSessionMenuMenuLabel();
    
    // showBuildMenu
    @DefaultStringValue("Show Build Menu")
    String showBuildMenuMenuLabel();
    
    // showDebugMenu
    @DefaultStringValue("Show Debug Menu")
    String showDebugMenuMenuLabel();
    
    // showProfileMenu
    @DefaultStringValue("Show Profile Menu")
    String showProfileMenuMenuLabel();
    
    // showToolsMenu
    @DefaultStringValue("Show Tools Menu")
    String showToolsMenuMenuLabel();
    
    // showHelpMenu
    @DefaultStringValue("Show Help Menu")
    String showHelpMenuMenuLabel();
    
    // editUserPrefs
    @DefaultStringValue("_Edit User Prefs File")
    String editUserPrefsMenuLabel();
    
    // clearUserPrefs
    @DefaultStringValue("_Clear User Prefs")
    String clearUserPrefsMenuLabel();
    
    // viewAllPrefs
    @DefaultStringValue("_View All Prefs")
    String viewAllPrefsMenuLabel();
    
    // toggleScreenReaderSupport
    @DefaultStringValue("_Screen Reader Support")
    String toggleScreenReaderSupportMenuLabel();
    
    // showAccessibilityHelp
    @DefaultStringValue("Accessibility _Help...")
    String showAccessibilityHelpMenuLabel();
    
    // toggleTabKeyMovesFocus
    @DefaultStringValue("_Tab Key Always Moves Focus")
    String toggleTabKeyMovesFocusMenuLabel();
    
    // focusMainToolbar
    @DefaultStringValue("Focus _Main Toolbar")
    String focusMainToolbarMenuLabel();
    
    // focusConsoleOutputEnd
    @DefaultStringValue("_Focus Console Output")
    String focusConsoleOutputEndMenuLabel();
    
    // focusNextPane
    @DefaultStringValue("_Focus Next Pane")
    String focusNextPaneMenuLabel();
    
    // focusPreviousPane
    @DefaultStringValue("_Focus Previous Pane")
    String focusPreviousPaneMenuLabel();
    
    // signOut
    @DefaultStringValue("Sign Out")
    String signOutLabel();
    @DefaultStringValue("")
    String signOutButtonLabel();
    @DefaultStringValue("Sign Ou_t")
    String signOutMenuLabel();
    @DefaultStringValue("Sign out from RStudio")
    String signOutDesc();
    
    // loadServerHome
    @DefaultStringValue("")
    String loadServerHomeButtonLabel();
    @DefaultStringValue("RStudio Server _Home")
    String loadServerHomeMenuLabel();
    
    // speakEditorLocation
    @DefaultStringValue("Speak Text _Editor Location")
    String speakEditorLocationMenuLabel();
    
    // focusLeftSeparator
    @DefaultStringValue("A_djust Left Splitter")
    String focusLeftSeparatorMenuLabel();
    
    // focusRightSeparator
    @DefaultStringValue("Ad_just Right Splitter")
    String focusRightSeparatorMenuLabel();
    
    // focusCenterSeparator
    @DefaultStringValue("Adjust Center S_plitter")
    String focusCenterSeparatorMenuLabel();
    
    // focusSourceColumnSeparator
    @DefaultStringValue("Adjust Source Column Spli_tter")
    String focusSourceColumnSeparatorMenuLabel();
    
    // showShortcutCommand
    @DefaultStringValue("Show _Keyboard Shortcut Commands")
    String showShortcutCommandMenuLabel();
    
    // showCommandPalette
    @DefaultStringValue("Show Command Palette")
    String showCommandPaletteLabel();
    @DefaultStringValue("Show _Command Palette")
    String showCommandPaletteMenuLabel();
    
    // clearCommandPaletteMru
    @DefaultStringValue("Clear Recently Executed Command List")
    String clearCommandPaletteMruMenuLabel();
    
    // freeUnusedMemory
    @DefaultStringValue("_Free Unused R Memory")
    String freeUnusedMemoryMenuLabel();
    
    // showMemoryUsageReport
    @DefaultStringValue("Memory Usage _Report...")
    String showMemoryUsageReportMenuLabel();
    
    // toggleShowMemoryUsage
    @DefaultStringValue("Toggle Memory Usage Display in Environment Pane")
    String toggleShowMemoryUsageLabel();
    @DefaultStringValue("_Show Current Memory Usage")
    String toggleShowMemoryUsageMenuLabel();
    
}