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

.rs.addFunction( "manipulatorExecute", function(manipulator)
{
  eval(get(".code", envir = manipulator), envir = manipulator)
})

.rs.addFunction( "manipulatorSave", function(manipulator, filename)
{
   suppressWarnings(save(manipulator, file=filename))
})

.rs.addFunction( "manipulatorLoad", function(filename)
{
   load(filename)
   return (manipulator)
})

.rs.addGlobalFunction( "slider", function(value, 
                                          min, 
                                          max,
                                          step = 1,
                                          ticks = FALSE,
                                          label = NULL)
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
  else if ( !is.numeric(step) )
    stop("step is not a numeric value")
  else if ( !is.logical(ticks) )
    stop("ticks is not a logical value")
  else if ( !is.null(label) && !is.character(label) )
    stop("label is not a character value")
  
  # create slider and return it
  slider <- list(type = 0,
                 initialValue = value,
                 min = min,
                 max = max,
                 step = step,
                 ticks = ticks,
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
  if ( !is.character(label) )
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

.rs.addGlobalFunction( "manipulatorGetState", function(name)
{
  if ( .Call("rs_hasActiveManipulator") )
  {
    value <- NULL
    try(silent = TRUE,
        value <- get(name, 
                     envir = get(".state", envir = .Call("rs_activeManipulator")))
    )
    return (value)
  }
  else
  {
    stop("no plot manipulator currently active")
  }
})

.rs.addGlobalFunction( "manipulatorSetState", function(name, value)
{
  if ( .Call("rs_hasActiveManipulator") )
  {
     assign(name, value, envir = get(".state", envir = .Call("rs_activeManipulator")))
     .Call("rs_ensureManipulatorSaved")
     invisible(NULL)
  }
  else
  {
    stop("no plot manipulator currently active")
  }
})

.rs.addGlobalFunction( "manipulate", function(code, ...)
{
  # create new list container for the manipulator
  manipulator <- new.env(parent = parent.frame())
  assign(".id", .rs.createUUID(), envir = manipulator)
  
  # save the unevaluated expression as the code
  assign(".code", substitute(code), envir = manipulator) 
  
  # save a human readable version of the code (specify control = NULL
  # to make the display as close to the original text as possible)
  assign(".codeAsText", deparse(substitute(code), control = NULL), envir = manipulator)
  
  # get the controls and their names
  controls <- list(...)
  controlNames <- names(controls)
 
  # save the controls and their names into the manipulator
  assign(".controls", controls, envir = manipulator)
  assign(".variables", controlNames, envir = manipulator)
  
  # establish state
  assign(".state", new.env(parent = globalenv()), envir = manipulator)
  
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
    assign(name, control$initialValue, envir = manipulator)
  }

  # execute the manipulator -- will execute the code and attach it
  # to the first plot (if any) generated by the execution
  .Call("rs_executeAndAttachManipulator", manipulator)
  
  # return invisibly
  invisible(NULL)
})













