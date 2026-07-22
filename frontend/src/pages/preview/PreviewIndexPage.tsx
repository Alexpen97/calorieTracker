import { Link } from 'react-router-dom'

export default function PreviewIndexPage() {
  return (
    <main className="mobile-page">
      <h1>UI Preview</h1>
      <p className="product-meta">Dev-only routes for reviewing UI without backend auth.</p>
      <ul className="preview-links">
        <li>
          <Link to="/preview/dashboard">Dashboard (mock data)</Link>
        </li>
        <li>
          <Link to="/preview/diary">Diary (mock data)</Link>
        </li>
        <li>
          <Link to="/preview/analytics">Analytics (mock data)</Link>
        </li>
      </ul>
    </main>
  )
}

