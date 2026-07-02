import React from 'react';
import { Link } from 'react-router-dom';
import '../../styles/global.css';

const STEPS = [
  { num: '1', title: 'Add Faculty',     desc: 'Enter all IT department faculty members with their availability and teaching load.',  path: '/faculty' },
  { num: '2', title: 'Add Rooms',       desc: 'Configure classrooms, software labs, linux labs and project labs with capacity.',     path: '/rooms' },
  { num: '3', title: 'Add Courses',     desc: 'Define SE, TE and BE courses with credits, hours and assigned faculty.',              path: '/courses' },
  { num: '4', title: 'Set Constraints', desc: 'Configure hard and soft scheduling rules. Hard constraints are never violated.',      path: '/constraints' },
  { num: '5', title: 'Generate',        desc: 'Run the Genetic Algorithm to produce a conflict-free timetable automatically.',       path: '/generate' },
  { num: '6', title: 'Review & Deploy', desc: 'Review generated timetable, check conflicts and deploy the final schedule.',         path: '/timetable' },
];

export default function Home() {
  return (
    <div style={{ minHeight: '100vh', padding: '20px 60px', display: 'flex', flexDirection: 'column' }}>
      {/* Navbar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 40 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div className="logo">TT</div>
          <div>
            <div style={{ fontWeight: 600, fontSize: 18 }}>TimeTableGen</div>
            <div style={{ fontSize: 12, color: '#9ca3af' }}>IT Department · 2025-26</div>
          </div>
        </div>
        <Link to="/timetable" className="link" style={{ fontSize: 13 }}>View Active Timetable →</Link>
      </div>

      {/* Hero */}
      <div className="card" style={{ maxWidth: 860, margin: '0 auto 50px auto', padding: 50 }}>
        <h1 style={{ fontSize: 32, marginBottom: 16 }}>
          Automated IT Department{' '}
          <span style={{ background: 'linear-gradient(90deg,#6C63FF,#3B82F6)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            Timetable Generator
          </span>
        </h1>
        <p style={{ color: '#9ca3af', marginBottom: 28, lineHeight: 1.7, maxWidth: 600 }}>
          Generate conflict-free schedules for SE, TE and BE batches of the IT Department
          using a <strong style={{ color: '#6C63FF' }}>Genetic Algorithm</strong>. Handles faculty
          clashes, room allocation, lab batches (I1/I2/I3) and all hard &amp; soft constraints automatically.
        </p>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <Link to="/faculty">
            <button className="btn btn-primary" style={{ fontSize: 15, padding: '13px 28px' }}>
              Start Setup →
            </button>
          </Link>
          <Link to="/generate">
            <button className="btn btn-secondary" style={{ fontSize: 15, padding: '13px 28px' }}>
              Generate Timetable
            </button>
          </Link>
        </div>
      </div>

      {/* Steps grid */}
      <div style={{ display: 'flex', justifyContent: 'center', gap: 24, flexWrap: 'wrap', maxWidth: 960, margin: '0 auto' }}>
        {STEPS.map(s => (
          <Link to={s.path} key={s.num} style={{ textDecoration: 'none', flex: '1 1 260px', maxWidth: 300 }}>
            <div className="card" style={{ height: '100%', cursor: 'pointer' }}>
              <div style={{
                width: 34, height: 34, borderRadius: '50%', background: '#1e254d',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                marginBottom: 14, fontWeight: 700, color: '#6C63FF'
              }}>{s.num}</div>
              <h3 style={{ marginBottom: 10, fontSize: 15 }}>{s.title}</h3>
              <p style={{ color: '#9ca3af', fontSize: 13, lineHeight: 1.6 }}>{s.desc}</p>
            </div>
          </Link>
        ))}
      </div>

      {/* Footer */}
      <div className="footer" style={{ marginTop: 'auto', paddingTop: 40 }}>
        IT Department Timetable Generator · Sem-II 2025-26 · Genetic Algorithm Powered
      </div>
    </div>
  );
}