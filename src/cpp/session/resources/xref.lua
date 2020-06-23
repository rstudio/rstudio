
-- lua pattern (including capture) for an xref label
local xref_label_pattern = "([a-zA-Z0-9/%-]+)"

-- output paragraph if it contains an xref (some of the functions below
-- (e.g. Code and DisplayMath) set this flag if the discover an xref
local xref_pending = false
function Para(s)
  if xref_pending then
    xref_pending = false
    return s
  else
    return ''
  end
end

-- headers just output id and header text
function Header(lev, s, attr)
  if string.len(attr.id) > 0 then
    return attr.id .. ' ' .. s .. '\n'
  else
    return ''
  end
end

-- rmd code chunks w/ labels get turned into inline code within a paragraph
-- look for a code chunk w/ fig.cap or a kable w/ a caption
function Code(s, attr)
  
  local chunk_begin = "^{[a-zA-Z0-9_]+[%s,]+" .. xref_label_pattern
  local chunk_end = ".*}.*$"
  local param = "%s*=%s*[\"']([^\"']+)[\"']"
  
  -- start by looking for just fig.cap. if we find that then also look for a quoted value
  local fig_cap_begin = "[ ,].*fig%.cap"
  local fig_label = string.match(s, chunk_begin .. fig_cap_begin .. chunk_end)
  if fig_label then
    xref_pending = true
    local _, fig_caption = string.match(s, chunk_begin .. fig_cap_begin .. param .. chunk_end)
    if fig_caption then
      return 'fig:' .. fig_label .. ' ' .. fig_caption .. '\n'
    else
      return 'fig:' .. fig_label .. '\n'
    end
  end
  
  -- start by looking for just caption, if we find that then also look for a quoted value
  local tab_caption_begin = ".*}.*kable%s*%(.*caption"
  local tab_label = string.match(s, chunk_begin .. tab_caption_begin .. ".*$")
  if (tab_label) then
    xref_pending = true
    local _, tab_caption = string.match(s, chunk_begin .. tab_caption_begin .. param .. ".*$")
    if tab_caption then
       return 'tab:' .. tab_label .. ' ' .. tab_caption .. '\n'
    else
       return 'tab:' .. tab_label .. ' ' .. '\n'
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

function CodeBlock(s, attr)
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

