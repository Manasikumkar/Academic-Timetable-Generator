import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Topbar from '../common/Topbar';
import { useToast } from '../common/ToastContext';
import { courseApi, facultyApi } from '../../services/api';
import '../../styles/global.css';

const emptyForm = {
  code:'', fullName:'', type:'THEORY', hoursPerWeek:4,
  credits:4, yearClass:'SE', semester:2, facultyId:''
};

export default function Courses() {
  const toast = useToast();
  const [courses, setCourses] = useState([]);
  const [faculty, setFaculty] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving]   = useState(false);
  const [form, setForm]       = useState(emptyForm);
  const [editId, setEditId]   = useState(null);
  const [filter, setFilter]   = useState('ALL');
  const [semFilter, setSemFilter] = useState('ALL');
  const [search, setSearch]   = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const [cRes, fRes] = await Promise.all([courseApi.getAll(), facultyApi.getAll()]);
      setCourses(cRes.data); setFaculty(fRes.data);
    } catch { toast.error('Failed to load'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const set = (k,v) => setForm(f=>({...f,[k]:v}));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.code.trim()||!form.fullName.trim()) { toast.error('Code and name required'); return; }
    if (!form.facultyId) { toast.error('Please assign a faculty'); return; }
    setSaving(true);
    try {
      const payload = {...form, facultyId:+form.facultyId, semester:+form.semester};
      if (editId) { await courseApi.update(editId, payload); toast.success('Updated!'); }
      else        { await courseApi.create(payload);         toast.success('Added!');   }
      setForm(emptyForm); setEditId(null); load();
    } catch (err) { toast.error(err.response?.data?.message||'Save failed'); }
    finally { setSaving(false); }
  };

  const handleEdit = (c) => {
    setForm({
      code:c.code, fullName:c.fullName, type:c.type,
      hoursPerWeek:c.hoursPerWeek, credits:c.credits,
      yearClass:c.yearClass, semester:c.semester||2,
      facultyId:c.faculty?.id||''
    });
    setEditId(c.id);
    window.scrollTo({top:0,behavior:'smooth'});
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this course?')) return;
    try { await courseApi.delete(id); toast.success('Deleted'); load(); }
    catch { toast.error('Delete failed'); }
  };

  const filtered = courses.filter(c => {
    const my = filter==='ALL'||c.yearClass===filter;
    const ms = semFilter==='ALL'||String(c.semester||2)===semFilter;
    const mt = c.code.toLowerCase().includes(search.toLowerCase())||
               c.fullName.toLowerCase().includes(search.toLowerCase());
    return my&&ms&&mt;
  });

  const countBy = (y) => courses.filter(c=>c.yearClass===y).length;
  const yearColor = { SE:'badge-info', TE:'badge-success', BE:'badge-warning' };

  return (
    <div className="theme-dark">
      <Topbar />
      <div className="container">
        <div className="page-header">
          <h2>Course Management</h2>
          <p>Add courses and tag each with its semester (Odd or Even). The GA uses this to generate the correct semester's timetable.</p>
        </div>

        <div className="stats-card">
          <div className="stat-item"><span className="stat-label">Total</span><span className="stat-value">{courses.length}</span></div>
          <div className="stat-item"><span className="stat-label">SE</span><span className="stat-value">{countBy('SE')}</span></div>
          <div className="stat-item"><span className="stat-label">TE</span><span className="stat-value">{countBy('TE')}</span></div>
          <div className="stat-item"><span className="stat-label">BE</span><span className="stat-value">{countBy('BE')}</span></div>
          <div className="stat-item"><span className="stat-label">Labs</span><span className="stat-value">{courses.filter(c=>c.type==='LAB').length}</span></div>
          <div className="stat-item"><span className="stat-label">Odd Sem</span><span className="stat-value" style={{color:'#F59E0B'}}>{courses.filter(c=>c.semester===1).length}</span></div>
          <div className="stat-item"><span className="stat-label">Even Sem</span><span className="stat-value" style={{color:'#6C63FF'}}>{courses.filter(c=>c.semester===2||!c.semester).length}</span></div>
        </div>

        <div className="info-card">
          <span className="info-icon">ℹ</span>
          <div style={{fontSize:13,color:'#cbd5e1'}}>
            <strong>Semester tagging:</strong> Tag each course with its semester so the GA only picks the right courses.
            Sem-II (Even) courses are tagged with <strong>2</strong>. Sem-I (Odd) courses are tagged with <strong>1</strong>.
            Project Work = <strong>0</strong> (both semesters).
            <br/>
            <strong>Lab hours:</strong> Enter hoursPerWeek=<strong>2</strong> for lab courses — the GA places them as one 2-hour consecutive block per week.
          </div>
        </div>

        {/* Form */}
        <div className="card">
          <div className="card-title">{editId?'✎ Edit Course':'+ Add Course'}</div>
          <form onSubmit={handleSubmit} className="form-grid">
            <div className="form-row">
              <div className="form-field">
                <label>Course Code <span className="required">*</span></label>
                <input placeholder="e.g. DBMS, CG, WAD" value={form.code}
                  onChange={e=>set('code',e.target.value.toUpperCase())} />
              </div>
              <div className="form-field">
                <label>Year Class <span className="required">*</span></label>
                <select value={form.yearClass} onChange={e=>set('yearClass',e.target.value)}>
                  <option value="SE">SE — Second Year</option>
                  <option value="TE">TE — Third Year</option>
                  <option value="BE">BE — Final Year</option>
                </select>
              </div>
            </div>

            <div className="form-field">
              <label>Full Name <span className="required">*</span></label>
              <input placeholder="e.g. Database Management System" value={form.fullName}
                onChange={e=>set('fullName',e.target.value)} />
            </div>

            <div className="form-row">
              <div className="form-field">
                <label>Semester <span className="required">*</span></label>
                <select value={form.semester} onChange={e=>set('semester',+e.target.value)}>
                  <option value={1}>1 — Odd (I / III / V)</option>
                  <option value={2}>2 — Even (II / IV / VI)</option>
                  <option value={0}>0 — Both semesters (Project, TPO)</option>
                </select>
              </div>
              <div className="form-field">
                <label>Type</label>
                <select value={form.type} onChange={e=>set('type',e.target.value)}>
                  <option value="THEORY">Theory (Classroom)</option>
                  <option value="LAB">Lab (2-hr block, I1/I2/I3)</option>
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-field">
                <label>Hours / Week</label>
                <input type="number" min={1} max={8} value={form.hoursPerWeek}
                  onChange={e=>set('hoursPerWeek',+e.target.value)} />
                <span className="field-hint">Theory: 4 | Lab: 2 (one 2-hr block per week)</span>
              </div>
              <div className="form-field">
                <label>Credits</label>
                <input type="number" min={0} max={6} value={form.credits}
                  onChange={e=>set('credits',+e.target.value)} />
              </div>
            </div>

            <div className="form-field">
              <label>Assigned Faculty <span className="required">*</span></label>
              <select value={form.facultyId} onChange={e=>set('facultyId',e.target.value)}>
                <option value="">— Select Faculty —</option>
                {faculty.map(f=>(
                  <option key={f.id} value={f.id}>{f.name} ({f.shortCode})</option>
                ))}
              </select>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving?'Saving…':editId?'Update Course':'Add Course'}
              </button>
              {editId&&(
                <button type="button" className="btn btn-secondary"
                  onClick={()=>{setForm(emptyForm);setEditId(null);}}>Cancel</button>
              )}
            </div>
          </form>
        </div>

        {/* List */}
        <div className="card">
          <div className="card-header">
            <div className="card-title" style={{margin:0}}>Course List</div>
            <div style={{display:'flex',gap:8,flexWrap:'wrap',alignItems:'center'}}>
              <div className="filter-buttons">
                {['ALL','SE','TE','BE'].map(y=>(
                  <button key={y} className={`filter-btn ${filter===y?'active':''}`}
                    onClick={()=>setFilter(y)}>{y}</button>
                ))}
              </div>
              <div className="filter-buttons">
                {[['ALL','All Sem'],['1','Odd'],['2','Even'],['0','Both']].map(([v,l])=>(
                  <button key={v} className={`filter-btn ${semFilter===v?'active':''}`}
                    onClick={()=>setSemFilter(v)}
                    style={{fontSize:11}}>{l}</button>
                ))}
              </div>
              <input className="search-input" style={{width:160}} placeholder="Search…"
                value={search} onChange={e=>setSearch(e.target.value)} />
            </div>
          </div>

          {loading?(
            <div className="loading-container"><div className="spinner"/><div className="loading-text">Loading…</div></div>
          ):filtered.length===0?(
            <div className="empty-state"><div className="empty-icon">📚</div><p>No courses found.</p></div>
          ):filtered.map(c=>(
            <div key={c.id} className="list-item">
              <div className="item-info">
                <div className="item-title">
                  {c.code}
                  <span className={`badge ${yearColor[c.yearClass]}`} style={{marginLeft:8}}>{c.yearClass}</span>
                  <span className={`badge ${c.type==='LAB'?'badge-success':'badge-info'}`} style={{marginLeft:6}}>{c.type}</span>
                  <span className="badge" style={{marginLeft:6,
                    background: c.semester===1?'rgba(245,158,11,0.15)':c.semester===0?'rgba(107,114,128,0.2)':'rgba(108,99,255,0.15)',
                    color:      c.semester===1?'#F59E0B'             :c.semester===0?'#9ca3af'             :'#6C63FF'
                  }}>
                    {c.semester===1?'Odd Sem':c.semester===0?'Both':'Even Sem'}
                  </span>
                </div>
                <div className="item-meta">
                  <span>{c.fullName}</span>
                  <span>{c.hoursPerWeek}h/wk · {c.credits} cr</span>
                  {c.faculty&&<span style={{color:'#6C63FF'}}>👤 {c.faculty.shortCode}</span>}
                </div>
              </div>
              <div className="item-actions">
                <button className="btn btn-secondary btn-sm" onClick={()=>handleEdit(c)}>Edit</button>
                <button className="btn btn-danger btn-sm" onClick={()=>handleDelete(c.id)}>Delete</button>
              </div>
            </div>
          ))}
        </div>

        <div className="nav-actions">
          <Link to="/rooms"><button className="btn btn-secondary">← Rooms</button></Link>
          <Link to="/constraints"><button className="btn btn-primary">Next: Constraints →</button></Link>
        </div>
      </div>
      <div className="footer">TimeTableGen · IT Department</div>
    </div>
  );
}  