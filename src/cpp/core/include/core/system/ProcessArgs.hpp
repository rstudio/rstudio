/*
 * ProcessArgs.hpp
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

#ifndef CORE_SYSTEM_PROCESS_ARGS_HPP
#define CORE_SYSTEM_PROCESS_ARGS_HPP

#include <string>
#include <vector>

#include <boost/utility.hpp>

namespace rstudio {
namespace core {
namespace system {

// helper class used to manage argument memory during child spawn
class ProcessArgs : boost::noncopyable
{
public:
   ProcessArgs() : argCount_(0), args_(nullptr) {}

   ProcessArgs(const std::vector<std::string>& args)
      : argCount_(0), args_(nullptr)
   {
      setArgs(args);
   }

   virtual ~ProcessArgs()
   {
      try
      {
         freeArgs();
      }
      catch(...)
      {
      }
   }

   bool empty() const { return argCount_ == 0; }
   std::size_t argCount() const { return argCount_; }
   char** args() const { return !empty() ? args_ : nullptr; }
   
private:
   void setArgs(const std::vector<std::string>& args)
   {
      // free existing
      freeArgs();

      // allocate args
      argCount_ = args.size();
      args_ = new char*[argCount_+1];

      // copy each arg to a buffer
      for (unsigned int i=0; i<argCount_; i++)
      {
         const std::string& arg = args[i];
         args_[i] = new char[arg.size() + 1];
         arg.copy(args_[i], arg.size());
         args_[i][arg.size()] = '\0';
      }

      // null terminate the list of args
      args_[argCount_] = nullptr;
   }

   void freeArgs()
   {
      if (args_ && argCount_ > 0)
      {
         for (std::size_t i = 0; i<argCount_; ++i)
            delete [] args_[i];
      }

      delete [] args_;
   }
   
private:
   std::size_t argCount_;
   char** args_;
};

} // namespace system 
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_PROCESS_ARGS_HPP

