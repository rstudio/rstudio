
-- lua patterns (including capture) used in various handlers
local chunk_begin_pattern = "^{([a-zA-Z0-9_]+)[%s,]+"
local chunk_end_pattern = ".*}.*$"
local xref_label_pattern = "([a-zA-Z0-9/%-]+)"
local param_pattern = "%s*=%s*[\"']([^\"']+)[\"']"

-- state indicating whether an xref write is pending (some of the functions
-- below e.g. Code and DisplayMath set this flag if they discover an xref)
local xref_pending = false

function Para(s)
  
  -- write xref if it's pending
  if xref_pending then
    xref_pending = false
    return s
  
  -- otherwise look for a text reference
  else
    local text_ref_pattern = "^%(ref:[a-zA-Z0-9_]+%)%s+.*$" 
    if string.match(s, text_ref_pattern) then
      return s .. '\n'
    else
      return ''
    end
  end
  
end

-- headers
function Header(lev, s, attr)
  if string.len(attr.id) > 0 then
    return 'h' .. lev .. ':' .. attr.id .. ' ' .. s .. '\n'
  else
    return ''
  end
end

-- code chunks can contain figure, table, or theorem xrefs
function CodeBlock(s, attr)
  
  local chunk_begin = chunk_begin_pattern .. xref_label_pattern
 
  -- fig: start by looking for just fig.cap. if we find that then also look for a quoted value
  local fig_cap_begin = "[ ,].*fig%.cap"
  local _, fig_label = string.match(s, chunk_begin .. fig_cap_begin .. chunk_end_pattern)
  if fig_label then
    local _, _, fig_caption = string.match(s, chunk_begin .. fig_cap_begin .. param_pattern .. chunk_end_pattern)
    if fig_caption then
      return 'fig:' .. fig_label .. ' ' .. fig_caption .. '\n'
    else
      return 'fig:' .. fig_label .. '\n'
    end
  end
  
  -- tab: start by looking for just caption, if we find that then also look for a quoted value
  local tab_caption_begin = ".*}.*kable%s*%(.*caption"
  local _, tab_label = string.match(s, chunk_begin .. tab_caption_begin .. ".*$")
  if (tab_label) then
    local _, _, tab_caption = string.match(s, chunk_begin .. tab_caption_begin .. param_pattern .. ".*$")
    if tab_caption then
       return 'tab:' .. tab_label .. ' ' .. tab_caption .. '\n'
    else
       return 'tab:' .. tab_label .. ' ' .. '\n'
    end
  end
  
  -- thereom
  -- https://bookdown.org/yihui/bookdown/markdown-extensions-by-bookdown.html#theorems
  local theorem_types = 
  {
    theorem = 'thm',
    lemma = 'lem',
    corollary = 'cor',
    proposition = 'prp',
    conjecture = 'cnj',
    definition = 'def',
    example = 'exm',
    exercise = 'exr'
  }

  -- look for a labeled chunk 
  local chunk_type = string.match(s, chunk_begin_pattern .. chunk_end_pattern)

  -- see if it's one of our supported types
  if theorem_types[chunk_type] then
    
    -- set chunk_type to shorter variation
    chunk_type = theorem_types[chunk_type]
    
    -- look for an explicit label 
    local _, chunk_label = string.match(s, chunk_begin_pattern .. "label" .. param_pattern .. chunk_end_pattern)

    -- look for a conventional knitr label
    if not(chunk_label) then
      _, chunk_label = string.match(s, chunk_begin .. chunk_end_pattern)
    end
    
    -- if we found a label it's not 'name', look for a name before returning
    if chunk_label and chunk_label ~= 'name' then
       -- see if we can find a name
       local _, thm_name = string.match(s, chunk_begin_pattern .. '.*' .. "name" .. param_pattern .. chunk_end_pattern)
       if thm_name then
         return chunk_type .. ':' .. chunk_label .. ' ' .. thm_name .. '\n'
       else
         return chunk_type .. ':' .. chunk_label .. '\n'
       end
    end
  end
  
  -- nothing found
  return ''
  
end

-- tables with specially formatted caption
function Table(caption, aligns, widths, headers, rows)
  local tab_pattern = "^%s*%(#tab:" .. xref_label_pattern .. "%)%s*(.*)$"
  local tab_label, tab_caption = string.match(caption, tab_pattern)
  if tab_label and tab_caption then
    return 'tab:' .. tab_label .. ' ' .. tab_caption .. '\n'
  else
    return ''
  end
end

-- equations w/ embedded (\#eq:label) 
function DisplayMath(s)
  local eq_pattern = "^.*%(\\#eq:" .. xref_label_pattern .. "%).*$"
  local eq_xref = string.match(s, eq_pattern)
  if (eq_xref) then
    xref_pending = true
    return 'eq:' .. eq_xref .. '\n'
  else
    return ''
  end
end

-- echo body
function Doc(body, metadata, variables)
  return body
end

-- echo text
function Str(s)
  return s
end

-- echo space
function Space()
  return ' '
end


-- no-op for other things within the ast

function Blocksep()
  return ''
end

function SoftBreak()
  return ''
end

function LineBreak()
  return ''
end

function Emph(s)
  return ''
end

function Strong(s)
  return ''
end

function Subscript(s)
  return ''
end

function Superscript(s)
  return ''
end

function SmallCaps(s)
  return ''
end

function Strikeout(s)
  return ''
end

function Link(s, src, tit, attr)
  return ''
end

function Image(s, src, tit, attr)
  return ''
end

function CaptionedImage(src, tit, caption, attr)
  return ''   
end

function InlineMath(s)
  return ''  
end

function SingleQuoted(s)
  return ''  
end

function DoubleQuoted(s)
  return '' 
end

function Note(s)
  return ''  
end

function Span(s, attr)
  return '' 
end

function RawInline(format, str)
  return '' 
end

function Cite(s, cs)
  return ''
end

function Plain(s)
  return '' 
end

function Code(s, attr)
  return '' 
end

function BlockQuote(s)
  return ''
end

function HorizontalRule()
  return '' 
end

function LineBlock(ls)
  return ''  
end

function BulletList(items)
  return ''  
end

function OrderedList(items)
  return '' 
end

function DefinitionList(items)
  return '' 
end

function RawBlock(format, str)
  return ''
end

function Div(s, attr)
  return ''
end

