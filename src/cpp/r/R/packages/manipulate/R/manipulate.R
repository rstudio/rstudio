#
# manipulate.R
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


slider <- function(min, max, value = min, label = NULL, step = NULL, ticks = TRUE)
{
  # validate inputs
  if (!is.numeric(value) || !is.numeric(min) || !is.numeric(max))
    stop("min, max, amd value must all be numeric values")
  else if (value < min)
    stop(paste("slider value", value, "is less than the specified minimum"))
  else if (value > max)
    stop(paste("slider value", value, "is greater than the specified maximum"))
  else if (min > max)
    stop(paste("slider maximum is greater than minimum"))
  else if ( !is.null(step) )
  {
    if ( !is.numeric(step) )
      stop("step is not a numeric value")
    if ( step > (max - min) )
      stop("step is greater than range")
  }
  else if ( !is.logical(ticks) )
    stop("ticks is not a logical value")
  else if ( !is.null(label) && !is.character(label) )
    stop("label is not a character value")
  
  # serialize "default" step as -1
  if ( is.null(step) )
    step <- -1
  
  # create slider and return it
  slider <- list(type = 0,
                 min = min,
                 max = max,
                 initialValue = value,
                 label = label,
                 step = step,
                 ticks = ticks)
  class(slider) <- "manipulator.slider"
  return (slider)
}

picker <- function(..., choices = list(), value = NULL, label = NULL)
{
  # get values
  values <- append(choices, list(...))
  
  # get value names
  valueNames <- names(values)
  if (is.null(valueNames))
    valueNames <- character(length(values))
 
  # default missing names to choice values
  missingNames <- valueNames == ""
  valueNames[missingNames] <- paste(values)[missingNames]
  names(values) <- valueNames
  
  # validate inputs
  if ( length(values) < 1 )
  {
    stop("picker choices must contain at least one value")
  }
  else if ( length(valueNames) != length(unique(valueNames)) )
  {
    stop("picker choices must have unique names (duplicate detected)")
  }
  else if ( !is.null(value) )
  {
    if (length(value) != 1)
      stop("value must be a single object")
    else if ( !(value %in% valueNames) )
      stop("value doesn't match one of the supplied choices") 
  }
  else if ( !is.null(label) && !is.character(label) )
  {
    stop("label is not a character value")
  }

  # provide default value if necessary
  if ( is.null(value) )
    value <- valueNames[1]

  # create picker
  picker <- list(type = 1,
                 choices = valueNames,
                 values = values,
                 initialValue = value,
                 label = label)
  class(picker) <- "manipulator.picker"
  return (picker) 
}

checkbox <- function(value = FALSE, label = NULL)
{
  # validate inputs
  if ( !is.logical(value) )
    stop("value must be a logical")
  else if ( !is.null(label) && !is.character(label) )
    stop("label is not a character value")
  
  # create checkbox and return it
  checkbox <- list(type = 2,
                   initialValue = value,
                   label = label)
  class(checkbox) <- "manipulator.checkbox"
  return (checkbox)
}

manipulatorGetState <- function(name)
{
  if ( hasActiveManipulator() )
  {
    value <- NULL
    try(silent = TRUE,
        value <- get(name, 
                     envir = get(".state", envir = activeManipulator()))
    )
    return (value)
  }
  else
  {
    stop("no plot manipulator currently active")
  }
}

manipulatorSetState <- function(name, value)
{
  if ( hasActiveManipulator() )
  {
     assign(name, value, envir = get(".state", envir = activeManipulator()))
     ensureManipulatorSaved()
     invisible(NULL)
  }
  else
  {
    stop("no plot manipulator currently active")
  }
}

manipulate <- function(expr, ..., controls = list())
{
  # validate inputs
  if (!is.list(controls))
    stop("controls must be a list")

  # create new list container for the manipulator
  manipulator <- new.env(parent = parent.frame())
  assign(".id", createUUID(), envir = manipulator)
  
  # save the unevaluated expression as the code
  assign(".code", substitute(expr), envir = manipulator) 
  
  # save a human readable version of the code (specify control = NULL
  # to make the display as close to the original text as possible)
  assign(".codeAsText", deparse(substitute(expr), control = NULL), envir = manipulator)
  
  # get the controls and control names
  controls <- append(controls, list(...))
  controlNames <- names(controls)
 
  # save the controls and their names into the manipulator
  assign(".controls", controls, envir = manipulator)
  assign(".variables", controlNames, envir = manipulator)
  
  # establish state
  assign(".state", new.env(parent = globalenv()), envir = manipulator)
  
  # iterate over the names and controls, adding the default values to the env
  for (name in names(controls))
  {
    # check the name
    if (name == "")
      stop("all controls passed to manipulate must be named")
    
    # confirm that this is in fact a control
    control <- controls[[name]]
    if ( ! (class(control) %in% c("manipulator.slider",
                                  "manipulator.picker",
                                  "manipulator.checkbox")) )
    {
      stop(paste("argument", name, "is not a control"))
    }
      
    # assign the control's default into the list
    value <- manipulatorControlValue(manipulator, name, control$initialValue)
    assign(name, value, envir = manipulator)
  }

  # execute the manipulator -- will execute the code and attach it
  # to the first plot (if any) generated by the execution
  executeAndAttachManipulator(manipulator)
  
  # return invisibly
  invisible(NULL)
}


