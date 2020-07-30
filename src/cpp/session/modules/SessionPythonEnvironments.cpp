/*
 * SessionPythonEnvironments.cpp
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

#include "SessionPythonEnvironments.hpp"

#include <core/Exec.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserPrefValues.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace python_environments {

namespace {

void updateDefaultPythonInterpreter()
{
   core::system::setenv(
            "RETICULATE_PYTHON",
            prefs::userPrefs().pythonDefaultInterpreter());
}

void onPrefsChanged(const std::string& /* layerName */, const std::string prefName)
{
   if (prefName == kPythonDefaultInterpreter)
   {
      updateDefaultPythonInterpreter();
   }
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;
   
   prefs::userPrefs().onChanged.connect(onPrefsChanged);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPythonEnvironments.R"));
   
   return initBlock.execute();
}

} // end namespace python_environments
} // end namespace modules
} // end namespace session
} // end namespace rstudio
