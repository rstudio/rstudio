/*
 * FileMonitorImpl.hpp
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

#ifndef CORE_SYSTEM_FILE_MONITOR_IMPL_HPP
#define CORE_SYSTEM_FILE_MONITOR_IMPL_HPP

#include <string>
#include <algorithm>
#include <list>

#include <boost/bind.hpp>

#include <shared_core/FilePath.hpp>
#include <core/collection/Tree.hpp>

#include <core/system/FileChangeEvent.hpp>

#include <core/system/FileMonitor.hpp>

namespace rstudio {
namespace core {   
namespace system {
namespace file_monitor {
namespace impl {

Error processFileAdded(
               tree<FileInfo>::iterator parentIt,
               const FileChangeEvent& fileChange,
               bool recursive,
               const boost::function<bool(const FileInfo&)>& filter,
               const boost::function<Error(const FileInfo&)>& onBeforeScanDir,
               tree<FileInfo>* pTree,
               std::vector<FileChangeEvent>* pFileChanges);

inline Error processFileAdded(
               tree<FileInfo>::iterator parentIt,
               const FileChangeEvent& fileChange,
               bool recursive,
               const boost::function<bool(const FileInfo&)>& filter,
               tree<FileInfo>* pTree,
               std::vector<FileChangeEvent>* pFileChanges)
{
   return processFileAdded(parentIt,
                           fileChange,
                           recursive,
                           filter,
                           boost::function<Error(const FileInfo&)>(),
                           pTree,
                           pFileChanges);
}

void processFileModified(tree<FileInfo>::iterator parentIt,
                         const FileChangeEvent& fileChange,
                         tree<FileInfo>* pTree,
                         std::vector<FileChangeEvent>* pFileChanges);

void processFileRemoved(tree<FileInfo>::iterator parentIt,
                        const FileChangeEvent& fileChange,
                        bool recursive,
                        tree<FileInfo>* pTree,
                        std::vector<FileChangeEvent>* pFileChanges);

Error discoverAndProcessFileChanges(
   const FileInfo& fileInfo,
   bool recursive,
   const boost::function<bool(const FileInfo&)>& filter,
   const boost::function<Error(const FileInfo&)>& onBeforeScanDir,
   tree<FileInfo>* pTree,
   const boost::function<void(const std::vector<FileChangeEvent>&)>&
                                                            onFilesChanged);

inline Error discoverAndProcessFileChanges(
   const FileInfo& fileInfo,
   bool recursive,
   const boost::function<bool(const FileInfo&)>& filter,
   tree<FileInfo>* pTree,
   const boost::function<void(const std::vector<FileChangeEvent>&)>&
                                                            onFilesChanged)
{
   return discoverAndProcessFileChanges(
                                 fileInfo,
                                 recursive,
                                 filter,
                                 boost::function<Error(const FileInfo&)>(),
                                 pTree,
                                 onFilesChanged);
}

template <typename Iterator>
Iterator findFile(Iterator begin, Iterator end, const std::string& path)
{
   return std::find_if(begin, end, boost::bind(fileInfoHasPath,
                                               _1,
                                               path));
}

template <typename Iterator>
Iterator findFile(Iterator begin, Iterator end, const FileInfo& fileInfo)
{
   return findFile(begin, end, fileInfo.absolutePath());
}

std::list<void*> activeEventContexts();


} // namespace impl
} // namespace file_monitor
} // namespace system
} // namespace core 
} // namespace rstudio

#endif // CORE_SYSTEM_FILE_MONITOR_IMPL_HPP


