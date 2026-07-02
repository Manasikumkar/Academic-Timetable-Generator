import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Topbar from '../common/Topbar';
import { useToast } from '../common/ToastContext';
import { constraintApi } from '../../services/api';
import '../../styles/global.css';

const emptyForm = { type: 'HARD', name: '', description: '', penalty: 10, active: true };

export default function Constraints() {
  const toast = useToast();
  const [constraints, setConstraints] = useState([]);
  const [loading, setLoading]         = useState(true);
  const [saving, setSaving]           = useState(false);
  const [form, setForm]               = useState(emptyForm);
  const [filter, setFilter]           = useState('ALL');

  const load = async () => {
    setLoading(true);
    try { const r = await constraintApi.getAll(); setConstraints(r.data); }
    catch { toast.error('Failed to load constraints'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name.trim() || !form.description.trim()) { toast.error('Name and description required'); return; }
    setSaving(true);
    try { await constraintApi.create(form); toast.success('Constraint added!'); setForm(emptyForm); load(); }
    catch (err) { toast.error(err.response?.data?.message || 'Save failed'); }
    finally { setSaving(false); }
  };

  const handleToggle = async (id) => {
    try { await constraintApi.toggle(id); toast.success('Toggled'); load(); }
    catch { toast.error('Toggle failed'); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this constraint?')) return;
    try { await constraintApi.delete(id); toast.success('Deleted'); load(); }
    catch { toast.error('Delete failed'); }
  };

  const filtered = filter === 'ALL' ? constraints : constraints.filter(c => c.type === filter);
  const hardActive = constraints.filter(c => c.type === 'HARD' && c.active).length;
  const softActive = constraints.filter(c => c.type === 'SOFT' && c.active).length;

  return (
    <div className="theme-dark">
      <Topbar />
      <div className="constraints-page container">
        <div className="page-header">
          <h2>Scheduling Constraints</h2>
          <p>Hard constraints are never violated. Soft constraints are minimized by the GA. All defaults are pre-seeded.</p>
        </div>

        <div className="stats-card">
          <div className="stat-item"><span className="stat-label">Hard Active</span><span className="stat-value" style={{ color: '#ef4444' }}>{hardActive}</span></div>
          <div className="stat-item"><span className="stat-label">Soft Active</span><span className="stat-value" style={{ color: '#F59E0B' }}>{softActive}</span></div>
          <div className="stat-item"><span className="stat-label">Total</span><span className="stat-value">{constraints.length}</span></div>
        </div>

        <div className="info-card">
          <span className="info-icon">💡</span>
          <span style={{ fontSize: 13, color: '#cbd5e1' }}>
            Default constraints based on your IT dept timetable are already seeded. You can add custom ones or toggle existing ones on/off.
          </span>
        </div>

        {/* Add form */}
        <div className="card">
          <div className="card-title">+ Add Custom Constraint</div>
          <form onSubmit={handleSubmit} className="form-grid">
            <div className="form-row">
              <div className="form-field">
                <label>Type</label>
                <select value={form.type} onChange={e => set('type', e.target.value)}>
                  <option value="HARD">HARD (never violate)</option>
                  <option value="SOFT">SOFT (minimize violations)</option>
                </select>
              </div>
              <div className="form-field">
                <label>Penalty Weight {form.type === 'HARD' && <span style={{ color: '#ef4444', fontSize: 11 }}>(HARD = always 100)</span>}</label>
                <input type="number" min={1} max={100} value={form.penalty}
                  onChange={e => set('penalty', +e.target.value)}
                  disabled={form.type === 'HARD'} />
              </div>
            </div>
            <div className="form-field">
              <label>Constraint Name <span className="required">*</span></label>
              <input placeholder="e.g. Avoid Saturday for BE" value={form.name}
                onChange={e => set('name', e.target.value)} />
            </div>
            <div className="form-field">
              <label>Description <span className="required">*</span></label>
              <input placeholder="Describe the rule in plain English" value={form.description}
                onChange={e => set('description', e.target.value)} />
            </div>
            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Adding…' : 'Add Constraint'}
              </button>
            </div>
          </form>
        </div>

        {/* List */}
        <div className="card">
          <div className="card-header">
            <div className="card-title" style={{ margin: 0 }}>Configured Constraints</div>
            <div className="filter-buttons">
              {['ALL','HARD','SOFT'].map(t => (
                <button key={t} className={`filter-btn ${filter===t?'active':''}`} onClick={() => setFilter(t)}>
                  {t === 'HARD' ? '🔴 HARD' : t === 'SOFT' ? '🟡 SOFT' : 'All'}
                </button>
              ))}
            </div>
          </div>

          {loading ? (
            <div className="loading-container"><div className="spinner" /><div className="loading-text">Loading…</div></div>
          ) : filtered.length === 0 ? (
            <div className="empty-state"><div className="empty-icon">⚙️</div><p>No constraints configured.</p></div>
          ) : filtered.map(c => (
            <div key={c.id} className="list-item" style={{ opacity: c.active ? 1 : 0.5 }}>
              <div className="item-info">
                <div className="item-title">
                  <span className={`badge ${c.type==='HARD'?'badge-danger':'badge-warning'}`} style={{ marginRight: 8 }}>{c.type}</span>
                  {c.name}
                  {!c.active && <span className="badge" style={{ marginLeft: 8, background: '#1e254d', color: '#9ca3af' }}>DISABLED</span>}
                </div>
                <div className="item-meta">
                  <span>{c.description}</span>
                  {c.type === 'SOFT' && <span style={{ color: '#F59E0B' }}>Penalty: {c.penalty}</span>}
                </div>
              </div>
              <div className="item-actions">
                <button className={`btn btn-sm ${c.active ? 'btn-secondary' : 'btn-success'}`}
                  onClick={() => handleToggle(c.id)}>
                  {c.active ? 'Disable' : 'Enable'}
                </button>
                <button className="btn btn-danger btn-sm" onClick={() => handleDelete(c.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>

        <div className="nav-actions">
          <Link to="/courses"><button className="btn btn-secondary">← Courses</button></Link>
          <Link to="/generate"><button className="btn btn-primary">Next: Generate →</button></Link>
        </div>
      </div>
      <div className="footer">TimeTableGen · IT Department</div>
    </div>
  );
}