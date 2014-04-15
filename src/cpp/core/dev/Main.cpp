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

#include <core/system/PosixSched.hpp>

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


      core::system::CpuAffinity cpuAffinity = core::system::emptyCpuAffinity();

      cpuAffinity[1] = true;
      cpuAffinity[3] = true;

      error = core::system::setCpuAffinity(cpuAffinity);
      if (error)
         LOG_ERROR(error);

      core::system::CpuAffinity cpuAffinity2;
      error = core::system::getCpuAffinity(&cpuAffinity2);

      BOOST_FOREACH(bool val, cpuAffinity2)
      {
         std::cerr << (val ? "true" : "false") << std::endl;
      }

      std::flush(std::cerr);


      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

