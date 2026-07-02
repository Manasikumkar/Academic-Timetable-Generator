import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

// Admin‑only steps
const ADMIN_STEPS = [
  { label: 'Home',        path: '/home' },
  { label: 'Faculty',     path: '/faculty' },
  { label: 'Rooms',       path: '/rooms' },
  { label: 'Courses',     path: '/courses' },
  { label: 'Constraints', path: '/constraints' },
  { label: 'Generate',    path: '/generate' },
  { label: 'Timetable',   path: '/timetable' },
];

// Common steps for all authenticated users
const COMMON_STEPS = [
  { label: 'Timetable',   path: '/timetable' },
];

export default function Topbar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const role = user?.role?.toUpperCase();

  // Choose steps based on role
  const steps = role === 'ADMIN' ? ADMIN_STEPS : COMMON_STEPS;
  const currentIdx = steps.findIndex(s => s.path === location.pathname);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <>
      <div className="topbar">
        <div className="brand">
          <div className="logo">TT</div>
          <div>
            <div className="title">TimeTableGen</div>
            <div className="subtitle">IT Department · 2025-26</div>
          </div>
        </div>
        <div className="user-section">
          <span className="user-name">👋 {user?.name || 'User'}</span>
          <button onClick={handleLogout} className="logout-btn">Logout</button>
        </div>
      </div>

      <div className="progressbar">
        <ul className="steps">
          {steps.map((s, i) => (
            <li key={s.path}
                className={`step ${i === currentIdx ? 'active' : ''} ${i < currentIdx ? 'done' : ''}`}>
              <Link to={s.path} className="link" style={{ color: 'inherit' }}>{s.label}</Link>
            </li>
          ))}
        </ul>
      </div>
    </>
  );
}