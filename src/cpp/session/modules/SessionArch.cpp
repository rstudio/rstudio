/*
 * SessionArch.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionArch.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>

#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace arch {

namespace {


void setRArchIfNecessary()
{
   // on OSX set the R_ARCH if this is a standard multi-arch installation
#ifdef __APPLE__

   // get the R home bin dir
   FilePath rHomeBinDir;
   Error error = module_context::rBinDir(&rHomeBinDir);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // if it starts with the standard prefix and an etc/x86_64 directory
   // exists then we set the R_ARCH
   if (boost::algorithm::starts_with(rHomeBinDir.absolutePath(),
                                     "/Library/Frameworks/R.framework/") &&

       FilePath("/Library/Frameworks/R.framework/Resources/etc/x86_64")
                                                                   .exists())
   {
      core::system::setenv("R_ARCH", "/x86_64");
   }
#endif
}


} // anonymous namespace


Error initialize()
{
   setRArchIfNecessary();

   return Success();

}
   
   
} // namespace arch
} // namespace modules
} // namesapce session

