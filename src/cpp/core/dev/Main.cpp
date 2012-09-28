/*
 * Main.cpp
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

#include <iostream>

#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Log.hpp>

#include <core/system/System.hpp>

#include <core/r_util/RToolsInfo.hpp>

using namespace core ;

int main(int argc, char * const argv[]) 
{
   try
   { 

      // initialize log
      initializeSystemLog("coredev", core::system::kLogLevelWarning);

      std::vector<r_util::RToolsInfo> rTools;
      Error error = core::r_util::discoverRTools(&rTools);
      if (error)
         LOG_ERROR(error);

      BOOST_FOREACH(const r_util::RToolsInfo& tool, rTools)
      {
         std::cout << tool << std::endl << std::endl;
      }

      core::system::Options environment;
      core::system::environment(&environment);
      r_util::prependToSystemPath(rTools.back(), &environment);

      std::cout << core::system::getenv(environment, "PATH") << std::endl;
     
      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

