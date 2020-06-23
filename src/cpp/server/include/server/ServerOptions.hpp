/*
 * ServerOptions.hpp
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

#ifndef SERVER_SERVER_OPTIONS_HPP
#define SERVER_SERVER_OPTIONS_HPP

#include <string>
#include <map>
#include <iosfwd>

#include <boost/regex.hpp>
#include <boost/utility.hpp>

#include <shared_core/FilePath.hpp>
#include <core/ProgramOptions.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/system/Types.hpp>

#include <server/ServerOptions.gen.hpp>

namespace rstudio {
namespace server {

// singleton
class Options;
Options& options();

// add overlay-specific args and/or environment variables
void sessionProcessConfigOverlay(core::system::Options* pArgs, core::system::Options* pEnvironment);

class Options : public GeneratedOptions,
                boost::noncopyable
{
private:
   Options() {}
   friend Options& options();
   // COPYING: boost::noncopyable
   
public:
   virtual ~Options() {}
   core::ProgramStatus read(int argc,
                            char * const argv[],
                            std::ostream& osWarnings) override;

   std::string gwtPrefix() const;

   std::string wwwPort(bool secure = false) const
   {
      if (!wwwPort_.empty())
      {
         return wwwPort_;
      }
      else
      {
         if (secure)
            return std::string("443");
         else
            return std::string("8787");
      }
   }

   std::string monitorSharedSecret() const
   {
      return monitorSharedSecret_;
   }

   bool serverOffline() const
   {
      return serverOffline_;
   }

   std::string getOverlayOption(const std::string& name)
   {
      return overlayOptions_[name];
   }

   std::string rsessionExecutable() const;

private:
   std::string monitorSharedSecret_;
   bool serverOffline_;
   core::FilePath installPath_;

   std::map<std::string,std::string> overlayOptions_;

   void resolvePath(const core::FilePath& basePath,
                    std::string* pPath) const;

   void addOverlayOptions(boost::program_options::options_description* pVerify,
                          boost::program_options::options_description* pServer,
                          boost::program_options::options_description* pWWW,
                          boost::program_options::options_description* pRSession,
                          boost::program_options::options_description* pDatabase,
                          boost::program_options::options_description* pAuth,
                          boost::program_options::options_description* pMonitor);

   bool validateOverlayOptions(std::string* pErrMsg, std::ostream& osWarnings);

   void resolveOverlayOptions();

   void setOverlayOption(const std::string& name, const std::string& value)
   {
      overlayOptions_[name] = value;
   }

   void setOverlayOption(const std::string& name, bool value)
   {
      setOverlayOption(name, value ? std::string("1") : std::string("0"));
   }

   void setOverlayOption(const std::string& name, int value)
   {
      setOverlayOption(name, core::safe_convert::numberToString(value));
   }
};
      
} // namespace server
} // namespace rstudio

#endif // SERVER_SERVER_OPTIONS_HPP

