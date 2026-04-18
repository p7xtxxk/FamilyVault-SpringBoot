import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api';
import '../styles/upload.css';

export default function UploadPage() {
  const [title, setTitle] = useState('');
  const [docType, setDocType] = useState('');
  const [file, setFile] = useState(null);
  const [fileName, setFileName] = useState('—');
  const [fileSize, setFileSize] = useState('—');
  const [showPreview, setShowPreview] = useState(false);
  const [msg, setMsg] = useState({ text: '', type: '' });
  const [uploading, setUploading] = useState(false);
  const [progressWidth, setProgressWidth] = useState('0%');
  const [showProgress, setShowProgress] = useState(false);
  const [limitText, setLimitText] = useState('LOADING…');
  const [limitFill, setLimitFill] = useState('0%');
  const [limitReached, setLimitReached] = useState(false);
  const [adminEmail, setAdminEmail] = useState('—');
  const [showAdmin, setShowAdmin] = useState(false);
  const [dragover, setDragover] = useState(false);
  const [theme, setTheme] = useState(localStorage.getItem('vaultTheme') || 'light');
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const inactivityRef = useRef(null);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  // Scope body styles to this page only
  useEffect(() => {
    document.body.classList.add('page-upload');
    return () => document.body.classList.remove('page-upload');
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

  function showMsg(text, type = 'error') {
    setMsg({ text: '> ' + text, type });
  }

  function fileSelected(input) {
    const f = input.files[0];
    if (!f) return;
    if (!f.name.toLowerCase().endsWith('.pdf')) {
      showMsg('Only PDF files are accepted.');
      input.value = '';
      return;
    }
    if (f.size > 20 * 1024 * 1024) {
      showMsg('File too large. Maximum 20 MB.');
      input.value = '';
      return;
    }
    setFile(f);
    setFileName(f.name);
    setFileSize((f.size / 1024 / 1024).toFixed(2) + ' MB');
    setShowPreview(true);
  }

  async function loadDocCount() {
    try {
      const r = await api.get('/api/documents');
      const d = r.data;
      const pct = Math.min((d.count / d.limit) * 100, 100);
      let text = `${d.count} / ${d.limit} DOCS USED`;
      if (d.count >= d.limit) {
        text += ' — LIMIT REACHED';
        setLimitReached(true);
      }
      setLimitText(text);
      setLimitFill(pct + '%');
    } catch {}
  }

  async function checkAdmin() {
    try {
      const r = await api.get('/api/auth/me');
      if (!r.data.is_admin) {
        document.body.innerHTML = `<div style="min-height:100vh;display:flex;align-items:center;justify-content:center;background:#111;font-family:'Space Mono',monospace">
          <div style="border:3px solid #f5f0e8;background:#FF2D2D;color:white;padding:52px 48px;text-align:center;box-shadow:10px 10px 0 #f5f0e8;max-width:440px;width:90%">
            <div style="font-family:'Bebas Neue',sans-serif;font-size:56px;letter-spacing:0.1em">ACCESS DENIED</div>
            <p style="font-size:12px;margin:14px 0 24px;letter-spacing:0.08em;opacity:0.85">// ADMIN ONLY PAGE</p>
            <a href="/dashboard" style="background:#f5f0e8;color:#0a0a0a;padding:13px 28px;text-decoration:none;font-size:13px;font-weight:700;letter-spacing:0.08em;display:inline-block;border:2px solid #f5f0e8">← BACK TO VAULT</a>
          </div>
        </div>`;
        return;
      }
      setAdminEmail(r.data.email);
      setShowAdmin(true);
    } catch {
      navigate('/');
    }
  }

  useEffect(() => {
    checkAdmin();
    loadDocCount();
  }, []);

  async function uploadDocument() {
    if (!title.trim()) return showMsg('Document title required.');
    if (!docType) return showMsg('Document type required.');
    if (!file) return showMsg('PDF file required.');

    setUploading(true);
    setShowProgress(true);
    setProgressWidth('30%');

    const formData = new FormData();
    formData.append('title', title.trim());
    formData.append('document_type', docType);
    formData.append('file', file);

    try {
      setProgressWidth('65%');
      const r = await api.post('/api/upload-document', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setProgressWidth('100%');
      showMsg('Upload complete: ' + r.data.message, 'success');
      setTitle('');
      setDocType('');
      setFile(null);
      setShowPreview(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
      loadDocCount();
    } catch (e) {
      showMsg(e.response?.data?.detail || 'Upload failed');
      setProgressWidth('0');
    } finally {
      setUploading(false);
      setTimeout(() => setShowProgress(false), 2200);
    }
  }

  return (
    <>
      <nav>
        <div className="nav-brand">Family Vault</div>
        <div className="nav-right">
          <Link to="/dashboard" className="btn-nav">← Dashboard</Link>
          <button className="btn-nav" onClick={toggleTheme} id="themeBtn">
            {theme === 'dark' ? '🌙' : '☀️'}
          </button>
          <button className="btn-nav" onClick={doLogout}>Sign Out</button>
        </div>
      </nav>

      <main>
        <div className="page-header">
          <h2>UPLOAD <span>DOC</span></h2>
          <p>// FILES ARE AES-256 ENCRYPTED BEFORE STORAGE</p>
        </div>

        {showAdmin && (
          <div className="admin-notice" id="adminNotice">
            UPLOADING AS: <strong id="adminEmail">{adminEmail}</strong>
          </div>
        )}

        <div className="doc-limit" id="docLimitBar">
          <span id="limitText">{limitText}</span>
          <div className="limit-bar-track">
            <div className="limit-bar-fill" id="limitFill" style={{ width: limitFill }}></div>
          </div>
        </div>

        {msg.type && <div className={`msg ${msg.type}`}>{msg.text}</div>}

        <div className="card">
          <div className="card-header-strip">DOCUMENT DETAILS</div>
          <div className="card-body">
            <div className="field">
              <label>Document Title</label>
              <input
                type="text"
                id="titleInput"
                placeholder="e.g. Home Insurance Policy 2024"
                maxLength="100"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </div>

            <div className="field">
              <label>Document Type</label>
              <div className="select-wrapper">
                <select id="typeInput" value={docType} onChange={(e) => setDocType(e.target.value)}>
                  <option value="">— Select type —</option>
                  <option>Property</option>
                  <option>Insurance</option>
                  <option>Passport</option>
                  <option>Identity</option>
                  <option>Medical</option>
                  <option>Financial</option>
                  <option>Legal</option>
                  <option>Vehicle</option>
                  <option>Other</option>
                </select>
              </div>
            </div>

            <div className="field">
              <label>PDF File</label>
              <div
                className={`drop-zone ${dragover ? 'dragover' : ''}`}
                id="dropZone"
                onDragOver={(e) => { e.preventDefault(); setDragover(true); }}
                onDragLeave={() => setDragover(false)}
                onDrop={(e) => { e.preventDefault(); setDragover(false); }}
              >
                <input
                  type="file"
                  id="fileInput"
                  accept=".pdf"
                  ref={fileInputRef}
                  onChange={(e) => fileSelected(e.target)}
                />
                <span className="drop-icon">📄</span>
                <div className="drop-text">CLICK TO CHOOSE or DRAG & DROP</div>
                <div className="drop-hint">// PDF ONLY · MAX 20 MB</div>
              </div>
              <div className={`file-preview ${showPreview ? 'show' : ''}`} id="filePreview">
                <span>📄</span>
                <span id="fileName">{fileName}</span>
                <span className="file-size" id="fileSize">{fileSize}</span>
              </div>
            </div>

            <button
              className="btn-upload"
              id="uploadBtn"
              onClick={uploadDocument}
              disabled={uploading || limitReached}
            >
              {uploading ? <><span className="spinner"></span>ENCRYPTING & UPLOADING…</> : 'ENCRYPT & UPLOAD'}
            </button>
            <div className={`progress-wrap ${showProgress ? 'show' : ''}`} id="progressWrap">
              <div className="progress-fill" id="progressFill" style={{ width: progressWidth }}></div>
            </div>
          </div>
        </div>
      </main>
    </>
  );
}
