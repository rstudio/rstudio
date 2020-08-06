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

// the last-known Python path as provided via preferences
std::string s_defaultPythonInterpreterPath;

void onPrefsChanged(const std::string& /* layerName */,
                    const std::string& prefName)
{
   if (prefName == kPythonDefaultInterpreter)
   {
      // get new preference value
      std::string defaultPythonInterpreterPath =
         prefs::userPrefs().pythonDefaultInterpreter();

      // only update RETICULATE_PYTHON if it has not been modified by the user
      std::string reticulatePython = core::system::getenv("RETICULATE_PYTHON");
      if (reticulatePython == s_defaultPythonInterpreterPath)
      {
         core::system::setenv("RETICULATE_PYTHON", defaultPythonInterpreterPath);
      }

      // update our last-known preference value
      s_defaultPythonInterpreterPath = defaultPythonInterpreterPath;
   }
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;
   
   prefs::userPrefs().onChanged.connect(onPrefsChanged);

   // initialize last known Python path
   s_defaultPythonInterpreterPath = prefs::userPrefs().pythonDefaultInterpreter();

   // if RETICULATE_PYTHON has not yet been set, initialize it
   // via the value stored in preferences (if any)
   std::string pythonPath = core::system::getenv("RETICULATE_PYTHON");
   if (pythonPath.empty() && !s_defaultPythonInterpreterPath.empty())
      core::system::setenv("RETICULATE_PYTHON", s_defaultPythonInterpreterPath);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPythonEnvironments.R"));
   
   return initBlock.execute();
}

} // end namespace python_environments
} // end namespace modules
} // end namespace session
} // end namespace rstudio
