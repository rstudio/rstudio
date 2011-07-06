/*
 * DirectoryMonitor.hpp
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

#ifndef CORE_SYSTEM_DIRECTORY_MONITOR_HPP
#define CORE_SYSTEM_DIRECTORY_MONITOR_HPP

#include <string>
#include <vector>
#include <iostream>

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/function.hpp>

#include <core/FileInfo.hpp>

#include <core/system/FileChangeEvent.hpp>

namespace core {

class Error;
   
namespace system {

class DirectoryMonitor : boost::noncopyable
{   
public:
   DirectoryMonitor();
   virtual ~DirectoryMonitor();
   // COPYING: boost::noncopyable
   
public:   
   // start monitoring the specified path. automatically stops monitoring
   // any previously monitored path.
   typedef boost::function<bool(const FileInfo&)> Filter;
   core::Error start(const std::string& path, const Filter& filter = Filter());
   
   // check for new events. if this is called prior to start() it simply
   // returns no events. if the monitored directory is deleted then
   // the monitor is automatically stopped the next time checkForEvents
   // is called on it.
   core::Error checkForEvents(std::vector<FileChangeEvent>* pEvents);
   
   // stop monitoring. no-op if the monitor was never started.
   core::Error stop();
   
   // get the currently monitored path
   std::string path() const ;
   
private:
   bool shouldFireEventForFile(const FileInfo& file);
   
private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

  
} // namespace system
} // namespace core 

#endif // CORE_SYSTEM_DIRECTORY_MONITOR_HPP


