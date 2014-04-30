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

#include <core/system/PosixSystem.hpp>

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


      core::system::SysInfo sysInfo;
      error = core::system::systemInformation(&sysInfo);

      std::cerr << " Cores: " << sysInfo.cores << std::endl;
      std::cerr << " Load1: " << sysInfo.load1 << std::endl;
      std::cerr << " Load5: " << sysInfo.load5 << std::endl;
      std::cerr << "Load15: " << sysInfo.load15 << std::endl;

      std::vector<PidType> pids;
      error = core::system::pidof("rsession", &pids);
      if (error)
         LOG_ERROR(error);

      BOOST_FOREACH(PidType pid, pids)
      {
         std::cerr << pid << std::endl;
      }

      std::vector<core::system::IpAddress> addresses;
      error = core::system::ipAddresses(&addresses);
      if (error)
         LOG_ERROR(error);

      std::cerr << std::endl;
      BOOST_FOREACH(const core::system::IpAddress& address, addresses)
      {
         std::cerr << address.name << " - " << address.addr << std::endl;
      }

      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}

