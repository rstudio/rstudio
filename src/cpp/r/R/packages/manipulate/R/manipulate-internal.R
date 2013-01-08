#
# manipulate-internal.R
#
# Copyright (C) 2009-11 by RStudio, Inc.
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

manipulatorExecute <- function(manipulator)
{
   # evaulate the expression
   result <- withVisible(eval(manipulator$.code, envir = manipulator))

   # emulate the behavior of the console by printing the result if it
   # is visible. this will allow objects returned from e.g. lattice or
   # ggplot plots to be displayed without requiring an explicit print
   # statement, whereas plotting functions like plot (or custom user
   # functions will not print anything assuming they return invisibly.
   if (result$visible)
   {
     # evaluate print in the context of the manipulator's parent
     # environment (typically the global environment if manipulate was
     # entered directly at the consle). this allows the dispatch of the
     # print generic method to find the appropriate class method
     eval(print(result$value), enclos=parent.env(manipulator))
   }
}

manipulatorSave <- function(manipulator, filename)
{
  suppressWarnings(save(manipulator, file=filename))
}

manipulatorLoad <- function(filename)
{
  load(filename)
  return (manipulator)
}

hasActiveManipulator <- function()
{
  .Call(getNativeSymbolInfo("rs_hasActiveManipulator", PACKAGE="")) 
}

activeManipulator <- function()
{
  .Call(getNativeSymbolInfo("rs_activeManipulator", PACKAGE=""))
}

ensureManipulatorSaved <- function()
{
  .Call(getNativeSymbolInfo("rs_ensureManipulatorSaved", PACKAGE="")) 
}

createUUID <- function()
{
  .Call(getNativeSymbolInfo("rs_createUUID", PACKAGE=""))
}

executeAndAttachManipulator <- function(manipulator)
{
  .Call(getNativeSymbolInfo("rs_executeAndAttachManipulator", PACKAGE=""),
        manipulator) 
}

setManipulatorValue <- function(manipulator, name, value)
{
  # assign the user visible value
  assign(name, value, envir = get(".userVisibleValues", envir = manipulator))

  # calculate the underlying value. if this was a picker then lookup the
  # underlying value otherwise use the value passed as-is
  underlyingValue <- value
  controls <- get(".controls", envir = manipulator)
  control <- controls[[name]]
  if (inherits(control, "manipulator.picker"))
    underlyingValue <- (control$values[[value]])

  # assign the value
  assign(name, underlyingValue, envir = manipulator)
}

userVisibleValues <- function(manipulator, variables)
{
  mget(variables, envir = get(".userVisibleValues", envir = manipulator))
}

buttonNames <- function(manipulator)
{
  if (exists(".buttonNames", envir = manipulator))
    get(".buttonNames", envir = manipulator)
  else
    character()
}

trackingMouseClicks <- function(manipulator)
{
  exists(".mouseClick", envir = manipulator)
}

setMouseClick <- function(manipulator,
                          deviceX,
                          deviceY,
                          userX,
                          userY,
                          ndcX,
                          ndcY)
{
  mouseClick <- list(deviceX = deviceX,
                     deviceY = deviceY,
                     userX = userX,
                     userY = userY,
                     ndcX = ndcX,
                     ndcY = ndcY)
  assign(".mouseClick", mouseClick, envir = manipulator)
}

clearMouseClick <- function(manipulator)
{
  assign(".mouseClick", NULL, envir = manipulator)
}

resolveVariableArguments <- function(args)
{
  # if the first argument is an unnamed list then just use this list
  if ( (length(args) == 1L) &&
       is.list(args[[1L]])  &&
       (is.null(names(args)) || (names(args)[[1L]] == "")) )
  {
    return (args[[1L]])
  }
  else
  {
    return (args)
  }
}







