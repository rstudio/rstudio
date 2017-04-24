/*
 * SessionClientEvent.hpp
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

#ifndef SESSION_SESSION_CLIENT_EVENT_HPP
#define SESSION_SESSION_CLIENT_EVENT_HPP

#include <string>

#include <core/json/Json.hpp>

namespace rstudio {
namespace core {
   class FilePath;
}
}

namespace rstudio {
namespace session {
   
namespace client_events {
   
extern const int kConsolePrompt;
extern const int kConsoleWriteOutput;
extern const int kConsoleWriteError ;
extern const int kShowErrorMessage;
extern const int kShowHelp;
extern const int kBrowseUrl;
extern const int kShowEditor;
extern const int kChooseFile;
extern const int kQuit;
extern const int kSuicide;
extern const int kAbendWarning;
extern const int kBusy;
extern const int kFileChanged;
extern const int kWorkingDirChanged;
extern const int kPlotsStateChanged;
extern const int kViewData;
extern const int kPackageStatusChanged;
extern const int kPackageStateChanged;
extern const int kLocator;
extern const int kConsoleResetHistory;
extern const int kSessionSerialization;
extern const int kHistoryEntriesAdded;
extern const int kQuotaStatus;
extern const int kFileEdit;
extern const int kShowContent;
extern const int kShowData;
extern const int kAsyncCompletion;
extern const int kSaveActionChanged;
extern const int kConsoleWritePrompt;
extern const int kConsoleWriteInput;
extern const int kShowWarningBar;
extern const int kOpenProjectError;
extern const int kVcsRefresh;
extern const int kAskPass;
extern const int kConsoleProcessOutput;
extern const int kConsoleProcessExit;
extern const int kListChanged;
extern const int kConsoleProcessCreated;
extern const int kUiPrefsChanged;
extern const int kHandleUnsavedChanges;
extern const int kConsoleProcessPrompt;
extern const int kConsoleProcessCreated;
extern const int kHTMLPreviewStartedEvent;
extern const int kHTMLPreviewOutputEvent;
extern const int kHTMLPreviewCompletedEvent;
extern const int kCompilePdfStartedEvent;
extern const int kCompilePdfOutputEvent;
extern const int kCompilePdfErrorsEvent;
extern const int kCompilePdfCompletedEvent;
extern const int kSynctexEditFile;
extern const int kFindResult;
extern const int kFindOperationEnded;
extern const int kRPubsUploadStatus;
extern const int kBuildStarted;
extern const int kBuildOutput;
extern const int kBuildCompleted;
extern const int kBuildErrors;
extern const int kDirectoryNavigate;
extern const int kDeferredInitCompleted;
extern const int kPlotsZoomSizeChanged;
extern const int kSourceCppStarted;
extern const int kSourceCppCompleted;
extern const int kLoadedPackageUpdates;
extern const int kActivatePane;
extern const int kShowPresentationPane;
extern const int kEnvironmentRefresh;
extern const int kContextDepthChanged;
extern const int kEnvironmentAssigned;
extern const int kEnvironmentRemoved;
extern const int kBrowserLineChanged;
extern const int kPackageLoaded;
extern const int kPackageUnloaded;
extern const int kPresentationPaneRequestCompleted;
extern const int kUnhandledError;
extern const int kErrorHandlerChanged;
extern const int kViewerNavigate;
extern const int kSourceExtendedTypeDetected;
extern const int kShinyViewer;
extern const int kDebugSourceCompleted;
extern const int kRmdRenderStarted;
extern const int kRmdRenderOutput;
extern const int kRmdRenderCompleted;
extern const int kRmdTemplateDiscovered;
extern const int kRmdTemplateDiscoveryCompleted;
extern const int kRmdShinyDocStarted;
extern const int kRmdRSConnectDeploymentOutput;
extern const int kRmdRSConnectDeploymentCompleted;
extern const int kRmdRSConnectDeploymentFailed;
extern const int kUserPrompt;
extern const int kInstallRtools;
extern const int kInstallShiny;
extern const int kSuspendAndRestart;
extern const int kDataViewChanged;
extern const int kViewFunction;
extern const int kMarkersChanged;
extern const int kEnableRStudioConnect;
extern const int kUpdateGutterMarkers;
extern const int kSnippetsChanged;
extern const int kJumpToFunction;
extern const int kCollabEditStarted;
extern const int kSessionCountChanged;
extern const int kCollabEditEnded;
extern const int kProjectUsersChanged;
extern const int kRVersionsChanged;
extern const int kShinyGadgetDialog;
extern const int kRmdParamsReady;
extern const int kRegisterUserCommand;
extern const int kSendToConsole;
extern const int kUserFollowStarted;
extern const int kUserFollowEnded;
extern const int kProjectAccessRevoked;
extern const int kCollabEditSaved;
extern const int kAddinRegistryUpdated;
extern const int kChunkOutput;
extern const int kChunkOutputFinished;
extern const int kRprofStarted;
extern const int kRprofStopped;
extern const int kRprofCreated;
extern const int kEditorCommand;
extern const int kPreviewRmd;
extern const int kWebsiteFileSaved;
extern const int kChunkPlotRefreshed;
extern const int kChunkPlotRefreshFinished;
extern const int kReloadWithLastChanceSave;
extern const int kConnectionUpdated;
extern const int kEnableConnections;
extern const int kConnectionListChanged;
extern const int kActiveConnectionsChanged;
extern const int kConnectionOpened;
extern const int kNotebookRangeExecuted;
extern const int kChunkExecStateChanged;
extern const int kNavigateShinyFrame;
extern const int kUpdateNewConnectionDialog;
extern const int kProjectTemplateRegistryUpdated;
extern const int kTerminalSubprocs;
extern const int kPackageExtensionIndexingCompleted;
extern const int kRStudioAPIShowDialog;
extern const int kRStudioAPIShowDialogCompleted;
extern const int kObjectExplorerEvent;
}
   
class ClientEvent
{   
public:
   explicit ClientEvent(int type)
   {
      init(type, core::json::Value());
   }
   
   ClientEvent(int type, const core::json::Value& data)
   {
      init(type, data);
   }
   
   ClientEvent(int type, const char* data)
   {
      init(type, core::json::Value(std::string(data)));
   }
   
   ClientEvent(int type, const std::string& data)
   {
      init(type, core::json::Value(data));
   }
   
   ClientEvent(int type, bool data)
   {
      core::json::Object boolObject ;
      boolObject["value"] = data;
      init(type, boolObject);
   }
      
   // COPYING: via compiler (copyable members)

public:
   int type() const { return type_; }
   std::string typeName() const;
   const core::json::Value& data() const { return data_; }
   const std::string& id() const { return id_; }
   
   void asJsonObject(int id, core::json::Object* pObject) const;
     
private:
   void init(int type, const core::json::Value& data);
  
private:
   int type_ ;
   core::json::Value data_ ;
   std::string id_;
};

ClientEvent showEditorEvent(const std::string& content,
                            bool isRCode,
                            bool lineWrapping);

ClientEvent browseUrlEvent(const std::string& url,
                           const std::string& window = "_blank");
   
ClientEvent showErrorMessageEvent(const std::string& title,
                                  const std::string& message);
   
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_CLIENT_EVENT_HPP

