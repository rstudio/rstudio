/*
 * ChildProcess.hpp
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


#ifndef CORE_SYSTEM_CHILD_PROCESS_HPP
#define CORE_SYSTEM_CHILD_PROCESS_HPP

#include <vector>

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/shared_ptr.hpp>

namespace core {

class Error;

namespace system {

class ChildProcess : boost::noncopyable
{
public:
   ChildProcess(const std::string& cmd,
                const std::vector<std::string>& args);
   virtual ~ChildProcess() ;



   Error writeToStdin(const std::string& input);

   Error terminate();

   bool exited();

   void pollForEvents();

protected:
   virtual void onStdout(const std::string& output);
   virtual void onStderr(const std::string& err);
   virtual void onExited(int statusCode);

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};


class ChildProcessSupervisor : boost::noncopyable
{
public:

   template<typename T>
   void add(boost::shared_ptr<T> pChild)
   {
      children_.push_back(boost::shared_static_cast<ChildProcess>(pChild));
   }


   void pollForEvents();

private:
   std::vector<boost::shared_ptr<ChildProcess> > children_;
};


} // namespace system
} // namespace core

#endif // CORE_SYSTEM_CHILD_PROCESS_HPP
