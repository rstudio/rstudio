#
# SessionBreakpoints.R
#
# Copyright (C) 2009-12 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

# this function is used to get a list of function steps that correspond to
# line numbers in a given file.
.rs.addFunction("getFunctionSteps", function(fileName, lineNumbers)
{
    return(lapply(lineNumbers, function(lineNumber)
    {
        findLineNum(fileName, lineNumber, envir = globalenv())
    }))
})

.rs.addFunction("setFunctionBreakpoints", function(functionName, steps)
{
    if (length(steps) == 0)
    {
        untrace(functionName)
    }
    else
    {
        trace(
            what = functionName,
            at = steps,
            tracer = browser,
            print = FALSE)
    }
})

.rs.addJsonRpcHandler("set_function_breakpoints", function(functionName, steps)
{
    .rs.setFunctionBreakpoints(functionName, steps)
})

.rs.addJsonRpcHandler("get_function_steps", function(fileName, lineNumbers)
{
    results <- .rs.getFunctionSteps(fileName, lineNumbers)
    formattedResults <- data.frame(
        line = numeric(0),
        name = character(0),
        at = numeric(0),
        stringsAsFactors = FALSE)
    for (result in results)
    {
        for (entry in result)
        {
            formattedResult <- list(
                line = entry$line,
                name = entry$name,
                at = entry$at)
            formattedResults <- rbind(formattedResults, formattedResult)
        }
    }
    formattedResults$name <- as.character(formattedResults$name)
    return(formattedResults)
})

