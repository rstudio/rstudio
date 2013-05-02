/*
 * SessionData.cpp
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

#include "SessionData.hpp"

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

#include "DataViewer.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace data {

Error initialize()
{
   using boost::bind;
   using namespace session::module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (data::viewer::initialize)
      (bind(sourceModuleRFile, "SessionDataImport.R"));

   return initBlock.execute();
}
   
} // namespace data
} // namespace modules
} // namesapce session

