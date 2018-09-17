/*
 * RSRun.hpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#ifndef RSRUN_HPP
#define RSRUN_HPP

#include <string>
#include <boost/noncopyable.hpp>

namespace rstudio {
namespace core {
namespace terminal {

extern const char* const kRSRunPrefix; // prefix of RSRun ESC sequence
extern const char* const kRSRunSuffix; // suffix (EOM) of RSRun ESC sequence

class RSRun : boost::noncopyable
{
public:
   explicit RSRun();

   std::string processESC(const std::string& input);

private:
   enum class ParseState { 
      normal, 
      partial, 
      running
   };
   
   std::string stripESC(const std::string& strInput);
   
private:
   ParseState state_ = ParseState::normal;
   std::string esc_;
};

} // namespace terminal
} // namespace core
} // namespace terminal

#endif // RSRUN_HPP
