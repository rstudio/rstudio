#
# Manipulate.R
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction( "manipulator.createControl", function(type, value, min, max)
{
   # validate inputs
  if ( ! (type %in% c("slider")) )
    stop(paste("invalid control type:", type))
  else if (value < min)
    stop(paste(type, "value", value, "is less than the specified minimum"))
  else if (value > max)
    stop(paste(type, "value", value, "is greater than the specified maximum"))
  else if (min > max)
    stop(paste(type, "maximum is greater than minimum"))
  
  # create control and return it
  control <- list(type = type,
                  value = value,
                  min = min,
                  max = max)
  class(control) <- "manipulate.control"
  return (control)
})

.rs.addGlobalFunction( "slider", function(value, min, max)
{
   .rs.manipulator.createControl("slider", value, min, max)
})

.rs.addFunction( "manipulator.execute", function(manipulator)
{
  code <- get("code", envir=manipulator)
  eval(code, envir=manipulator)  
})

.rs.addGlobalFunction( "manipulate", function(code, ...)
{
  # create new environment for the manipulator
  manipulator <- new.env() 
  
  # save the unevaluated expression as the code
  assign("code", substitute(code), envir=manipulator) 
  
  # get the controls and their names, then save them into the env
  controls <- list(...)
  controlNames <- names(controls)
  assign("controls", controls, envir=manipulator)  
 
  # iterate over the names and controls, adding the default values to the env
  c = 1 
  for (control in controls)
  {
    # get the name and bump the control index
    name <- controlNames[c]
    if (name == "")
      stop("all controls passed to manipulate must be named")
    c = c + 1  
    
    # confirm that this is in fact a control
    if (class(control) != "manipulate.control")
      stop(paste("argument", name, "is not a control"))
    
    # assign the value
    assign(name, control$value, envir=manipulator)
  }

  # execute the manipulator
  .Call("rs_executeManipulator", manipulator)
  
  # return invisibly
  invisible(NULL)
})













