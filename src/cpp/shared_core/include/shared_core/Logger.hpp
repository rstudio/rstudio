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
#include <string>

namespace rstudio {
namespace core {

class Error;
class ErrorLocation;
class ILogDestination;

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
 * @brief Adds a log destination to the logger.
 *
 * If a duplicate destination is added, the duplicate will be ignored.
 *
 * @param in_destination     The destination to add.
 */
void addLogDestination(std::unique_ptr<ILogDestination> in_destination);

/**
 * @brief Removes a log destination from the logger.
 *
 * If a log destination does not exist with the given ID, no destination will be removed.
 *
 * @param in_destinationID   The ID of the destination to remove.
 */
void removeLogDestination(unsigned int in_destinationId);

/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the log level is below LogLevel::ERROR, no log will be written.
 *
 * @param in_error      The error to log.
 */
void logError(const Error& in_error);

/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the log level is below LogLevel::ERROR, no log will be written.
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
 * If the log level is below LogLevel::WARNING, no log will be written.
 *
 * @param in_error      The error to log as a warning.
 */
void logErrorAsWarning(const Error& in_error);

/**
 * @brief Logs an error as an info message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the log level is below LogLevel::INFO, no log will be written.
 *
 * @param in_error      The error to log as an info message.
 */
void logErrorAsInfo(const Error& in_error);

/**
 * @brief Logs an error as a debug message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the log level is below LogLevel::DEBUG, no log will be written.
 *
 * @param in_error      The error to log as a debug message.
 */
void logErrorAsDebug(const Error& in_error);
/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the log level is below LogLevel::ERROR, no log will be written.
 *
 * @param in_error              The message to log as an error.
 */
void logErrorMessage(const std::string& in_message);
/**
 * @brief Logs an error to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the log level is below LogLevel::ERROR, no log will be written.
 *
 * @param in_error              The message to log as an error.
 * @param in_loggedFrom         The location from which the error message was logged.
 */
void logErrorMessage(const std::string& in_message, const ErrorLocation& in_loggedFrom);

/**
 * @brief Logs a warning message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the log level is below LogLevel::WARNING, no log will be written.
 *
 * @param in_message      The message to log as a warning.
 */
void logWarningMessage(const std::string& in_message);

/**
 * @brief Logs an info message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the log level is below LogLevel::INFO, no log will be written.
 *
 * @param in_error      The message to log as an info message.
 */
void logInfoMessage(const std::string& in_message);

/**
 * @brief Logs a debug message to all registered destinations.
 *
 * If no destinations are registered, no log will be written.
 * If the log level is below LogLevel::DEBUG, no log will be written.
 *
 * @param in_message      The message to log as a debug message.
 */
void logDebugMessage(const std::string& in_message);

} // namespace core
} // namespace rstudio

#endif
