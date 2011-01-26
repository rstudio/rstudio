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

.rs.addFunction( "manipulator.execute", function(manipulator)
{
  eval(manipulator$manip_code, envir=manipulator, enclos=globalenv())
})

.rs.addFunction( "manipulator.save", function(manipulator, filename)
{
   save(manipulator, file=filename)
})

.rs.addFunction( "manipulator.load", function(filename)
{
   load(filename)
   return (manipulator)
})


.rs.addGlobalFunction( "slider", function(value, min, max, label = NULL)
{
  # validate inputs
  if (!is.numeric(value) || !is.numeric(min) || !is.numeric(max))
    stop("min, max, amd value must all be numeric values")
  else if (value < min)
    stop(paste(type, "value", value, "is less than the specified minimum"))
  else if (value > max)
    stop(paste(type, "value", value, "is greater than the specified maximum"))
  else if (min > max)
    stop(paste(type, "maximum is greater than minimum"))
  else if ( !is.null(label) && !is.character(label) )
    stop("label is not a character value")
  
  # create slider and return it
  slider <- list(type = 0,
                 min = min,
                 max = max,
                 initialValue = value,
                 label = label)
  class(slider) <- "manipulator.slider"
  return (slider)
})

.rs.addGlobalFunction( "picker", function(value, choices, label = NULL)
{
  # validate inputs
  if ( !is.character(value) || (length(value) != 1) )
    stop("value must be a character object")
  else if ( !is.character(choices) )
    stop("choices is not a character vector")
  else if ( length(choices) < 1 )
    stop("choices must contain at least one value")
  else if ( !(value %in% choices) )
    stop("value doesn't match one of the supplied choices") 
  else if ( !is.null(label) && !is.character(label) )
    stop("label is not a character value")
   
  picker <- list(type = 1,
                 choices = choices,
                 initialValue = value,
                 label = label)
  class(picker) <- "manipulator.picker"
  return (picker) 
})

.rs.addGlobalFunction( "checkbox", function(value, label)
{
  # validate inputs
  if ( !is.null(label) && !is.character(label) )
     stop("label is not a character value")  
  else if ( !is.logical(value) )
    stop("value must be a logical")
    
  # create checkbox and return it
  checkbox <- list(type = 2,
                   initialValue = value,
                   label = label)
  class(checkbox) <- "manipulator.checkbox"
  return (checkbox)
})


.rs.addGlobalFunction( "manipulator.changed", function()
{
  if ( .Call("rs_hasActiveManipulator") )
  {
    .Call("rs_activeManipulator")$manip_changed
  }
  else
  {
    stop("no plot manipulator currently active")
  }
})

.rs.addGlobalFunction( "manipulator.state", function()
{
  if ( .Call("rs_hasActiveManipulator") )
  {
    .Call("rs_activeManipulator")$manip_state
  }
  else
  {
    stop("no plot manipulator currently active")
  }
})

.rs.addGlobalFunction( "manipulator.setState", function(state)
{
  if ( !is.list(state) )
    stop("manipulator state must be a list")
  
  if ( .Call("rs_hasActiveManipulator") )
  {
     .Call("rs_setManipulatorState", state)
     invisible(NULL)
  }
  else
  {
    stop("no plot manipulator currently active")
  }
})

.rs.addGlobalFunction( "manipulate", function(code, ...)
{
  # TODO: validate that all controls have variables in the expression

  # create new list container for the manipulator
  manipulator <- list()
  class(manipulator) <- "manipulator"
  
  # save the unevaluated expression as the code
  manipulator$manip_code <- substitute(code) 
  
  # save a human readable version of the code (specify control = NULL
  # to make the display as close to the original text as possible)
  manipulator$manip_codeAsText <- deparse(substitute(code), control = NULL)

  # get the controls and their names
  controls <- list(...)
  controlNames <- names(controls)
 
  # save the controls and their names into the manipulator
  manipulator$manip_controls <- controls
  manipulator$manip_variables <- controlNames
  
  # establish changed and state list elements
  manipulator$manip_changed <- character()
  manipulator$manip_state <- list()
  
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
    if ( ! (class(control) %in% c("manipulator.slider",
                                  "manipulator.picker",
                                  "manipulator.checkbox")) )
    {
      stop(paste("argument", name, "is not a control"))
    }
      
    # assign the control's default into the list
    manipulator[name] <- control$initialValue
  }

  # execute the manipulator -- will execute the code and attach it
  # to the first plot (if any) generated by the execution
  .Call("rs_executeAndAttachManipulator", manipulator)
  
  # return invisibly
  invisible(NULL)
})













