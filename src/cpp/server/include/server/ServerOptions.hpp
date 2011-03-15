/*
 * ServerOptions.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SERVER_SERVER_OPTIONS_HPP
#define SERVER_SERVER_OPTIONS_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/http/Request.hpp>

namespace core {
   class ProgramStatus;
}

namespace server {

// singleton
class Options ;
Options& options();   
   
class Options : boost::noncopyable
{
private:
   Options() {}
   friend Options& options();
   // COPYING: boost::noncopyable
   
public:
   virtual ~Options() {}
   core::ProgramStatus read(int argc, char * const argv[]);
   
   std::string serverWorkingDir() const
   { 
      return std::string(serverWorkingDir_.c_str());
   }
      
   bool serverOffline() const
   {
      return serverOffline_;
   }
   
   std::string serverUser() const
   { 
      return std::string(serverUser_.c_str());
   }
   
   bool serverDaemonize() const { return serverDaemonize_; }
      
   // www 
   std::string wwwAddress() const
   { 
      return std::string(wwwAddress_.c_str()) ; 
   }
   
   std::string wwwPort() const
   { 
      return std::string(wwwPort_.c_str()); 
   }
   
   std::string wwwLocalPath() const
   {
      return std::string(wwwLocalPath_.c_str()); 
   }
   
   int wwwThreadPoolSize() const
   {
      return wwwThreadPoolSize_;
   }

   // auth
   bool authValidateUsers()
   {
      return authValidateUsers_;
   }

   std::string authRequiredUserGroup()
   {
      return std::string(authRequiredUserGroup_.c_str());
   }

   // r
   std::string rHome() const
   {
      return std::string(rHome_.c_str());
   }

   std::string rDocDir() const
   {
      return std::string(rDocDir_.c_str());
   }

   std::string rLibDir() const
   {
      return std::string(rLibDir_.c_str());
   }

   // rsession
   std::string rsessionPath() const
   { 
      return std::string(rsessionPath_.c_str()); 
   }

   std::string rldpathPath() const
   {
      return std::string(rldpathPath_.c_str());
   }
   
   std::string rsessionConfigFile() const
   { 
      return std::string(rsessionConfigFile_.c_str()); 
   }
   
   int rsessionMemoryLimitMb() const
   {
      return rsessionMemoryLimitMb_;
   }
   
   int rsessionStackLimitMb() const
   {
      return rsessionStackLimitMb_;
   }

   int rsessionUserProcessLimit() const
   {
      return rsessionUserProcessLimit_;
   }

private:
   bool resolveRPaths();

private:
   std::string serverWorkingDir_;
   std::string serverUser_;
   bool serverDaemonize_;
   bool serverOffline_;
   std::string wwwAddress_ ;
   std::string wwwPort_ ;
   std::string wwwLocalPath_ ;
   int wwwThreadPoolSize_;
   bool authValidateUsers_;
   std::string authRequiredUserGroup_;
   std::string rHome_;
   std::string rDocDir_;
   std::string rLibDir_;
   std::string rsessionPath_;
   std::string rldpathPath_;
   std::string rsessionConfigFile_;
   int rsessionMemoryLimitMb_;
   int rsessionStackLimitMb_;
   int rsessionUserProcessLimit_;
};
      
} // namespace server

#endif // SERVER_SERVER_OPTIONS_HPP

