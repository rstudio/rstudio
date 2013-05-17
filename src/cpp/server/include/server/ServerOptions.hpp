/*
 * ServerOptions.hpp
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

#ifndef SERVER_SERVER_OPTIONS_HPP
#define SERVER_SERVER_OPTIONS_HPP

#include <string>
#include <map>

#include <boost/utility.hpp>

#include <core/FilePath.hpp>
#include <core/ProgramOptions.hpp>
#include <core/SafeConvert.hpp>

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
   
   bool verifyInstallation() const
   {
      return verifyInstallation_;
   }

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

   bool serverAppArmorEnabled() const { return serverAppArmorEnabled_; }
      
   // www 
   std::string wwwAddress() const
   { 
      return std::string(wwwAddress_.c_str()) ; 
   }
   
   std::string wwwPort(bool secure = false) const
   { 
      if (!wwwPort_.empty())
      {
         return std::string(wwwPort_.c_str());
      }
      else
      {
         if (secure)
            return std::string("443");
         else
            return std::string("8787");
      }
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

   std::string authPamHelperPath() const
   {
      return std::string(authPamHelperPath_.c_str());
   }

   // rsession
   std::string rsessionWhichR() const
   {
      return std::string(rsessionWhichR_.c_str());
   }

   std::string rsessionPath() const
   { 
      return std::string(rsessionPath_.c_str()); 
   }

   std::string rldpathPath() const
   {
      return std::string(rldpathPath_.c_str());
   }

   std::string rsessionLdLibraryPath() const
   {
      return std::string(rsessionLdLibraryPath_.c_str());
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

   std::string getOverlayOption(const std::string& name)
   {
      return overlayOptions_[name];
   }

private:

   void resolvePath(std::string* pPath) const;

   void addOverlayOptions(boost::program_options::options_description* pServer,
                          boost::program_options::options_description* pWWW,
                          boost::program_options::options_description* pRSession,
                          boost::program_options::options_description* pAuth);

   bool validateOverlayOptions(std::string* pErrMsg);

   void resolveOverlayOptions();

   void setOverlayOption(const std::string& name, const std::string& value)
   {
      overlayOptions_[name] = value;
   }

   void setOverlayOption(const std::string& name, bool value)
   {
      setOverlayOption(name, value ? "1" : "0");
   }

   void setOverlayOption(const std::string& name, int value)
   {
      setOverlayOption(name, core::safe_convert::numberToString(value));
   }


private:
   core::FilePath installPath_;
   bool verifyInstallation_;
   std::string serverWorkingDir_;
   std::string serverUser_;
   bool serverDaemonize_;
   bool serverAppArmorEnabled_;
   bool serverOffline_;
   std::string wwwAddress_ ;
   std::string wwwPort_ ;
   std::string wwwLocalPath_ ;
   int wwwThreadPoolSize_;
   bool authValidateUsers_;
   std::string authRequiredUserGroup_;
   std::string authPamHelperPath_;
   std::string rsessionWhichR_;
   std::string rsessionPath_;
   std::string rldpathPath_;
   std::string rsessionConfigFile_;
   std::string rsessionLdLibraryPath_;
   int rsessionMemoryLimitMb_;
   int rsessionStackLimitMb_;
   int rsessionUserProcessLimit_;
   std::map<std::string,std::string> overlayOptions_;
};
      
} // namespace server

#endif // SERVER_SERVER_OPTIONS_HPP

