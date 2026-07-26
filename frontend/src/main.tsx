import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'highlight.js/styles/github-dark.css' // 코드 문법 강조 색상 테마(hljs 토큰 클래스에 색 입힘)
import './index.css'
import App from './App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
