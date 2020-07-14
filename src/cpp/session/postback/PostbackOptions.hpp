/*
 * PostbackOptions.hpp
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

#ifndef POSTBACK_OPTIONS_HPP
#define POSTBACK_OPTIONS_HPP

#include <string>

#include <boost/utility.hpp>

namespace rstudio {
namespace core {
   class ProgramStatus;
}
}

namespace rstudio {
namespace session {
namespace postback {

// singleton
class Options;
Options& options();
   
class Options : boost::noncopyable
{
private:
   Options() {};
   friend Options& options();
   // COPYING: boost::noncopyable
   
public:
   core::ProgramStatus read(int argc, char * const argv[]);

   std::string command() const
   {
      return std::string(command_.c_str());
   }
   
   std::string argument() const
   {
      return std::string(argument_.c_str());
   }

private:
   std::string programName_;
   std::string command_;
   std::string argument_;
};

} // namespace postback
} // namespace session
} // namespace rstudio

#endif // POSTBACK_OPTIONS_HPP

