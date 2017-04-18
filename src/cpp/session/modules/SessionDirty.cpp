/*
 * SessionDirty.cpp
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

#include "SessionDirty.hpp"

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/format.hpp>
#include <boost/utility.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RErrorCategory.hpp>
#include <r/session/RSession.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionSourceDatabase.hpp>

using namespace rstudio::core ;
using namespace rstudio::r::sexp;
using namespace rstudio::r::exec;

namespace rstudio {
namespace session {
namespace modules {
namespace dirty {

namespace {

// last save action.
// NOTE: we don't persist this (or the workspace dirty state) during suspends in
// server mode. this means that if you are ever suspended then you will always
// end up with a 'dirty' workspace. not a big deal considering how infrequently
// quit occurs in server mode.
// TODO: this now affects switching projects after a suspend. we should try
// to figure out how to preserve dirty state of the workspace accross suspend
int s_lastSaveAction = r::session::kSaveActionAsk;

// list of dirty documents (if it's empty then no document save is required)
std::set<std::string> s_dirtyDocuments;

const char * const kSaveActionState = "saveActionState";
const char * const kImageDirtyState = "imageDirtyState";



void updateSavePromptRequired()
{
   bool workspaceSavePromptRequired =
                    s_lastSaveAction == r::session::kSaveActionAsk;

   bool documentSavePromptRequired = s_dirtyDocuments.size() > 0;

   bool savePromptRequired = workspaceSavePromptRequired ||
                             documentSavePromptRequired;

   module_context::activeSession().setSavePromptRequired(savePromptRequired);
}

void onDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // ignore docs with no path
   if (pDoc->path().empty())
      return;

   // if it's dirty then ensure it's in the list, otherwise remove it
   size_t previousDirtyDocsSize = s_dirtyDocuments.size();
   if (pDoc->dirty())
      s_dirtyDocuments.insert(pDoc->id());
   else
      s_dirtyDocuments.erase(pDoc->id());

   if (s_dirtyDocuments.size() != previousDirtyDocsSize)
      updateSavePromptRequired();
}

void onDocRemoved(const std::string& id, const std::string&)
{
   s_dirtyDocuments.erase(id);
   updateSavePromptRequired();
}

void onRemoveAll()
{
   s_dirtyDocuments.clear();
   updateSavePromptRequired();
}

void handleSaveActionChanged()
{
   // update savePromptRequired
   updateSavePromptRequired();

   // enque event to client
   json::Object saveAction;
   saveAction["action"] = s_lastSaveAction;
   ClientEvent event(client_events::kSaveActionChanged, saveAction);
   module_context::enqueClientEvent(event);
}

void checkForSaveActionChanged()
{
   // compute current save action
   int currentSaveAction = r::session::imageIsDirty() ?
                                 module_context::saveWorkspaceAction() :
                                 r::session::kSaveActionNoSave;

   // compare and fire event if necessary
   if (s_lastSaveAction != currentSaveAction)
   {
      s_lastSaveAction = currentSaveAction;
      handleSaveActionChanged();
   }
}

void onSuspend(const r::session::RSuspendOptions&, Settings* pSettings)
{
   pSettings->set(kSaveActionState, s_lastSaveAction);
   pSettings->set(kImageDirtyState, r::session::imageIsDirty());
}

void onResume(const Settings& settings)
{
   s_lastSaveAction = settings.getInt(kSaveActionState,
                                      r::session::kSaveActionAsk);

   r::session::setImageDirty(settings.getBool(kImageDirtyState, true));

   handleSaveActionChanged();
}

void onClientInit()
{
   // enque save action changed
   handleSaveActionChanged();
}
 
void onDetectChanges(module_context::ChangeSource source)
{
   // check for save action changed
   checkForSaveActionChanged();
}

} // anonymous namespace
 
Error initialize()
{         
   // add suspend handler
   using namespace session::module_context;
   addSuspendHandler(SuspendHandler(onSuspend, onResume));

   // subscribe to events
   using boost::bind;
   module_context::events().onClientInit.connect(bind(onClientInit));
   module_context::events().onDetectChanges.connect(bind(onDetectChanges, _1));
   source_database::events().onDocUpdated.connect(onDocUpdated);
   source_database::events().onDocRemoved.connect(onDocRemoved);
   source_database::events().onRemoveAll.connect(onRemoveAll);

   return Success();
}
   

} // namepsace dirty
} // namespace modules
} // namespace session
} // namespace rstudio

