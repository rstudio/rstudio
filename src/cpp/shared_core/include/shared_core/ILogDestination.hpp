/*
 * ILogDestination.hpp
 * 
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant to the terms of a commercial license agreement
 * with Posit, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#ifndef SHARED_CORE_I_LOG_DESTINATION_HPP
#define SHARED_CORE_I_LOG_DESTINATION_HPP

#include <boost/noncopyable.hpp>

#include <string>

#include "Logger.hpp"

namespace rstudio {
namespace core {
namespace log {

/**
 * @brief Interface which allows a logger to write a log message to a destination.
 */
class ILogDestination : boost::noncopyable
{
public:
   /**
    * @brief Constructor.
    *
    * @param in_id          The unique ID of this log destination. You must ensure that sufficient uniqueness is provided to ensure
    *                       no other log destinations attempt to use the same ID, which will prevent them from being registered.
    * @param in_logLevel    The most detailed level of log to be written to this log destination.
    * @param in_formatType  The log message format type.
    * @param in_reloadable  Whether or not the destination is reloadable. If so, reloading of logging configuration
    *                       will cause the log destination to be removed. Set this to true only for log destinations
    *                       that are intended to be hot-reconfigurable, such as the global default logger.
    */
   explicit ILogDestination(const std::string& in_id,
                            LogLevel in_logLevel,
                            LogMessageFormatType in_formatType,
                            bool in_reloadable) : m_id(in_id), m_logLevel(in_logLevel), m_formatType(in_formatType), m_reloadable(in_reloadable) {}



   /**
    * @brief Virtual destructor to allow for inheritance.
    */
   virtual ~ILogDestination() = default;

   /**
    * @brief Gets the unique ID of the log destination.
    *
    * @return The unique ID of the log destination.
    */
   std::string getId() const { return m_id; }

   /**
    * @brief Gets the maximum level of logs that will be written to this log destination.
    *
    * @return This log destination's maximum log level.
    */
   LogLevel getLogLevel() { return m_logLevel; }

   /**
    * @brief Gets the log message format type for this log destination.
    *
    * @return This log destination's message format type.
    */
   LogMessageFormatType getLogMessageFormatType() { return m_formatType; }

   /**
    * @brief Gets whether or not the log destination is reloadable.
    *
    * @return Whether or not the log destination is reloadable.
    */
   bool isReloadable() const { return m_reloadable; }

   /**
    * @brief Refresh the log destintation. Ensures that the log does not have any stale file handles.
    *
    * @param in_refreshParams   Refresh params to use when refreshing the log destinations (if applicable).
    */
   virtual void refresh(const RefreshParams& in_refreshParams = RefreshParams()) = 0;

   /**
    * @brief Writes a message to this log destination.
    *
    * @param in_logLevel    The log level of the message to write.
    * @param in_message     The message to write to the destination.
    */
   virtual void writeLog(LogLevel in_logLevel, const std::string& in_message) = 0;

protected:
   /**
    * @brief The unique ID of the log destination.
    */
    std::string m_id;

   /**
    * @brief The maximum level of log messages to write for this logger.
    */
    LogLevel m_logLevel;

   /**
    * @brief The log message format type.
    */
    LogMessageFormatType m_formatType;

   /**
    * @brief Whether or not the log destination is reloadable (i.e. it will be destroyed when the global logger is reinitialized).
    */
    bool m_reloadable;
};

} // namespace log
} // namespace core
} // namespace rstudio

#endif
