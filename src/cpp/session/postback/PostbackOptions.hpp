/*
 * PostbackOptions.hpp
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

#ifndef POSTBACK_OPTIONS_HPP
#define POSTBACK_OPTIONS_HPP

#include <string>

#include <boost/utility.hpp>

namespace core {
   class ProgramStatus;
}

namespace session {
namespace postback {

// singleton
class Options ;
Options& options();   
   
class Options : boost::noncopyable
{
private:
   Options() {} ;
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

#endif // POSTBACK_OPTIONS_HPP

