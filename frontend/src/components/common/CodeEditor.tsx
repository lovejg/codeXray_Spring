// CodeMirror 기반 코드 에디터: 언어별 문법 강조 + 자동 들여쓰기 + Tab 들여쓰기 + 라인번호.
import CodeMirror from '@uiw/react-codemirror'
import { githubDark } from '@uiw/codemirror-theme-github'
import { keymap, type KeyBinding } from '@codemirror/view'
import { indentWithTab } from '@codemirror/commands'
import { StreamLanguage, type LanguageSupport } from '@codemirror/language'
import { python } from '@codemirror/lang-python'
import { java } from '@codemirror/lang-java'
import { cpp } from '@codemirror/lang-cpp'
import { javascript } from '@codemirror/lang-javascript'
import { go } from '@codemirror/lang-go'
import { rust } from '@codemirror/lang-rust'
import { sql } from '@codemirror/lang-sql'
import { kotlin } from '@codemirror/legacy-modes/mode/clike'
import { swift } from '@codemirror/legacy-modes/mode/swift'
import type { Extension } from '@codemirror/state'

interface Props {
  value: string
  onChange: (v: string) => void
  placeholder?: string
  language?: string
  minHeight?: string
}

// LANGUAGES 값 → CodeMirror 언어 확장. 없는 언어는 강조 없이(빈 배열) 편집 기능만.
function languageExtension(language?: string): Extension[] {
  switch (language) {
    case 'python': return [python()]
    case 'java': return [java()]
    case 'cpp':
    case 'c': return [cpp()]
    case 'javascript': return [javascript()]
    case 'typescript': return [javascript({ typescript: true }) as LanguageSupport]
    case 'go': return [go()]
    case 'rust': return [rust()]
    case 'sql': return [sql()]
    case 'kotlin': return [StreamLanguage.define(kotlin)]
    case 'swift': return [StreamLanguage.define(swift)]
    default: return []
  }
}

export default function CodeEditor({ value, onChange, placeholder, language, minHeight = '340px' }: Props) {
  const tabKeymap = keymap.of([indentWithTab as KeyBinding])

  return (
    <div className="overflow-hidden rounded-xl border border-white/10 focus-within:border-teal-400/70 focus-within:ring-2 focus-within:ring-teal-500/20">
      <CodeMirror
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        theme={githubDark}
        extensions={[...languageExtension(language), tabKeymap]}
        minHeight={minHeight}
        basicSetup={{
          lineNumbers: true,
          highlightActiveLine: true,
          bracketMatching: true,
          closeBrackets: true,
          indentOnInput: true,
          autocompletion: false,
        }}
      />
    </div>
  )
}
