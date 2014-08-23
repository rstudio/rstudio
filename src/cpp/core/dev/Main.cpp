/*
 * Main.cpp
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

#include <iostream>

#include <boost/test/minimal.hpp>
#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>

#include <core/r_util/RVersionInfo.hpp>
#include <core/r_util/RVersionsPosix.hpp>

using namespace core ;

int test_main(int argc, char * argv[])
{
   try
   { 
      // setup log
      initializeStderrLog("coredev", core::system::kLogLevelWarning);

      // ignore sigpipe
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      using namespace core::r_util;

      std::vector<RVersionNumber> vers;
      vers.push_back(RVersionNumber::parse("3.0"));
      vers.push_back(RVersionNumber::parse("2.14.3"));
      vers.push_back(RVersionNumber::parse("3.0.1"));
      vers.push_back(RVersionNumber::parse("2.15"));
      vers.push_back(RVersionNumber::parse("3.1.0"));

      std::sort(vers.begin(), vers.end());
      std::reverse(vers.begin(), vers.end());

      BOOST_FOREACH(RVersionNumber ver, vers)
      {
         std::cerr << ver << std::endl;
      }

      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

