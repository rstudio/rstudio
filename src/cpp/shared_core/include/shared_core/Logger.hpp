/*
 * Logger.hpp
 * 
 * Copyright (C) 2019 by RStudio, Inc.
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

#include "PImpl.hpp"

namespace rstudio {
namespace core {

class Error;
class ErrorLocation;

} // namespace rstudio
} // namesapce core


namespace rstudio {
namespace core {
namespace log {

class ILogDestination;

/**
 * @brief Log delimiting character which may be used for custom log formatting.
 */
constexpr char s_delim = ';';

/**
 * @brief Enum which represents the level of detail at which to log messages.
 */
enum class LogLevel
{
   OFF = 0,       // No messages will be logged.
   ERROR = 1,     // Error messages will be logged.
   WARNING = 2,   // Warning and error messages will be logged.
   INFO = 3,      // Info, warning, and error messages will be logged.
   DEBUG = 4      // All messages will be logged.
};

/**
 * @brief Class which represents a log section.
 */
struct LogSection
{
   /**
    * @brief Constructor.
    *
    * Intentionally not explicit.
    *
    * @param in_name        The name of the section.
    */
   LogSection(const std::string& in_name);

   /**
    * @brief Constructor.
    *
    * @param in_name        The name of the section.
    * @param in_logLevel    The maximum log level for this section.
    */
   LogSection(const std::string& in_name, LogLevel in_logLevel);

   /**
    * @brief Less-than operator. A log section is less than another if its name is alphabetically less than the other.
    *
    * @param in_other   The log section to which to compare this log section.
    *
    * @return True if this log section is less, alphabetically by name, than in_other; false otherwise.
    */
   bool operator<(const LogSection& in_other) const;

   /**
    * @brief Equality operator. A log section is equal to another if their names are equal.
    *
    * @param in_other   The log section to which to compare this log section.
    *
    * @return True if the two log sections have the same name; false otherwise.
    */
   bool operator==(const LogSection& in_other) const;

   // Private implementation of LogSection.
   PRIVATE_IMPL_SHARED(Impl);
};

/**
 * @brief Sets the program ID for the logger.
 *
 * @param in_programId       The ID of the program.
 */
void setProgramId(const std::string& in_programId);

/**
 * @brief Sets the level of detail at which to log messages.
 *
 * @param in_logLevel    The level of detail at which to log messages.
 */
void setLogLevel(LogLevel in_logLevel);
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
 */
void addLogDestination(const std::shared_ptr<ILogDestination>& in_destination, const LogSection& in_section);

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
 * If the configured log level is below LogLevel::ERROR, no log will be written.
 *
 * @param in_error      The error to log.
 */
void logError(const Error& in_error);

/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::ERROR, no log will be written.
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
 * If the configured log level is below LogLevel::WARNING, no log will be written.
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
 * If the configured log level is below LogLevel::ERROR, no log will be written.
 *
 * @param in_message        The message to log as an error.
 * @param in_section        The section of the log that the message belongs in. Default: no section.
 */
void logErrorMessage(const std::string& in_message, const std::string& in_section = std::string());

/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::ERROR, no log will be written.
 *
 * @param in_message        The message to log as an error.
 * @param in_location       The location from which the error message was logged.
 */
void logErrorMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::ERROR, no log will be written.
 *
 * @param in_message        The message to log as an error.
 * @param in_section        The section of the log that the message belongs in.
 * @param in_location       The location from which the error message was logged.
 */
void logErrorMessage(const std::string& in_message, const std::string& in_section, const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs a warning message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::WARNING, no log will be written.
 *
 * @param in_message      The message to log as a warning.
 * @param in_section      The section of the log that the message belongs in. Default: no section.
 */
void logWarningMessage(const std::string& in_message, const std::string& in_section = std::string());

/**
 * @brief Logs a warning message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::WARNING, no log will be written.
 *
 * @param in_message      The message to log as a warning.
 * @param in_location     The location from which the error message was logged.
 */
void logWarningMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs a warning message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the configured log level is below LogLevel::WARNING, no log will be written.
 *
 * @param in_message      The message to log as a warning.
 * @param in_section      The section of the log that the message belongs in.
 * @param in_location     The location from which the error message was logged.
 */
void logWarningMessage(const std::string& in_message, const std::string& in_section, const ErrorLocation& in_loggedFrom);

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
 * @param in_location     The location from which the error message was logged.
 */
void logDebugMessage(const std::string& in_message, const std::string& in_section, const ErrorLocation& in_loggedFrom);

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
 * @param in_location     The location from which the error message was logged.
 */
void logInfoMessage(const std::string& in_message, const std::string& in_section, const ErrorLocation& in_loggedFrom);

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
void removeLogDestination(unsigned int in_destinationId, const std::string& in_section = std::string());

/**
 * @brief Writes an error to the specified output stream.
 *
 * @param in_error      The error to write.
 * @param io_os         The ouptut stream to which to write the error.
 *
 * @return A reference to the specified output stream.
 */
 std::ostream& writeError(const Error& in_error, std::ostream& io_os);

} // namespace log
} // namespace core
} // namespace rstudio

#endif
