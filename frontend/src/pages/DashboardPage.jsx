import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api';
import '../styles/dashboard.css';

function escapeHtml(t) {
  return String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export default function DashboardPage() {
  const [isAdmin, setIsAdmin] = useState(false);
  const [username, setUsername] = useState('—');
  const [allDocuments, setAllDocuments] = useState([]);
  const [activeFilter, setActiveFilter] = useState('All');
  const [docCountText, setDocCountText] = useState('LOADING…');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [toast, setToast] = useState({ text: '', type: '', show: false });
  const [modal, setModal] = useState({ show: false, id: null, title: '', deleting: false });
  const [theme, setTheme] = useState(localStorage.getItem('vaultTheme') || 'light');
  const navigate = useNavigate();
  const inactivityRef = useRef(null);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  // Scope body styles to this page only
  useEffect(() => {
    document.body.classList.add('page-dashboard');
    return () => document.body.classList.remove('page-dashboard');
  }, []);

  function toggleTheme() {
    const next = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    localStorage.setItem('vaultTheme', next);
  }

  const doLogout = useCallback(async () => {
    try { await api.post('/api/auth/logout'); } catch {}
    navigate('/');
  }, [navigate]);

  // Inactivity timer (5 min)
  const resetInactivity = useCallback(() => {
    if (inactivityRef.current) clearTimeout(inactivityRef.current);
    inactivityRef.current = setTimeout(doLogout, 5 * 60 * 1000);
  }, [doLogout]);

  useEffect(() => {
    const events = ['mousemove', 'mousedown', 'keypress', 'scroll', 'touchstart'];
    events.forEach(e => window.addEventListener(e, resetInactivity));
    resetInactivity();
    return () => {
      events.forEach(e => window.removeEventListener(e, resetInactivity));
      if (inactivityRef.current) clearTimeout(inactivityRef.current);
    };
  }, [resetInactivity]);

  function showToast(text, type = 'success') {
    setToast({ text: '> ' + text, type, show: true });
    setTimeout(() => setToast(prev => ({ ...prev, show: false })), 3000);
  }

  async function loadUser() {
    try {
      const r = await api.get('/api/auth/me');
      setUsername(r.data.username || r.data.email);
      setIsAdmin(r.data.is_admin);
    } catch {
      navigate('/');
    }
  }

  async function loadDocuments() {
    try {
      const r = await api.get('/api/documents');
      setDocCountText(`${r.data.count} / ${r.data.limit} DOCS`);
      setAllDocuments(r.data.documents || []);
      setLoading(false);
    } catch {
      setLoadError(true);
      setLoading(false);
    }
  }

  useEffect(() => {
    loadUser().then(loadDocuments);
  }, []);

  function filterDocs(type) {
    setActiveFilter(type);
  }

  const filtered = activeFilter === 'All'
    ? allDocuments
    : allDocuments.filter(d => d.document_type === activeFilter);

  const filters = ['All', 'Property', 'Insurance', 'Passport', 'Identity', 'Medical', 'Financial', 'Legal', 'Vehicle'];

  function openDeleteModal(id, title) {
    setModal({ show: true, id, title: title || 'this document', deleting: false });
  }

  function closeDeleteModal() {
    setModal({ show: false, id: null, title: '', deleting: false });
  }

  async function confirmDelete() {
    if (!modal.id) return;
    setModal(prev => ({ ...prev, deleting: true }));
    try {
      const r = await api.delete(`/api/documents/${modal.id}`);
      closeDeleteModal();
      showToast('Document deleted.');
      loadDocuments();
    } catch (e) {
      showToast(e.response?.data?.detail || 'Delete failed', 'error');
      setModal(prev => ({ ...prev, deleting: false }));
    }
  }

  // Close modal on Escape
  useEffect(() => {
    function handler(e) { if (e.key === 'Escape') closeDeleteModal(); }
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, []);

  return (
    <>
      <nav>
        <div className="nav-brand">Family Vault</div>
        <div className="nav-right">
          <span className="nav-user">// <strong id="navUsername">{username}</strong></span>
          {isAdmin && <Link to="/upload" className="btn-nav upload" id="adminLink">+ Upload</Link>}
          <button className="btn-nav theme" onClick={toggleTheme} id="themeBtn">
            {theme === 'dark' ? '🌙' : '☀️'}
          </button>
          <button className="btn-nav" onClick={doLogout}>Sign Out</button>
        </div>
      </nav>

      <main>
        <div className="page-header">
          <div>
            <h2>FAMILY <span>DOCS</span></h2>
            <p>// ALL FILES ENCRYPTED · AES-256</p>
          </div>
          <span className="doc-count" id="docCount">{docCountText}</span>
        </div>

        <div className="filter-bar" id="filterBar">
          {filters.map(f => (
            <button
              key={f}
              className={`filter-chip ${activeFilter === f ? 'active' : ''}`}
              onClick={() => filterDocs(f)}
            >
              {f}
            </button>
          ))}
        </div>

        <div className="doc-grid" id="docGrid">
          {loading ? (
            <div className="loading-state">
              <div className="spinner-block"></div>
              <p>LOADING DOCUMENTS…</p>
            </div>
          ) : loadError ? (
            <div className="empty-state">
              <span className="empty-icon">⚠️</span>
              <h3>Load Failed</h3>
              <p>// Please refresh the page.</p>
            </div>
          ) : filtered.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">📂</span>
              <h3>No Documents Found</h3>
              <p>// {activeFilter === 'All' ? 'No documents have been uploaded yet.' : `No ${activeFilter} documents found.`}</p>
            </div>
          ) : (
            filtered.map((doc, i) => (
              <div
                className="doc-card"
                data-type={doc.document_type}
                key={doc.id}
                style={{ animationDelay: `${i * 0.06}s` }}
              >
                <div className="doc-card-top">
                  <span className="doc-type-badge">{doc.document_type || 'Document'}</span>
                  <span className="doc-icon"></span>
                </div>
                <div className="doc-card-body">
                  <div className="doc-title">{doc.title}</div>
                  <div className="doc-date">// ADDED {formatDate(doc.uploaded_at)}</div>
                  <div className="doc-actions">
                    <a className="btn-doc primary" href={`/api/get_document/${doc.id}`} target="_blank" rel="noopener noreferrer">VIEW</a>
                    <a className="btn-doc" href={`/api/get_document/${doc.id}`} download>SAVE</a>
                    {isAdmin && (
                      <button
                        className="btn-doc danger"
                        onClick={() => openDeleteModal(doc.id, doc.title)}
                      >
                        DELETE
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </main>

      <div className={`toast ${toast.type} ${toast.show ? 'show' : ''}`} id="toast">{toast.text}</div>

      {/* DELETE CONFIRMATION MODAL */}
      <div
        className={`modal-overlay ${modal.show ? 'show' : ''}`}
        id="deleteModal"
        role="dialog"
        aria-modal="true"
        onClick={(e) => { if (e.target === e.currentTarget) closeDeleteModal(); }}
      >
        <div className="modal-box">
          <div className="modal-header">
            <span className="modal-header-icon">🗑️</span>
            <span className="modal-header-title" id="modalTitle">Delete Document</span>
          </div>
          <div className="modal-body">
            <p>Are you sure you want to delete <strong id="modalDocTitle">{modal.title}</strong>?</p>
            <small>// THIS ACTION CANNOT BE UNDONE.</small>
          </div>
          <div className="modal-actions">
            <button className="modal-btn cancel" onClick={closeDeleteModal}>No</button>
            <button
              className="modal-btn confirm-delete"
              onClick={confirmDelete}
              disabled={modal.deleting}
            >
              {modal.deleting ? 'Deleting…' : 'Yes'}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
