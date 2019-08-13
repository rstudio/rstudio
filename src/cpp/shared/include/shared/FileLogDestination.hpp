/*
 * FileLogDestination.hpp
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

#ifndef SHARED_FILE_LOG_DESTINATION_HPP
#define SHARED_FILE_LOG_DESTINATION_HPP

#include "ILogDestination.hpp"

#include <string>

#include "PImpl.hpp"
#include "FilePath.hpp"

namespace rstudio {
namespace shared {

/**
 * @brief Class which represents the options for a file logger.
 */
class FileLogOptions
{
public:
   /**
   * @brief Constructor.
   *
   * @param in_directory      The directory in which to create log files.
   */
   FileLogOptions(FilePath in_directory);

   /**
   * @brief Constructor.
   *
   * @param in_directory      The directory in which to create log files.
   * @param in_fileMode       The permissions to set on log files.
   * @param in_maxSizeMb      The maximum size of log files, in MB, before they are rotated and/or overwritten.
   * @param in_doRotation     Whether to rotate log files or not.
   */
   FileLogOptions(
      FilePath in_directory,
      std::string in_fileMode,
      double in_maxSizeMb,
      bool in_doRotation);

   /**
   * @brief Gets the directory where log files should be written.
   *
   * @return The directory where log files should be written.
   */
   const FilePath& getDirectory() const;

   /**
    * @brief Gets the permissions with which log files should be created.
    *
    * @return The permissions with which log files should be created.
    */
   const std::string& getFileMode() const;

   /**
    * @brief Gets the maximum size of log files, in MB.
    *
    * @return The maximum size of log files, in MB.
    */
   double getMaxSizeMb() const;

   /**
    * @brief Returns whether or not to rotate log files before overwriting them.
    *
    * @return True if log files should be rotated; false otherwise.
    */
   bool doRotation() const;

private:
   // Default values.
   static constexpr const char* kDefaultFileMode = "666";
   static constexpr int kDefaultMaxSizeMb = 2;
   static constexpr bool kDefaultDoRotation = true;

   // The directory where log files should be written.
   FilePath m_directory;

   // The permissions to set on log files.
   std::string m_fileMode;

   // The maximum size of log files, in MB.
   double m_maxSizeMb;

   // Whether to rotate log files or not.
   bool m_doRotation;
};

/**
 * @brief Class which allows sending log messages to a file.
 */
class FileLogDestination : public ILogDestination
{
public:
   /**
    * @brief Constructor.
    *
    * @param in_id              The ID of this log destination. Must be unique for each file log destination and > 100.
    * @param in_programId       The ID of this program.
    * @param in_logOptions      The options for log file creation and management.
    *
    * If the log file cannot be opened, no logs will be written to the file. If there are other log destinations
    * registered an error will be logged regarding the failure.
    */
   FileLogDestination(unsigned int in_id, std::string in_programId, FileLogOptions in_logOptions);

   /**
    * @brief Destructor.
    */
    ~FileLogDestination() override;

   /**
    * @brief Gets the unique ID of this file log destination.
    *
    * @return The unique ID of this file log destination.
    */
   unsigned int getId() const override;

   /**
    * @brief Writes a message to the log file.
    *
    * @param in_logLevel    The log level of the message to write. Filtering is done prior to this call. This is for
    *                       informational purposes only.
    * @param in_message     The message to write to the log file.
    */
   void writeLog(LogLevel in_logLevel, const std::string& in_message) override;

private:
   PRIVATE_IMPL_SHARED(m_impl);
};

} // namespace shared
} // namespace rstudio

#endif
