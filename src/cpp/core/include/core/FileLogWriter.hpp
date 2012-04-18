/*
 * FileLogWriter.hpp
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

#ifndef FILE_LOG_WRITER_HPP
#define FILE_LOG_WRITER_HPP

#include <core/FilePath.hpp>
#include <core/LogWriter.hpp>

namespace core {

class FileLogWriter : public LogWriter
{
public:
    FileLogWriter(const std::string& programIdentity,
                  int logLevel,
                  const FilePath& logDir);
    virtual ~FileLogWriter();

    virtual void log(core::system::LogLevel level,
                     const std::string& message);

private:
    bool rotateLogFile();

    std::string programIdentity_;
    int logLevel_;
    FilePath logFile_;
};

} // namespace core

#endif // FILE_LOG_WRITER_HPP
