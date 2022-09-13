/*
 * FileLogDestination.hpp
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

#ifndef SHARED_CORE_FILE_LOG_DESTINATION_HPP
#define SHARED_CORE_FILE_LOG_DESTINATION_HPP

#include "ILogDestination.hpp"

#include <string>

#include "PImpl.hpp"
#include "FilePath.hpp"

namespace rstudio {
namespace core {
namespace log {

/**
 * @brief Class which represents the options for a file logger.
 */
class FileLogOptions
{
public:
   /**
    * @brief Constructor.
    *
    * This constructor is intentionally not explicit to allow for conversion from FilePath to FileLogOptions.
    *
    * @param in_directory      The directory in which to create log files.
    */
   FileLogOptions(FilePath in_directory);

   /**
    * @brief Constructor.
    *
    * This constructor is intentionally not explicit to allow for conversion from FilePath to FileLogOptions.
    *
    * @param in_directory      The directory in which to create log files.
    * @param in_warnSyslog     Whether or not to also send warn/error logs to syslog for admin visibility.
    */
   FileLogOptions(FilePath in_directory,
                  bool in_warnSyslog);

   /**
    * @brief Constructor.
    *
    * @param in_directory         The directory in which to create log files.
    * @param in_fileMode          The permissions to set on log files.
    * @param in_maxSizeMb         The maximum size of log files, in MB, before they are rotated and/or overwritten.
    * @param in_rotationDays      The number of days a log file should be kept before being rotated.
    * @param in_maxRotations      The maximum number of allowed rotated log files.
    * @param in_deleteDays        The number of days a rotated log file should be kept before being deleted.
    * @param in_doRotation        Whether to rotate log files or not.
    * @param in_includePid        Whether to include the PID of the process in the log filename.
    * @param in_warnSyslog        Whether or not to also send warn/error logs to syslog for admin visibility.
    * @param in_forceLogDirectory Whether or not the log directory is forced, preventing user override.
    */
   FileLogOptions(
      FilePath in_directory,
      std::string in_fileMode,
      double in_maxSizeMb,
      int in_rotationDays,
      int in_maxRotations,
      int in_deletionDays,
      bool in_doRotation,
      bool in_includePid,
      bool in_warnSyslog,
      bool in_forceLogDirectory);

   /**
    * @brief Gets the number of days a rotated log file should persist before being deleted.
    *
    * @return The number of days a rotated log file should persist before being deleted.
    */
   int getDeletionDays() const;

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
    * @brief Gets whether or not the log directory is forced, preventing user override.
    *
    * @return Whether or not the log directory is forced, preventing user override.
    */
   bool getForceDirectory() const;

   /**
    * @brief Gets the maximum number of allowed rotated log files.
    *
    * @return The maximum number of allowed rotated log files.
    */
   int getMaxRotations() const;

   /**
    * @brief Gets the maximum size of log files, in MB.
    *
    * @return The maximum size of log files, in MB.
    */
   double getMaxSizeMb() const;

   /**
    * @brief Gets the number of days a log file should persist before being rotated.
    *
    * @return The number of days a log file should persist before being rotated.
    */
   int getRotationDays() const;

   /**
    * @brief Returns whether or not to rotate log files before overwriting them.
    *
    * @return True if log files should be rotated; false otherwise.
    */
   bool doRotation() const;

   /**
    * @brief Returns whether or not to include the PID in the log filename.
    *
    * @return True if the PID should be included in the log filename; false otherwise.
    */
   bool includePid() const;

   /**
    * @brief Returns whether or not to also send warn/error logs to syslog for admin visibility.
    *
    * @return True if warn/error logs should be duplicated to syslog; false otherwise.
    */
   bool warnSyslog() const;

   /**
    * @brief Sets the number of days a rotated log file should persist before being deleted.
    *
    * @param in_deletionDays   The number of days a rotated log file should be kept before being deleted.
    */
   void setDeletionDays(int in_deletionDays);

   /**
    * @brief Sets the directory where log files should be written.
    *
    * * @param in_directory      The directory in which to create log files.
    *
    */
   void setDirectory(const FilePath& in_directory);

   /**
    * @brief Sets the permissions with which the log files should be created.
    *
    * @param in_fileMode      The permissions to set on log files.
    *
    */
   void setFileMode(const std::string& in_fileMode);

   /**
    * @brief Sets whether or not the log directory is forced, preventing user override.
    *
    * @param in_forceDirectory  Whether or not the log directory is forced, preventing user override.
    *
    */
   void setForceDirectory(bool in_forceDirectory);

   /**
    * @brief Sets the maximum number of allowed rotated log files.
    *
    * @param in_maxRotations  The maximum number of allowed rotated log files.
    */
   void setMaxRotations(int in_maxRotations);

   /**
    * @brief Sets the maximum size of the log files, in MB.
    *
    * @param in_maxSizeMb  The maximum size of log files, in MB, before they are rotated and/or overwritten.
    */
   void setMaxSizeMb(double in_maxSizeMb);

   /**
    * @brief Sets the number of days a log file should persist before being rotated.
    *
    * @param in_rotationDays   The number of days a log file should be kept before being rotated.
    */
   void setRotationDays(int in_rotationDays);

   /**
    * @brief Sets whether or not to rotate log files before overwriting them.
    *
    * @param in_doRotation     Whether to rotate log files or not.
    */
   void setDoRotation(bool in_doRotation);

   /**
    * @brief Sets whether or not to include the PID of the process in the log filename.
    *
    * @param in_includePid   Whether to include the PID of the process in the log file name.
    */
   void setIncludePid(bool in_includePid);

   /**
    * @brief Sets whether or not to also send warn/error logs to syslog for admin visibility.
    *
    * @param in_warnSyslog Whether or not to also send warn/error logs to syslog for admin visibility.
    */
   void setWarnSyslog(bool in_warnSyslog);


private:
   // Default values.
   static constexpr const char* s_defaultFileMode = "600";
   static constexpr int s_defaultMaxSizeMb = 2;
   static constexpr int s_defaultRotationDays = 1;
   static constexpr int s_defaultMaxRotations = 100;
   static constexpr int s_defaultDeletionDays = 30;
   static constexpr bool s_defaultDoRotation = true;
   static constexpr bool s_defaultIncludePid = false;
   static constexpr bool s_defaultWarnSyslog = true;
   static constexpr bool s_defaultForceDirectory = false;

   // The directory where log files should be written.
   FilePath m_directory;

   // The permissions to set on log files.
   std::string m_fileMode;

   // The maximum size of log files, in MB.
   double m_maxSizeMb;

   // The number of days a log file should persist before being rotated.
   int m_rotationDays;

   // The maximum number of rotated log files that are allowed to exist.
   int m_maxRotations;

   // The number of days a log file should persist before being deleted.
   int m_deletionDays;

   // Whether to rotate log files or not.
   bool m_doRotation;

   // Whether to include the PID in logs.
   bool m_includePid;

   // Whether to also send warn/error logs to syslog for admin visibility.
   bool m_warnSyslog;

   // Whether or not to force the directory to prevent user override.
   bool m_forceDirectory;
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
    * @param in_id              The ID of this log destination. Must be unique for each log destination.
    * @param in_logLevel        The most detailed level of log to be written to this log file.
    * @param in_formatType      The format type for log messages.
    * @param in_programId       The ID of this program.
    * @param in_logOptions      The options for log file creation and management.
    * @param in_reloadable      Whether or not the destination is reloadable. If so, reloading of logging configuration
    *                           will cause the log destination to be removed. Set this to true only for log destinations
    *                           that are intended to be hot-reconfigurable, such as the global default logger.
    *
    * If the log file cannot be opened, no logs will be written to the file. If there are other log destinations
    * registered an error will be logged regarding the failure.
    */
   FileLogDestination(
      const std::string& in_id,
      LogLevel in_logLevel,
      LogMessageFormatType in_formatType,
      const std::string& in_programId,
      FileLogOptions in_logOptions,
      bool in_reloadable = false);

   /**
    * @brief Destructor.
    */
   ~FileLogDestination() override;

   /**
    * @brief Returns the log destination.
    */
   std::string path();

   /**
    * @brief Refreshes the log destintation. Ensures that the log does not have any stale file handles.
    *
    * @param in_refreshParams   Refresh params to use when refreshing the log destinations (if applicable).
    */
   void refresh(const RefreshParams& in_refreshParams = RefreshParams()) override;

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

} // namespace log
} // namespace core
} // namespace rstudio

#endif
