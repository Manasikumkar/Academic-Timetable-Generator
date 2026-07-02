import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Topbar from '../common/Topbar';
import { useToast } from '../common/ToastContext';
import { facultyApi } from '../../services/api';
import '../../styles/global.css';

const DAYS = ['MON','TUE','WED','THU','FRI','SAT'];
const SLOTS = [
  '1 (09:00-10:00)', '2 (10:00-11:00)', '3 (11:10-12:10)',
  '4 (12:10-13:10)', '5 (14:00-15:00)', '6 (15:00-16:00)'
];

const emptyForm = {
  name: '', shortCode: '', maxHoursPerDay: 6, maxHoursPerWeek: 24, unavailableSlots: []
};

export default function Faculty() {
  const toast = useToast();
  const [faculty, setFaculty]     = useState([]);
  const [loading, setLoading]     = useState(true);
  const [saving, setSaving]       = useState(false);
  const [form, setForm]           = useState(emptyForm);
  const [editId, setEditId]       = useState(null);
  const [search, setSearch]       = useState('');

  const load = async () => {
    try {
      setLoading(true);
      const res = await facultyApi.getAll();
      setFaculty(res.data);
    } catch { toast.error('Failed to load faculty'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const toggleSlot = (day, slot) => {
    const key = `${day}-${slot}`;
    setForm(f => ({
      ...f,
      unavailableSlots: f.unavailableSlots.includes(key)
        ? f.unavailableSlots.filter(s => s !== key)
        : [...f.unavailableSlots, key]
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name.trim() || !form.shortCode.trim()) {
      toast.error('Name and short code are required'); return;
    }
    setSaving(true);
    try {
      if (editId) {
        await facultyApi.update(editId, form);
        toast.success('Faculty updated!');
      } else {
        await facultyApi.create(form);
        toast.success('Faculty added!');
      }
      setForm(emptyForm); setEditId(null);
      load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Save failed');
    } finally { setSaving(false); }
  };

  const handleEdit = (f) => {
    setForm({ name: f.name, shortCode: f.shortCode, maxHoursPerDay: f.maxHoursPerDay,
              maxHoursPerWeek: f.maxHoursPerWeek, unavailableSlots: f.unavailableSlots || [] });
    setEditId(f.id);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this faculty member?')) return;
    try {
      await facultyApi.delete(id);
      toast.success('Faculty deleted'); load();
    } catch { toast.error('Delete failed'); }
  };

  const filtered = faculty.filter(f =>
    f.name.toLowerCase().includes(search.toLowerCase()) ||
    f.shortCode.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="theme-dark">
      <Topbar />
      <div className="container">
        <div className="page-header">
          <h2>Faculty Management</h2>
          <p>Add and manage IT department faculty members. Set availability to avoid scheduling conflicts.</p>
        </div>

        {/* Stats */}
        <div className="stats-card">
          <div className="stat-item"><span className="stat-label">Total Faculty</span><span className="stat-value">{faculty.length}</span></div>
          <div className="stat-item"><span className="stat-label">With Restrictions</span><span className="stat-value">{faculty.filter(f => f.unavailableSlots?.length > 0).length}</span></div>
          <div className="stat-item"><span className="stat-label">Avg Max Hrs/Day</span><span className="stat-value">{faculty.length ? Math.round(faculty.reduce((s, f) => s + f.maxHoursPerDay, 0) / faculty.length) : 0}</span></div>
        </div>

        {/* Form */}
        <div className="card">
          <div className="card-title">{editId ? '✎ Edit Faculty' : '+ Add Faculty Member'}</div>
          <form onSubmit={handleSubmit} className="form-grid">
            <div className="form-row">
              <div className="form-field">
                <label>Full Name <span className="required">*</span></label>
                <input placeholder="e.g. Dr. D. S. Hirolikar"
                  value={form.name} onChange={e => setForm(f => ({...f, name: e.target.value}))} />
              </div>
              <div className="form-field">
                <label>Short Code <span className="required">*</span></label>
                <input placeholder="e.g. DSH" maxLength={5}
                  value={form.shortCode} onChange={e => setForm(f => ({...f, shortCode: e.target.value.toUpperCase()}))} />
                <span className="field-hint">Used in timetable display (3–5 chars)</span>
              </div>
            </div>
            <div className="form-row">
              <div className="form-field">
                <label>Max Hours / Day</label>
                <input type="number" min={1} max={8} value={form.maxHoursPerDay}
                  onChange={e => setForm(f => ({...f, maxHoursPerDay: +e.target.value}))} />
              </div>
              <div className="form-field">
                <label>Max Hours / Week</label>
                <input type="number" min={1} max={40} value={form.maxHoursPerWeek}
                  onChange={e => setForm(f => ({...f, maxHoursPerWeek: +e.target.value}))} />
              </div>
            </div>

            {/* Unavailable slots grid */}
            <div className="form-field">
              <label>Unavailable Slots (click to mark)</label>
              <span className="field-hint">Purple = unavailable. GA will never schedule this faculty in marked slots.</span>
              <div style={{ overflowX: 'auto', marginTop: 10 }}>
                <table style={{ borderCollapse: 'collapse', fontSize: 12, width: '100%' }}>
                  <thead>
                    <tr>
                      <th style={{ padding: '6px 8px', color: '#9ca3af', textAlign: 'left' }}>Slot</th>
                      {DAYS.map(d => <th key={d} style={{ padding: '6px 10px', color: '#9ca3af' }}>{d}</th>)}
                    </tr>
                  </thead>
                  <tbody>
                    {[1,2,3,4,5,6].map(slot => (
                      <tr key={slot}>
                        <td style={{ padding: '4px 8px', color: '#9ca3af', whiteSpace: 'nowrap' }}>{SLOTS[slot-1]}</td>
                        {DAYS.map(day => {
                          const key = `${day}-${slot}`;
                          const marked = form.unavailableSlots.includes(key);
                          return (
                            <td key={day} style={{ textAlign: 'center', padding: 4 }}>
                              <button type="button" onClick={() => toggleSlot(day, slot)}
                                style={{
                                  width: 28, height: 28, borderRadius: 6, border: 'none', cursor: 'pointer',
                                  background: marked ? '#6C63FF' : '#1e254d',
                                  color: marked ? 'white' : '#9ca3af', fontSize: 12
                                }}>
                                {marked ? '✕' : '·'}
                              </button>
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving…' : editId ? 'Update Faculty' : 'Add Faculty'}
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
            <div className="card-title" style={{ margin: 0 }}>Faculty List</div>
            <div className="search-box">
              <input className="search-input" placeholder="Search by name or code…"
                value={search} onChange={e => setSearch(e.target.value)} />
            </div>
          </div>

          {loading ? (
            <div className="loading-container"><div className="spinner" /><div className="loading-text">Loading faculty…</div></div>
          ) : filtered.length === 0 ? (
            <div className="empty-state"><div className="empty-icon">👤</div><p>{search ? 'No results found.' : 'No faculty added yet.'}</p></div>
          ) : filtered.map(f => (
            <div key={f.id} className="list-item">
              <div className="item-info">
                <div className="item-title">{f.name} <span className="badge badge-primary">{f.shortCode}</span></div>
                <div className="item-meta">
                  <span>Max {f.maxHoursPerDay}h/day</span>
                  <span>Max {f.maxHoursPerWeek}h/week</span>
                  {f.unavailableSlots?.length > 0 && (
                    <span className="badge badge-warning">{f.unavailableSlots.length} restricted slots</span>
                  )}
                </div>
              </div>
              <div className="item-actions">
                <button className="btn btn-secondary btn-sm" onClick={() => handleEdit(f)}>Edit</button>
                <button className="btn btn-danger btn-sm" onClick={() => handleDelete(f.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>

        <div className="nav-actions">
          <Link to="/home"><button className="btn btn-secondary">← Back</button></Link>
          <Link to="/rooms"><button className="btn btn-primary">Next: Rooms →</button></Link>
        </div>
      </div>
      <div className="footer">TimeTableGen · IT Department</div>
    </div>
  );
}