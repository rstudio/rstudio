/*
 * SessionWorkspace.cpp
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

#include "SessionWorkspace.hpp"

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

using namespace core ;
using namespace r::sexp;
using namespace r::exec;

namespace session {
namespace modules {
namespace workspace {

namespace {

bool handleRBrowseEnv(const core::FilePath& filePath)
{
   if (filePath.filename() == "wsbrowser.html")
   {
      module_context::showContent("R objects", filePath);
      return true;
   }
   else
   {
      return false;
   }
}

// last save action.
// NOTE: we don't persist this (or the workspace dirty state) during suspends in
// server mode. this means that if you are ever suspended then you will always
// end up with a 'dirty' workspace. not a big deal considering how infrequently
// quit occurs in server mode.
// TODO: this now affects switching projects after a suspend. we should try
// to figure out how to preserve dirty state of the workspace accross suspend
int s_lastSaveAction = r::session::kSaveActionAsk;

const char * const kSaveActionState = "saveActionState";
const char * const kImageDirtyState = "imageDirtyState";

void enqueSaveActionChanged()
{
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
      enqueSaveActionChanged();
   }
}

void onSuspend(Settings* pSettings)
{
   pSettings->set(kSaveActionState, s_lastSaveAction);
   pSettings->set(kImageDirtyState, r::session::imageIsDirty());
}

void onResume(const Settings& settings)
{
   s_lastSaveAction = settings.getInt(kSaveActionState,
                                      r::session::kSaveActionAsk);

   r::session::setImageDirty(settings.getBool(kImageDirtyState, true));

   enqueSaveActionChanged();
}

void onClientInit()
{
   // enque save action changed
   enqueSaveActionChanged();
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
   events().onClientInit.connect(bind(onClientInit));
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   
   // register handlers
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRBrowseFileHandler, handleRBrowseEnv));
   return initBlock.execute();
}
   

} // namepsace workspace
} // namespace modules
} // namesapce session

