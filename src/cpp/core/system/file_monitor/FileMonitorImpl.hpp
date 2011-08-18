/*
 * FileMonitorImpl.hpp
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

#ifndef CORE_SYSTEM_FILE_MONITOR_IMPL_HPP
#define CORE_SYSTEM_FILE_MONITOR_IMPL_HPP

#include <string>


#include <core/FilePath.hpp>
#include <core/collection/Tree.hpp>

#include <core/system/FileChangeEvent.hpp>

#include <core/system/FileMonitor.hpp>

namespace core {   
namespace system {
namespace file_monitor {
namespace impl {

Error processFileAdded(tree<FileInfo>::iterator parentIt,
                       const FileChangeEvent& fileChange,
                       tree<FileInfo>* pTree,
                       std::vector<FileChangeEvent>* pFileChanges);

void processFileModified(tree<FileInfo>::iterator parentIt,
                         const FileChangeEvent& fileChange,
                         tree<FileInfo>* pTree,
                         std::vector<FileChangeEvent>* pFileChanges);

void processFileRemoved(tree<FileInfo>::iterator parentIt,
                        const FileChangeEvent& fileChange,
                        tree<FileInfo>* pTree,
                        std::vector<FileChangeEvent>* pFileChanges);

Error discoverAndProcessFileChanges(const FileInfo& fileInfo,
                                    bool recursive,
                                    tree<FileInfo>* pTree,
                                    const Callbacks::FilesChanged& onFilesChanged);


} // namespace impl
} // namespace file_monitor
} // namespace system
} // namespace core 

#endif // CORE_SYSTEM_FILE_MONITOR_IMPL_HPP


