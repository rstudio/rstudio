/*
 * StderrLogWriter.hpp
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

#ifndef STDERR_LOG_WRITER_HPP
#define STDERR_LOG_WRITER_HPP

#include <core/LogWriter.hpp>

namespace core {

class StderrLogWriter : public LogWriter
{
public:
    StderrLogWriter(const std::string& programIdentity, int logLevel);
    virtual ~StderrLogWriter();

    virtual void log(core::system::LogLevel level,
                     const std::string& message);

private:
    std::string programIdentity_;
    int logLevel_;
};

} // namespace core

#endif // STDERR_LOG_WRITER_HPP
