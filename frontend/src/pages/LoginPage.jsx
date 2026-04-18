import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import '../styles/login.css';

export default function LoginPage() {
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [username, setUsername] = useState('');
  const [msg, setMsg] = useState({ text: '', type: '' });
  const [sending, setSending] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [currentEmail, setCurrentEmail] = useState('');
  const [theme, setTheme] = useState(localStorage.getItem('vaultTheme') || 'light');
  const navigate = useNavigate();
  const otpRef = useRef(null);
  const usernameRef = useRef(null);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  // Scope body styles to this page only
  useEffect(() => {
    document.body.classList.add('page-login');
    return () => document.body.classList.remove('page-login');
  }, []);

  function toggleTheme() {
    const next = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    localStorage.setItem('vaultTheme', next);
  }

  function showMsg(text, type = 'error') {
    setMsg({ text: '> ' + text, type });
  }

  function clearMsg() {
    setMsg({ text: '', type: '' });
  }

  function goToStep(n) {
    setStep(n);
    clearMsg();
  }

  async function sendOTP() {
    const val = email.trim();
    if (!val) return showMsg('Email address required.');
    setSending(true);
    try {
      const r = await api.post('/api/auth/request-otp', { email: val });
      setCurrentEmail(val);
      goToStep(2);
      showMsg('Code sent to ' + val, 'success');
      setTimeout(() => otpRef.current?.focus(), 100);
    } catch (e) {
      showMsg(e.response?.data?.detail || 'Failed to send OTP');
    } finally {
      setSending(false);
    }
  }

  async function verifyOTP() {
    const val = otp.trim();
    if (val.length !== 6) return showMsg('Full 6-digit code required.');
    setVerifying(true);
    try {
      const r = await api.post('/api/auth/verify-otp', { email: currentEmail, otp: val });
      const d = r.data;
      if (d.needs_username) {
        goToStep(3);
        showMsg('Almost there! Choose a name.', 'success');
        setTimeout(() => usernameRef.current?.focus(), 100);
      } else {
        showMsg('Access granted. Loading vault…', 'success');
        setTimeout(() => navigate('/dashboard'), 900);
      }
    } catch (e) {
      showMsg(e.response?.data?.detail || 'Verification failed');
      setVerifying(false);
    }
  }

  async function handleSetUsername() {
    const val = username.trim();
    if (!val) return showMsg('Name required.');
    try {
      const r = await api.post('/api/auth/set-username', { email: currentEmail, username: val });
      showMsg('Welcome, ' + val + '! Entering vault…', 'success');
      setTimeout(() => navigate('/dashboard'), 900);
    } catch (e) {
      showMsg(e.response?.data?.detail || 'Failed to set username');
    }
  }

  function handleKeyDown(e) {
    if (e.key !== 'Enter') return;
    if (step === 1) sendOTP();
    else if (step === 2) verifyOTP();
    else if (step === 3) handleSetUsername();
  }

  return (
    <>
      <div className="blob blob-1"></div>
      <div className="blob blob-2"></div>
      <div className="blob blob-3"></div>
      <div className="blob blob-4"></div>

      <button className="theme-toggle" onClick={toggleTheme} id="themeBtn">
        {theme === 'dark' ? '🌙 DARK' : '☀️ LIGHT'}
      </button>

      <div className="card" onKeyDown={handleKeyDown}>
        <div className="card-header">
          <div className="card-header-text">
            <h1>Family Vault</h1>
            <p>Secure Document Access</p>
          </div>
        </div>

        <div className="card-body">
          <div className="step-indicator">
            <div className={`step-dot ${step === 1 ? 'active' : step > 1 ? 'done' : ''}`}></div>
            <div className={`step-dot ${step === 2 ? 'active' : step > 2 ? 'done' : ''}`}></div>
            <div className={`step-dot ${step === 3 ? 'active' : ''}`}></div>
          </div>

          {msg.type && <div className={`msg ${msg.type}`}>{msg.text}</div>}

          {/* Step 1 */}
          <div className={`step ${step === 1 ? 'active' : ''}`}>
            <div className="field">
              <label>Email Address</label>
              <input
                type="email"
                id="emailInput"
                placeholder="your@email.com"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <button className="btn" id="sendOtpBtn" onClick={sendOTP} disabled={sending}>
              {sending ? <><span className="spinner"></span>SENDING…</> : 'Send Code →'}
            </button>
          </div>

          {/* Step 2 */}
          <div className={`step ${step === 2 ? 'active' : ''}`}>
            <div className="field">
              <label>6-Digit Code</label>
              <input
                type="text"
                id="otpInput"
                className="otp-input"
                placeholder="000000"
                maxLength="6"
                inputMode="numeric"
                autoComplete="one-time-code"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                ref={otpRef}
              />
            </div>
            <button className="btn" id="verifyBtn" onClick={verifyOTP} disabled={verifying}>
              {verifying ? <><span className="spinner"></span>VERIFYING…</> : 'Verify & Enter →'}
            </button>
            <button className="btn-secondary" onClick={() => goToStep(1)}>← Different Email</button>
            <p className="otp-hint">// CODE EXPIRES IN 5 MINUTES. CHECK SPAM.</p>
          </div>

          {/* Step 3 */}
          <div className={`step ${step === 3 ? 'active' : ''}`}>
            <div className="field">
              <label>Your Display Name</label>
              <input
                type="text"
                id="usernameInput"
                placeholder="e.g. Priya, Dad, Grandma…"
                maxLength="40"
                autoComplete="nickname"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                ref={usernameRef}
              />
            </div>
            <button className="btn" onClick={handleSetUsername}>Join Vault →</button>
          </div>
        </div>
      </div>
    </>
  );
}
