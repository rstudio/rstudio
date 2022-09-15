/*
 * Logger.hpp
 * 
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant to the terms of a commercial license agreement
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

#ifndef SHARED_CORE_LOGGER_HPP
#define SHARED_CORE_LOGGER_HPP

#include <map>
#include <memory>
#include <ostream>
#include <string>
#include <vector>

#include <boost/any.hpp>
#include <boost/function.hpp>
#include <boost/optional.hpp>

#include <shared_core/system/User.hpp>

#include "PImpl.hpp"

namespace rstudio {
namespace core {

class Error;
class ErrorLocation;

} // namespace rstudio
} // namespace core


namespace rstudio {
namespace core {
namespace log {

/**
 * @file
 * Logging definitions and functions.
 */

class ILogDestination;

/**
 * @brief A struct encapsulating various params to pass when refreshing log destinations.
 *        This carries data that some loggers may be interested in consuming to determine
 *        how the refresh should be carried out. See the FileLogDestination for a concrete
 *        example of this.
 */
struct RefreshParams
{
#ifndef _WIN32
   /**
    * @brief An optional user to become the new owner of any open file logs. Used when
    *        changing process running user to ensure logs can still be written to.
    */
   boost::optional<core::system::User> newUser;
   bool chownLogDir;
#endif
};

/**
 * @brief Log delimiting character which may be used for custom log formatting.
 */
constexpr char s_delim = ';';

/**
 * @enum LogLevel
 * @brief Enum which represents the level of detail at which to log messages.
 */
enum class LogLevel
{
   OFF = 0,       // No messages will be logged.
   ERR = 1,       // Error messages will be logged.
   WARN = 2,      // Warning and error messages will be logged.
   INFO = 3,      // Info, warning, and error messages will be logged.
   DEBUG = 4      // All messages will be logged.
};

/**
 * @brief Convenience type for log message properties.
 * Using a boost::any instead of a boost::variant allows access to json
 * as it is currently impossible to include json types here because that
 * would cause a circular dependency.
 */
typedef std::vector<std::pair<std::string, boost::any>> LogMessageProperties;

/**
 * @enum LogMessageFormatType
 * @brief Enum which represents the format type for log messages.
 */
enum class LogMessageFormatType
{
   PRETTY = 0,   // A human-readable single line log message format
   JSON   = 1    // A JSON format, one JSON object per line
};

/**
 * @brief Helper function which cleans the log delimiter character from a string.
 *
 * @param in_str    The string to be cleaned
 *
 * @return The cleaned string.
 */
std::string cleanDelimiters(const std::string& in_str);

/**
 * @brief Sets the program ID for the logger.
 *
 * @param in_programId       The ID of the program.
 */
void setProgramId(const std::string& in_programId);

/**
 * @brief Adds an un-sectioned log destination to the logger.
 *
 * If a duplicate destination is added, the duplicate will be ignored.
 *
 * @param in_destination    The destination to add.
 */
void addLogDestination(const std::shared_ptr<ILogDestination>& in_destination);

/**
 * @brief Adds a sectioned log destination to the logger.
 *
 * If a duplicate destination is added, the duplicate will be ignored.
 * The log destination will be registered to only record log messages with that section.
 * A log destination may be added to multiple sections and as an un-sectioned log.
 *
 * @param in_destination    The destination to add.
 * @param in_section        The name of the log section to which this logger is assigned.
 *
 */
void addLogDestination(const std::shared_ptr<ILogDestination>& in_destination, const std::string& in_section);

/**
 * @brief Returns whether or not a file log destination is configured.
 *
 * @return Whether or not a file log destination is configured.
 */
bool hasFileLogDestination();

/**
 * @brief Returns whether or not a stderr log destination is configured.
 *
 * @return Whether or not a stderr log destination is configured.
 */
bool hasStderrLogDestination();

/**
 * @brief Use to write code conditioned on whether logging is configured or not
 *
 * @return true if log messages at this level will be displayed.
 */
bool isLogLevel(log::LogLevel level);

/**
 * @brief Replaces logging delimiters with ' ' in the specified string.
 *
 * @param in_toClean    The string from which to clean logging delimiters.
 *
 * @return The cleaned string.
 */
std::string cleanDelims(const std::string& in_toClean);

/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::ERR, no log will be written.
 *
 * @param in_error      The error to log.
 */
void logError(const Error& in_error);

/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::ERR, no log will be written.
 *
 * @param in_error              The error to log.
 * @param in_errorLocation      A location higher in the stack than the ErrorLocation in in_error. Provides more
 *                              context.
 */
void logError(const Error& in_error, const ErrorLocation& in_location);

/**
 * @brief Logs an error as a warning to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::WARN, no log will be written.
 *
 * @param in_error      The error to log as a warning.
 */
void logErrorAsWarning(const Error& in_error);

/**
 * @brief Logs an error as an info message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::INFO, no log will be written.
 *
 * @param in_error      The error to log as an info message.
 */
void logErrorAsInfo(const Error& in_error);

/**
 * @brief Logs an error as a debug message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::DEBUG, no log will be written.
 *
 * @param in_error      The error to log as a debug message.
 */
void logErrorAsDebug(const Error& in_error);

/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::ERR, no log will be written.
 *
 * @param in_message        The message to log as an error.
 * @param in_section        The section of the log that the message belongs in. Default: no section.
 */
void logErrorMessage(const std::string& in_message, const std::string& in_section = std::string());

/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::ERR, no log will be written.
 *
 * @param in_message        The message to log as an error.
 * @param in_location       The location from which the error message was logged.
 */
void logErrorMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::ERR, no log will be written.
 *
 * @param in_message        The message to log as an error.
 * @param in_section        The section of the log that the message belongs in.
 * @param in_properties     The log message properties to log.
 * @param in_location       The location from which the message was logged.
 */
void logErrorMessage(const std::string& in_message,
                     const std::string& in_section,
                     const boost::optional<LogMessageProperties>& in_properties,
                     const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs a warning message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::WARN, no log will be written.
 *
 * @param in_message      The message to log as a warning.
 * @param in_section      The section of the log that the message belongs in. Default: no section.
 */
void logWarningMessage(const std::string& in_message, const std::string& in_section = std::string());

/**
 * @brief Logs a warning message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::WARN, no log will be written.
 *
 * @param in_message      The message to log as a warning.
 * @param in_location     The location from which the error message was logged.
 */
void logWarningMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs a warning message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::WARN, no log will be written.
 *
 * @param in_message      The message to log as a warning.
 * @param in_section      The section of the log that the message belongs in.
 * @param in_properties   The log message properties to log.
 * @param in_location     The location from which the message was logged.
 */
void logWarningMessage(const std::string& in_message,
                       const std::string& in_section,
                       const boost::optional<LogMessageProperties>& in_properties,
                       const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs a debug message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::DEBUG, no log will be written.
 *
 * @param in_message      The message to log as a debug message.
 * @param in_section      The section of the log that the message belongs in. Default: no section.
 */
void logDebugMessage(const std::string& in_message, const std::string& in_section = std::string());

/**
 * @brief Logs a debug message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::DEBUG, no log will be written.
 *
 * @param in_message      The message to log as a debug message.
 * @param in_location     The location from which the error message was logged.
 */
void logDebugMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs a debug message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::DEBUG, no log will be written.
 *
 * @param in_message      The message to log as a debug message.
 * @param in_section      The section of the log that the message belongs in.
 * @param in_properties   The log message properties to log.
 * @param in_location     The location from which the message was logged.
 */
void logDebugMessage(const std::string& in_message,
                     const std::string& in_section,
                     const boost::optional<LogMessageProperties>& in_properties,
                     const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs a debug message to all registered destinations by invoking an action.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::DEBUG, no log will be written.
 *
 * @param in_action       The action that will construct the log message if a logger is configured with debug level.
 * @param in_section      The section of the log that the message belongs in.
 */
void logDebugAction(const boost::function<std::string(boost::optional<LogMessageProperties>*)>& in_action,
                    const std::string& in_section = std::string());

/**
 * @brief Logs an info message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::INFO, no log will be written.
 *
 * @param in_message      The message to log as an info message.
 * @param in_section      The section of the log that the message belongs in. Default: no section.
 */
void logInfoMessage(const std::string& in_message, const std::string& in_section = std::string());

/**
 * @brief Logs an info message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::INFO, no log will be written.
 *
 * @param in_message      The message to log as an info message.
 * @param in_location     The location from which the error message was logged.
 */
void logInfoMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs an info message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::INFO, no log will be written.
 *
 * @param in_message      The message to log as an info message.
 * @param in_section      The section of the log that the message belongs in.
 * @param in_properties   The log message properties to log.
 * @param in_location     The location from which the error message was logged.
 */
void logInfoMessage(const std::string& in_message,
                    const std::string& in_section,
                    const boost::optional<LogMessageProperties>& in_properties,
                    const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs an entire log message as is with an annotation indicating it came from source.
 *
 * If no destinations are registered, no log will be written.
 *
 * @param in_source       The source of the log message.
 * @param in_message      The log entire log message (as would be logged by the logging engine).
 */
void logPassthroughMessage(const std::string& in_source, const std::string& in_message);

/**
 * @brief Refreshes all log destinations. May be used after fork to prevent stale file handles.
 *
 * @param in_refreshParams   Refresh params to use when refreshing the log destinations (if applicable).
 */
void refreshAllLogDestinations(const log::RefreshParams& in_refreshParams = log::RefreshParams());

/**
 * @brief Removes a log destination from the logger.
 *
 * If a log destination does not exist with the given ID, no destination will be removed.
 * The log will be removed from the set of default log destinations as well as any sections it has been registered to,
 * if in_section is empty. If in_section is not empty, the log will be removed only from that section.
 *
 * @param in_destinationId   The ID of the destination to remove.
 * @param in_section         The name of the section from which to remove the log. Default: all sections.
 */
void removeLogDestination(const std::string& in_destinationId, const std::string& in_section = std::string());

/**
 * @brief Removes log destinations that are marked as reloadable from the logger. These loggers can then be reinitialized
 *        and re-registered with the logger to update the desired changes to the logging system.
 */
void removeReloadableLogDestinations();

/**
 * @brief Writes an error to the specified output stream.
 *
 * @param in_error      The error to write.
 * @param io_os         The output stream to which to write the error.
 *
 * @return A reference to the specified output stream.
 */
 std::ostream& writeError(const Error& in_error, std::ostream& io_os);
/**
 * @brief Writes an error to string.
 *
 * @param in_error      The error to write.
 *
 * @return The error as a string.
 */
std::string writeError(const Error& in_error);

} // namespace log
} // namespace core
} // namespace rstudio

#endif
