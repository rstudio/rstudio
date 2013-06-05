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

namespace core {
   class FilePath;
}

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
extern const int kInstalledPackagesChanged;
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

#endif // SESSION_SESSION_CLIENT_EVENT_HPP

