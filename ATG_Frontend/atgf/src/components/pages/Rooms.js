import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Topbar from '../common/Topbar';
import { useToast } from '../common/ToastContext';
import { roomApi } from '../../services/api';
import '../../styles/global.css';

const ROOM_TYPES = [
  { value: 'CLASSROOM',    label: 'Classroom',    icon: '🏫' },
  { value: 'SOFTWARE_LAB', label: 'Software Lab',  icon: '💻' },
  { value: 'LINUX_LAB',    label: 'Linux Lab',     icon: '🐧' },
  { value: 'PROJECT_LAB',  label: 'Project Lab',   icon: '🔬' },
];

const emptyForm = { name: '', type: 'CLASSROOM', capacity: 60 };

export default function Rooms() {
  const toast = useToast();
  const [rooms, setRooms]   = useState([]);
  const [loading, setLoading]= useState(true);
  const [saving, setSaving]  = useState(false);
  const [form, setForm]      = useState(emptyForm);
  const [editId, setEditId]  = useState(null);
  const [filter, setFilter]  = useState('ALL');

  const load = async () => {
    try { setLoading(true); const r = await roomApi.getAll(); setRooms(r.data); }
    catch { toast.error('Failed to load rooms'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) { toast.error('Room name is required'); return; }
    setSaving(true);
    try {
      if (editId) { await roomApi.update(editId, form); toast.success('Room updated!'); }
      else        { await roomApi.create(form);         toast.success('Room added!');   }
      setForm(emptyForm); setEditId(null); load();
    } catch (err) { toast.error(err.response?.data?.message || 'Save failed'); }
    finally { setSaving(false); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this room?')) return;
    try { await roomApi.delete(id); toast.success('Room deleted'); load(); }
    catch { toast.error('Delete failed'); }
  };

  const handleEdit = (r) => {
    setForm({ name: r.name, type: r.type, capacity: r.capacity });
    setEditId(r.id);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const filtered = filter === 'ALL' ? rooms : rooms.filter(r => r.type === filter);
  const countByType = (t) => rooms.filter(r => r.type === t).length;
  const typeInfo = (t) => ROOM_TYPES.find(r => r.value === t) || { label: t, icon: '🏠' };

  return (
    <div className="theme-dark">
      <Topbar />
      <div className="container">
        <div className="page-header">
          <h2>Room Management</h2>
          <p>Configure classrooms and labs. Labs are assigned to batches I1/I2/I3 by the GA.</p>
        </div>

        <div className="stats-card">
          <div className="stat-item"><span className="stat-label">Total Rooms</span><span className="stat-value">{rooms.length}</span></div>
          <div className="stat-item"><span className="stat-label">Classrooms</span><span className="stat-value">{countByType('CLASSROOM')}</span></div>
          <div className="stat-item"><span className="stat-label">Labs</span><span className="stat-value">{rooms.length - countByType('CLASSROOM')}</span></div>
        </div>

        <div className="info-card">
          <span className="info-icon">ℹ</span>
          <span style={{ fontSize: 13, color: '#cbd5e1' }}>
            Your IT dept has: <strong>1333</strong> &amp; <strong>1332</strong> classrooms, plus <strong>Software Lab</strong>, <strong>Linux Lab</strong> and <strong>Project Lab</strong>. These are pre-seeded — add more if needed.
          </span>
        </div>

        {/* Form */}
        <div className="card">
          <div className="card-title">{editId ? '✎ Edit Room' : '+ Add Room'}</div>
          <form onSubmit={handleSubmit} className="form-grid">
            <div className="form-row">
              <div className="form-field">
                <label>Room Name <span className="required">*</span></label>
                <input placeholder="e.g. 1333 or Software Lab"
                  value={form.name} onChange={e => setForm(f => ({...f, name: e.target.value}))} />
              </div>
              <div className="form-field">
                <label>Capacity</label>
                <input type="number" min={1} max={200} value={form.capacity}
                  onChange={e => setForm(f => ({...f, capacity: +e.target.value}))} />
              </div>
            </div>
            <div className="form-field">
              <label>Room Type</label>
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginTop: 4 }}>
                {ROOM_TYPES.map(rt => (
                  <button key={rt.value} type="button"
                    onClick={() => setForm(f => ({...f, type: rt.value}))}
                    className={`filter-btn ${form.type === rt.value ? 'active' : ''}`}
                    style={{ fontSize: 13 }}>
                    {rt.icon} {rt.label}
                  </button>
                ))}
              </div>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving…' : editId ? 'Update Room' : 'Add Room'}
              </button>
              {editId && (
                <button type="button" className="btn btn-secondary"
                  onClick={() => { setForm(emptyForm); setEditId(null); }}>Cancel</button>
              )}
            </div>
          </form>
        </div>

        {/* List */}
        <div className="card">
          <div className="card-header">
            <div className="card-title" style={{ margin: 0 }}>Room List</div>
            <div className="filter-buttons">
              {['ALL', ...ROOM_TYPES.map(r => r.value)].map(t => (
                <button key={t} className={`filter-btn ${filter === t ? 'active' : ''}`}
                  onClick={() => setFilter(t)}>
                  {t === 'ALL' ? 'All' : typeInfo(t).icon + ' ' + typeInfo(t).label}
                </button>
              ))}
            </div>
          </div>

          {loading ? (
            <div className="loading-container"><div className="spinner" /><div className="loading-text">Loading rooms…</div></div>
          ) : filtered.length === 0 ? (
            <div className="empty-state"><div className="empty-icon">🏠</div><p>No rooms found.</p></div>
          ) : filtered.map(r => {
            const ti = typeInfo(r.type);
            return (
              <div key={r.id} className="list-item">
                <div className="item-info">
                  <div className="item-title">{ti.icon} {r.name}</div>
                  <div className="item-meta">
                    <span className={`badge ${r.type === 'CLASSROOM' ? 'badge-info' : 'badge-success'}`}>{ti.label}</span>
                    <span>Capacity: {r.capacity}</span>
                  </div>
                </div>
                <div className="item-actions">
                  <button className="btn btn-secondary btn-sm" onClick={() => handleEdit(r)}>Edit</button>
                  <button className="btn btn-danger btn-sm" onClick={() => handleDelete(r.id)}>Delete</button>
                </div>
              </div>
            );
          })}
        </div>

        <div className="nav-actions">
          <Link to="/faculty"><button className="btn btn-secondary">← Faculty</button></Link>
          <Link to="/courses"><button className="btn btn-primary">Next: Courses →</button></Link>
        </div>
      </div>
      <div className="footer">TimeTableGen · IT Department</div>
    </div>
  );
}