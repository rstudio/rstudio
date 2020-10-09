/*
 * FileLogDestination.cpp
 * 
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
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

#include <shared_core/FileLogDestination.hpp>

#include <boost/thread.hpp>

#include <vector>

#include <shared_core/Error.hpp>
#include <shared_core/Logger.hpp>

namespace rstudio {
namespace core {
namespace log {

// FileLogOptions ======================================================================================================
FileLogOptions::FileLogOptions(FilePath in_directory) :
   m_directory(std::move(in_directory)),
   m_fileMode(s_defaultFileMode),
   m_maxSizeMb(s_defaultMaxSizeMb),
   m_doRotation(s_defaultDoRotation),
   m_includePid(s_defaultIncludePid)
{
}

FileLogOptions::FileLogOptions(
   FilePath in_directory,
   std::string in_fileMode,
   double in_maxSizeMb,
   bool in_doRotation,
   bool in_includePid) :
      m_directory(std::move(in_directory)),
      m_fileMode(std::move(in_fileMode)),
      m_maxSizeMb(in_maxSizeMb),
      m_doRotation(in_doRotation),
      m_includePid(in_includePid)
{
}

const FilePath& FileLogOptions::getDirectory() const
{
   return m_directory;
}

const std::string& FileLogOptions::getFileMode() const
{
   return m_fileMode;
}

double FileLogOptions::getMaxSizeMb() const
{
   return m_maxSizeMb;
}

bool FileLogOptions::doRotation() const
{
   return m_doRotation;
}

bool FileLogOptions::includePid() const
{
   return m_includePid;
}

// FileLogDestination ==================================================================================================
struct FileLogDestination::Impl
{
   Impl(unsigned int in_id, const std::string& in_name, FileLogOptions in_options) :
      LogOptions(std::move(in_options)),
      LogName(in_name + ".log"),
      RotatedLogName(in_name + ".old.log"),
      Id(in_id)
   {
      LogOptions.getDirectory().ensureDirectory();
   };

   ~Impl()
   {
      closeLogFile();
   }

   void closeLogFile()
   {
      if (LogOutputStream)
      {
         LogOutputStream->flush();
         LogOutputStream.reset();
      }
   }

   // Returns true if the log file was opened, false otherwise.
   bool openLogFile()
   {
      // We can't safely log in this function.
      Error error = LogOptions.getDirectory().completeChildPath(LogName, LogFile);
      if (error)
         return false;

      error = LogFile.ensureFile();
      if (error)
         return false;

      error = LogFile.openForWrite(LogOutputStream, false);
      if (error)
         return false;

      return true;
   }

   // Returns true if it is safe to log; false otherwise.
   bool rotateLogFile()
   {
      // Calculate the maximum size in bytes.
      const uintmax_t maxSize = 1048576.0 * LogOptions.getMaxSizeMb();

      // Only rotate if we're configured to rotate.
      if (LogOptions.doRotation())
      {
         // Convert MB to B for comparison.
         if (LogFile.getSize() >= maxSize)
         {
            FilePath rotatedLogFile = FilePath(LogOptions.getDirectory()).completeChildPath(RotatedLogName);

            // We can't safely log errors in this function because we'll end up in an infinitely recursive
            // rotateLogFile() call.
            Error error = rotatedLogFile.remove();
            if (error)
               return false;

            // Close the existing log file and then move it.
            closeLogFile();
            error = LogFile.move(rotatedLogFile);
            if (error)
               return false;

            // Now re-open the log file.
            if (!openLogFile())
               return false;
         }

         // we only sucessfully rotated (and thus are safe to log) if the
         // file size is now lower than the limit
         return LogFile.getSize() < maxSize;
      }
      else
      {
         // we are configured not to rotate logs, which means the log file can grow unboundedly large
         // thus, we are safe to log
         return true;
      }
   }

   FileLogOptions LogOptions;
   FilePath LogFile;
   std::string LogName;
   std::string RotatedLogName;
   boost::mutex Mutex;
   unsigned int Id;
   std::shared_ptr<std::ostream> LogOutputStream;
};

FileLogDestination::FileLogDestination(
   unsigned int in_id,
   LogLevel in_logLevel,
   const std::string& in_programId,
   FileLogOptions in_logOptions) :
      ILogDestination(in_logLevel),
      m_impl(new Impl(in_id, in_programId, std::move(in_logOptions)))
{
}

FileLogDestination::~FileLogDestination()
{
   if (m_impl->LogOutputStream.get())
      m_impl->LogOutputStream->flush();
}

unsigned int FileLogDestination::getId() const
{
   return m_impl->Id;
}

void FileLogDestination::writeLog(LogLevel in_logLevel, const std::string& in_message)
{
   // Don't write logs that are more detailed than the configured maximum.
   if (in_logLevel > m_logLevel)
      return;

   // Lock the mutex before attempting to write.
   boost::unique_lock<boost::mutex> lock(m_impl->Mutex);

   // Open the log file if it's not open. If it fails to open, log nothing.
   if (!m_impl->LogOutputStream && !m_impl->openLogFile())
      return;

   // Rotate the log file if necessary. If it fails to rotate, log nothing.
   if (!m_impl->rotateLogFile())
      return;

   (*m_impl->LogOutputStream) << in_message;
   m_impl->LogOutputStream->flush();
}

} // namespace log
} // namespace core
} // namespace rstudio

