#
# SessionDebugging.R
#
# Copyright (C) 2024 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.setVar("debugging.activeSink", NULL)

.rs.addFunction("debugging.beginSinkOutput", function(file)
{
   # Open write connection to file.
   conn <- file(file, open = "a")
   .rs.setVar("debugging.activeSink", conn)
   
   # Sink output to this file.
   sink(conn, append = TRUE, type = "output")
   sink(conn, append = TRUE, type = "message")
})

.rs.addFunction("debugging.endSinkOutput", function()
{
   # Close the sinks.
   sink(NULL, type = "output")
   sink(NULL, type = "message")
   
   # Close the fifo connection.
   conn <- .rs.getVar("debugging.activeSink")
   close(conn)
})

.rs.addFunction("debugging.printTraceback", function()
{
   if (requireNamespace("rlang", quietly = TRUE))
   {
      opts <- options("rlang:::visible_bottom" = parent.frame())
      on.exit(options(opts), add = TRUE)
      print(rlang::trace_back())
   }
   else
   {
      print(sys.calls())
   }
})
