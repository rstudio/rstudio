/*
 * SessionPythonEnvironments.cpp
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

#include "SessionPythonEnvironments.hpp"

#include <core/Exec.hpp>

#include <r/RExec.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserPrefValues.hpp>
#include <session/projects/SessionProjects.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace python_environments {

namespace {

void onPrefsChanged(const std::string& /* layerName */,
                    const std::string& prefName)
{
   if (prefName == kPythonPath)
   {
      std::string pythonPath = prefs::userPrefs().pythonPath();
      
      if (!pythonPath.empty())
      {
         Error error = r::exec::RFunction(".rs.reticulate.usePython")
               .addParam(pythonPath)
               .call();

         if (error)
            LOG_ERROR(error);
      }
   }
}

void onInitComplete()
{
   if (!projects::projectContext().hasProject())
      return;
   
   Error error = r::exec::RFunction(".rs.python.initialize")
         .addUtf8Param(projects::projectContext().directory())
         .call();
   
   if (error)
      LOG_ERROR(error);
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;
   
   events().onInitComplete.connect(onInitComplete);
   
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
