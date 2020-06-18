
-- output paragraph if it contains an xref (some of the functions below
-- (e.g. Code and DisplayMath) set this flag if the discover an xref)
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
  return attr.id .. ' ' .. s .. '\n'
end

-- rmd code chunks w/ labels get turned into inline code within a paragraph
-- look for a code chunk w/ fig.cap or a kable within the 'inline' code
function Code(s, attr)
  
  local chunk_begin = "^{[a-zA-Z0-9_]+[%s,]+([a-zA-Z0-9/%-]+)"
  local param = "%s*=%s*[\"']([^\"']+)[\"']"
  local fig_pattern = chunk_begin .. "[ ,].*fig%.cap" .. param .. ".*}.*$"
  local tab_pattern = chunk_begin .. ".*}.*kable%s*%(.*caption" .. param .. ".*$"

  local fig_label, fig_caption = string.match(s, fig_pattern)
  local tab_label, tab_caption = string.match(s, tab_pattern)
  
  if fig_label and fig_caption then
    xref_pending = true
    return 'fig:' .. fig_label .. ' ' .. fig_caption .. '\n'
  elseif tab_label and tab_caption then
    xref_pending = true
    return 'tab:' .. tab_label .. ' ' .. tab_caption .. '\n'
  else
    return ''
  end
end

-- tables with specially formatted caption
function Table(caption, aligns, widths, headers, rows)
  local tab_pattern = "^%s*%(#(tab:[a-zA-Z0-9/%-]+)%)%s*(.*)$"
  local tab_label, tab_caption = string.match(caption, tab_pattern)
  if tab_label and tab_caption then
    return tab_label .. ' ' .. tab_caption .. '\n'
  else
    return ''
  end
end

-- equations w/ embedded (\#eq:label) 
function DisplayMath(s)
  local eq_pattern = "^.*%(\\#(eq:[a-zA-Z0-9/%-]+)%).*$"
  local eq_xref = string.match(s, eq_pattern)
  if (eq_xref) then
    xref_pending = true
    return eq_xref .. '\n'
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

