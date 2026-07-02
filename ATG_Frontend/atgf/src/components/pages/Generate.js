import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Topbar from '../common/Topbar';
import { useToast } from '../common/ToastContext';
import { timetableApi } from '../../services/api';
import '../../styles/global.css';

const SEMESTER_INFO = {
  1: { label: 'Odd Semester (I / III / V)',   years: ['SE','TE','BE'], color: '#F59E0B', desc: 'July – November' },
  2: { label: 'Even Semester (II / IV / VI)', years: ['SE','TE','BE'], color: '#6C63FF', desc: 'January – May'   },
};

export default function Generate() {
  const toast    = useToast();
  const navigate = useNavigate();

  const [versions, setVersions] = useState([]);
  const [loading, setLoading]   = useState(true);
  const [running, setRunning]   = useState(false);
  const [progress, setProgress] = useState(0);
  const [form, setForm]         = useState({
    name:           'Sem-II 2025-26',
    academicYear:   '2025-26',
    semester:       2,
    years:          ['SE','TE','BE'],
    populationSize: 200,
    maxGenerations: 1000,
    mutationRate:   0.08,
  });

  const load = async () => {
    try { const r = await timetableApi.getAllVersions(); setVersions(r.data); }
    catch { toast.error('Failed to load versions'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  // Auto-update name when semester/year changes
  const setSemester = (s) => {
    setForm(f => ({
      ...f,
      semester: s,
      name: `Sem-${s === 1 ? 'I' : 'II'} ${f.academicYear}`,
    }));
  };

  const toggleYear = (yr) => {
    setForm(f => ({
      ...f,
      years: f.years.includes(yr)
        ? f.years.filter(y => y !== yr)
        : [...f.years, yr],
    }));
  };

  const handleGenerate = async (e) => {
    e.preventDefault();
    if (form.years.length === 0) { toast.error('Select at least one year class'); return; }
    setRunning(true); setProgress(0);

    const interval = setInterval(() => {
      setProgress(p => Math.min(p + Math.random() * 3, 88));
    }, 800);

    try {
      const payload = { ...form, years: form.years.join(',') };
      const res = await timetableApi.generate(payload);
      clearInterval(interval); setProgress(100);
      toast.success(res.data.message || 'Generated!');
      setTimeout(() => { load(); setRunning(false); setProgress(0); }, 700);
    } catch (err) {
      clearInterval(interval); setProgress(0); setRunning(false);
      toast.error(err.response?.data?.message || 'Generation failed. Check courses are configured.');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this draft?')) return;
    try { await timetableApi.deleteVersion(id); toast.success('Deleted'); load(); }
    catch { toast.error('Delete failed'); }
  };

  const statusColor = { DRAFT:'#3B82F6', FINAL:'#F59E0B', DEPLOYED:'#10B981' };
  const semInfo = SEMESTER_INFO[form.semester];

  return (
    <div className="theme-dark">
      <Topbar />
      <div className="container">
        <div className="page-header">
          <h2>Generate Timetable</h2>
          <p>Select semester, choose which years to schedule, configure GA parameters and run.</p>
        </div>

        {/* ── Semester Selection ── */}
        <div className="card">
          <div className="card-title">Step 1 — Select Semester</div>
          <div style={{ display:'flex', gap:12, flexWrap:'wrap' }}>
            {[1,2].map(s => {
              const info = SEMESTER_INFO[s];
              const active = form.semester === s;
              return (
                <div key={s} onClick={() => setSemester(s)}
                  style={{
                    flex:'1', minWidth:220, cursor:'pointer', padding:16,
                    borderRadius:10, border: active ? `2px solid ${info.color}` : '1px solid #2a2f5a',
                    background: active ? `${info.color}18` : '#0f1433',
                    transition:'all 0.2s'
                  }}>
                  <div style={{ fontWeight:600, fontSize:14, color: active ? info.color : '#cbd5e1', marginBottom:4 }}>
                    {active ? '● ' : '○ '}{info.label}
                  </div>
                  <div style={{ fontSize:12, color:'#9ca3af' }}>{info.desc}</div>
                  <div style={{ marginTop:8, display:'flex', gap:6 }}>
                    {info.years.map(y => (
                      <span key={y} style={{
                        fontSize:11, padding:'2px 8px', borderRadius:12,
                        background: active ? `${info.color}30` : '#1e254d',
                        color: active ? info.color : '#9ca3af'
                      }}>{y}</span>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* ── Year Selection ── */}
        <div className="card">
          <div className="card-title">Step 2 — Select Year Classes to Schedule</div>
          <p style={{ fontSize:12, color:'#9ca3af', marginBottom:12 }}>
            Uncheck a year if you want to generate timetable for specific years only.
          </p>
          <div style={{ display:'flex', gap:12 }}>
            {['SE','TE','BE'].map(yr => {
              const labels = { SE:'2IT — Second Year', TE:'3IT — Third Year', BE:'4IT — Final Year' };
              const active = form.years.includes(yr);
              return (
                <div key={yr} onClick={() => toggleYear(yr)}
                  style={{
                    flex:1, cursor:'pointer', padding:14, borderRadius:10, textAlign:'center',
                    border: active ? '2px solid #6C63FF' : '1px solid #2a2f5a',
                    background: active ? 'rgba(108,99,255,0.12)' : '#0f1433',
                    transition:'all 0.2s'
                  }}>
                  <div style={{ fontSize:20, marginBottom:4 }}>
                    {active ? '✓' : '○'}
                  </div>
                  <div style={{ fontWeight:600, color: active ? '#6C63FF' : '#9ca3af' }}>{yr}</div>
                  <div style={{ fontSize:11, color:'#9ca3af' }}>{labels[yr]}</div>
                </div>
              );
            })}
          </div>
        </div>

        {/* ── GA Configuration ── */}
        <div className="card">
          <div className="card-title">Step 3 — GA Configuration</div>
          <form onSubmit={handleGenerate} className="form-grid">
            <div className="form-row">
              <div className="form-field">
                <label>Timetable Name</label>
                <input value={form.name} onChange={e => setForm(f=>({...f,name:e.target.value}))} />
              </div>
              <div className="form-field">
                <label>Academic Year</label>
                <input value={form.academicYear} onChange={e => setForm(f=>({...f,academicYear:e.target.value}))} />
              </div>
            </div>

            <div className="form-row">
              <div className="form-field">
                <label>Population Size
                  <span className="field-hint" style={{display:'inline',marginLeft:6}}>(150–300 recommended)</span>
                </label>
                <input type="number" min={50} max={500} value={form.populationSize}
                  onChange={e => setForm(f=>({...f,populationSize:+e.target.value}))} />
              </div>
              <div className="form-field">
                <label>Max Generations
                  <span className="field-hint" style={{display:'inline',marginLeft:6}}>(500–2000)</span>
                </label>
                <input type="number" min={100} max={5000} value={form.maxGenerations}
                  onChange={e => setForm(f=>({...f,maxGenerations:+e.target.value}))} />
              </div>
            </div>

            <div className="form-field" style={{maxWidth:300}}>
              <label>Mutation Rate
                <span className="field-hint" style={{display:'inline',marginLeft:6}}>(0.05–0.15)</span>
              </label>
              <input type="number" step={0.01} min={0.01} max={0.5} value={form.mutationRate}
                onChange={e => setForm(f=>({...f,mutationRate:+e.target.value}))} />
            </div>

            {/* Summary */}
            <div style={{ background:'#0f1433', borderRadius:8, padding:'12px 16px',
                          border:'1px solid #2a2f5a', fontSize:12, color:'#9ca3af', lineHeight:1.8 }}>
              <span style={{ color:'#6C63FF', fontWeight:600 }}>Will generate: </span>
              {SEMESTER_INFO[form.semester].label} · Years: {form.years.join(', ')} ·
              Academic Year: {form.academicYear}
              <br/>
              <span style={{ color:'#10B981' }}>Lab structure: 2 consecutive slots (e.g. 14:00–16:00), all 3 batches simultaneously in different labs</span>
            </div>

            {/* Progress */}
            {running && (
              <div>
                <div style={{ display:'flex', justifyContent:'space-between', fontSize:12, color:'#9ca3af', marginBottom:6 }}>
                  <span>🧬 Running Genetic Algorithm — placing lab blocks + filling theory slots…</span>
                  <span>{Math.round(progress)}%</span>
                </div>
                <div className="ga-progress-bar">
                  <div className="ga-progress-fill" style={{width:`${progress}%`}} />
                </div>
                <div style={{fontSize:11,color:'#9ca3af',marginTop:4}}>
                  2-hr lab blocks · No empty slots · All 3 batches parallel · ITSA slots reserved
                </div>
              </div>
            )}

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={running || form.years.length===0}
                style={{fontSize:15,padding:'12px 28px'}}>
                {running ? '⏳ Generating…' : '🚀 Generate Timetable'}
              </button>
            </div>
          </form>
        </div>

        {/* ── Version History ── */}
        <div className="card">
          <div className="card-title">Generated Versions</div>
          {loading ? (
            <div className="loading-container"><div className="spinner"/><div className="loading-text">Loading…</div></div>
          ) : versions.length === 0 ? (
            <div className="empty-state"><div className="empty-icon">📄</div><p>No versions yet.</p></div>
          ) : versions.map(v => (
            <div key={v.id} className="list-item">
              <div className="item-info">
                <div className="item-title">
                  {v.name}
                  <span className="badge" style={{marginLeft:8,background:statusColor[v.status]+'22',color:statusColor[v.status]}}>
                    {v.status}
                  </span>
                  <span style={{marginLeft:8,fontSize:11,color:'#9ca3af'}}>
                    Sem-{v.semester} · {v.includedYears}
                  </span>
                  {v.hardConflicts===0
                    ? <span className="badge badge-success" style={{marginLeft:6}}>✓ Conflict-Free</span>
                    : <span className="badge badge-danger"  style={{marginLeft:6}}>{v.hardConflicts} conflicts</span>}
                </div>
                
              </div>
              <div className="item-actions">
                <button className="btn btn-secondary btn-sm"
                  onClick={() => navigate(`/timetable?versionId=${v.id}`)}>View</button>
                {v.status==='DRAFT' && (
                  <button className="btn btn-danger btn-sm" onClick={()=>handleDelete(v.id)}>Delete</button>
                )}
              </div>
            </div>
          ))}
        </div>

        <div className="nav-actions">
          <Link to="/constraints"><button className="btn btn-secondary">← Constraints</button></Link>
          <Link to="/timetable"><button className="btn btn-primary">View Timetable →</button></Link>
        </div>
      </div>
      <div className="footer">TimeTableGen · IT Department</div>
    </div>
  );
}