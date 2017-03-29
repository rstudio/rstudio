/*
 * Backtrace.cpp
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

#include <core/Backtrace.hpp>
#include <core/RegexUtils.hpp>

#ifndef _WIN32
# include <core/Algorithm.hpp>
# include <iostream>
# include <boost/regex.hpp>
# include <execinfo.h>
# include <cxxabi.h>
#endif

namespace rstudio {
namespace core {
namespace backtrace {

std::string demangle(const std::string& name)
{
   std::string result = name;
   
#ifndef _WIN32
   int status = -1;
   char* demangled = ::abi::__cxa_demangle(name.c_str(), NULL, NULL, &status);
   if (status == 0) {
      result = demangled;
      ::free(demangled);
   }
#endif
  
   return result;
}

void printBacktrace(std::ostream& os)
{
#ifndef _WIN32
   
   os << "Backtrace (most recent calls first):" << std::endl << std::endl;
   
   std::size_t maxDepth = 100;
   std::size_t stackDepth;
   
   void* stackAddrs[maxDepth];
   stackDepth = ::backtrace(stackAddrs, maxDepth);
   
   char** stackStrings;
   stackStrings = ::backtrace_symbols(stackAddrs, stackDepth);
   
   std::vector<std::string> demangled;
   demangled.reserve(stackDepth);
   
   // Each printed string will have output of the form:
   //
   //     (row) (program) (address) (function) + (offset)
   //
   // with variable numbers of spaces separating each field.
   std::string reBacktraceString = std::string() +
         "(\\d+)" +           // stack number
         "(\\s+)" +           // spaces
         "([^\\s]+)" +        // program name
         "(\\s+)" +           // spaces
         "([^\\s]+)" +        // address
         "(\\s+)" +           // spaces
         "([^\\s]+)" +        // (mangled) function name
         "(.*)";              // offset (the rest)
   
   boost::regex reBacktrace(reBacktraceString);
   boost::cmatch match;
   
   for (std::size_t i = 0; i < stackDepth; ++i)
   {
      std::string readyForPrinting = stackStrings[i];
      if (regex_utils::match(stackStrings[i], match, reBacktrace))
      {
         std::size_t n = match.length();
         if (n >= 8)
         {
            std::string demangled = demangle(match[7]);
            
            std::string joined;
            for (std::size_t i = 1; i < 7; ++i)
               joined += match[i];
            
            joined += demangled;
            if (n > 8)
               joined += match[8];
            
            readyForPrinting = joined;
         }
      }
      os << readyForPrinting << std::endl;
   }
   
   if (stackStrings != NULL)
      ::free(stackStrings);
   
#endif
}

} // namespace debug
} // namespace core
} // namespace rstudio
