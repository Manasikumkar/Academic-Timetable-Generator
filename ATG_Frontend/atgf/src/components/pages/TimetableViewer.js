import React, { useState, useEffect, useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import Topbar from '../common/Topbar';
import { useToast } from '../common/ToastContext';
import { timetableApi, courseApi, facultyApi, roomApi } from '../../services/api'; // added courseApi, facultyApi, roomApi
import '../../styles/global.css';

const DAYS  = ['MON','TUE','WED','THU','FRI','SAT'];
const YEARS = ['SE','TE','BE'];
const YLBL  = { SE:'2IT (SE)', TE:'3IT (TE)', BE:'4IT (BE)' };
const STIMES = {
  1:'09:00–10:00', 2:'10:00–11:00', 3:'11:10–12:10',
  4:'12:10–13:10', 5:'14:00–15:00', 6:'15:00–16:00'
};
const LAB_BLOCK_LABEL = {
  1:'09:00 – 11:00', 3:'11:10 – 13:10', 5:'14:00 – 16:00'
};
const RSHORT = {
  SOFTWARE_LAB:'SW Lab', LINUX_LAB:'Linux Lab',
  PROJECT_LAB:'Proj Lab', CLASSROOM:'Room'
};

// ─── Build grid (unchanged) ───────────────────────────────────────────────
function buildGrid(rawSlots) {
  const labMin = new Map();
  for (const s of rawSlots) {
    if (!s.labSession) continue;
    const key = `${s.yearClass}|${s.day}|${s.course.code}|${s.division||''}`;
    const existing = labMin.get(key);
    if (!existing || s.slotNumber < existing.slotNumber) labMin.set(key, s);
  }
  const theorySlots = rawSlots.filter(s => !s.labSession);
  const combined = [...theorySlots, ...labMin.values()];
  const grid = {};
  for (const y of YEARS) {
    grid[y] = {};
    for (const d of DAYS) {
      grid[y][d] = {};
      for (let s = 1; s <= 6; s++) grid[y][d][s] = { theory: null, labs: {} };
    }
  }
  for (const s of combined) {
    const yr  = s.yearClass;
    const day = s.day;
    const sn  = s.slotNumber;
    if (!grid[yr]?.[day]?.[sn]) continue;
    if (s.labSession) {
      const code = s.course.code;
      if (!grid[yr][day][sn].labs[code]) grid[yr][day][sn].labs[code] = [];
      const div = s.division || '';
      const already = grid[yr][day][sn].labs[code].some(x => (x.division||'') === div);
      if (!already) grid[yr][day][sn].labs[code].push(s);
    } else {
      if (!grid[yr][day][sn].theory) grid[yr][day][sn].theory = s;
    }
  }
  return grid;
}

// ─── Theory cell ──────────────────────────────────────────────────────────
function TheoryTd({ ts, handleEdit }) {
  if (!ts) return <td style={S.empty} className="slot">—</td>;
  const c = (ts.course?.code || '').toUpperCase();
  if (['INTERN','INTERNSHIP'].includes(c))
    return <td style={S.special}><div style={S.spc}>Internship</div><div style={S.spf}>{ts.faculty?.shortCode}</div></td>;
  if (['TPOBE','TPO'].includes(c))
    return <td style={S.special}><div style={S.spc}>TPO</div><div style={S.spf}>{ts.faculty?.shortCode}</div></td>;
  if (c === 'PROJECT')
    return <td style={S.project}><div style={S.pc}>PROJECT WORK</div><div style={S.pr}>Proj Lab</div><div style={S.pf}>DSH · ABG · RMK · SS · SJ</div></td>;
  if (['AUDBE','AUDTE','AUDIT'].includes(c))
    return <td style={S.theory}><div style={S.tc}>Audit</div><div style={S.tf}>{ts.faculty?.shortCode}</div><div style={S.tr}>{ts.room?.name}</div></td>;
  return (
    <td style={S.theory} onClick={() => handleEdit(ts)} className="slot" title="Click to edit">
      <div style={S.tc}>{ts.course?.code}</div>
      <div style={S.tf}>{ts.faculty?.shortCode}</div>
      <div style={S.tr}>{ts.room?.name}</div>
    </td>
  );
}

// ─── Lab 2-hr merged cell (colSpan=2) ─────────────────────────────────────
function LabTd({ batches, startSlot, handleEdit }) {
  const sorted = [...batches].sort((a, b) => (a.division || '').localeCompare(b.division || ''));
  const timeLabel = LAB_BLOCK_LABEL[startSlot] || `${STIMES[startSlot]} – ${STIMES[startSlot+1] || ''}`;
  return (
    <td style={S.lab} colSpan={2} onClick={() => sorted.length > 0 && handleEdit(sorted[0])} className="slot" title="Click to edit lab">
      <div style={S.labHdr}>{timeLabel} &nbsp;·&nbsp; 2 hrs &nbsp;·&nbsp; All batches simultaneous</div>
      {sorted.map((ts, i) => (
        <div key={ts.division || i} style={{ ...S.labRow, borderBottom: i < sorted.length-1 ? '1px solid rgba(16,185,129,0.18)' : 'none' }}>
          <span style={S.badge}>{ts.division || '?'}</span>
          <span style={S.lc}>{ts.course?.code}</span>
          <span style={S.lr}>{RSHORT[ts.room?.type] || ts.room?.name || '?'}</span>
          <span style={S.lf}>{ts.faculty?.shortCode || '?'}</span>
        </div>
      ))}
    </td>
  );
}

// ─── Row cells (unchanged except passing handlers) ────────────────────────
function RowCells({ grid, year, day, yi, handleEdit, handleEmptyClick }) {
  const isSat = day === 'SAT';
  const cells = [];
  let skipAt = -1;
  for (let s = 1; s <= 6; s++) {
    if (s === 3 && yi === 0) cells.push(<td key="sb" style={S.brk} rowSpan={3}>S<br/>H<br/>O<br/>R<br/>T<br/><br/>B<br/>R<br/>K</td>);
    if (s === 5 && !isSat && yi === 0) cells.push(<td key="lb" style={S.brk} rowSpan={3}>L<br/>U<br/>N<br/>C<br/>H<br/><br/>B<br/>R<br/>K</td>);
    if (isSat && s === 5) {
      if (yi === 0) cells.push(<td key="so" style={S.satOff} colSpan={3} rowSpan={3}>—</td>);
      break;
    }
    if (s === skipAt) { skipAt = -1; continue; }
    const cell = grid[year]?.[day]?.[s];
    if (!cell) {
      cells.push(<td key={s} style={S.empty} onClick={() => handleEmptyClick(year, day, s)} className="slot">—</td>);
      continue;
    }
    const labCodes = Object.keys(cell.labs);
    if (labCodes.length > 0) {
      let allBatches = [];
      labCodes.forEach(code => { allBatches = allBatches.concat(cell.labs[code]); });
      const endSlot = s + 1 <= (isSat ? 4 : 6) ? s + 1 : s;
      cells.push(<LabTd key={s} batches={allBatches} startSlot={s} handleEdit={handleEdit} />);
      if (endSlot > s) skipAt = endSlot;
    } else if (cell.theory) {
      cells.push(<TheoryTd key={s} ts={cell.theory} handleEdit={handleEdit} />);
    } else {
      cells.push(<td key={s} style={S.empty} onClick={() => handleEmptyClick(year, day, s)} className="slot">—</td>);
    }
  }
  return <>{cells}</>;
}

function UnifiedGrid({ rawSlots, handleEdit, handleEmptyClick }) {
  const grid = buildGrid(rawSlots);
  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={S.table}>
        <thead><tr>
          <th style={{ ...S.th, width: 34 }}>DAY</th>
          <th style={{ ...S.th, width: 54 }}>Class</th>
          <th style={{ ...S.th, width: 116 }}>1<br/><span style={S.sub}>09:00–10:00</span></th>
          <th style={{ ...S.th, width: 116 }}>2<br/><span style={S.sub}>10:00–11:00</span></th>
          <th style={{ ...S.th, width: 24, background:'#1a1f3a', fontSize:7 }}>S<br/>B<br/>K</th>
          <th style={{ ...S.th, width: 116 }}>3<br/><span style={S.sub}>11:10–12:10</span></th>
          <th style={{ ...S.th, width: 116 }}>4<br/><span style={S.sub}>12:10–13:10</span></th>
          <th style={{ ...S.th, width: 24, background:'#1a1f3a', fontSize:7 }}>L<br/>N<br/>H</th>
          <th style={{ ...S.th, width: 116 }}>5<br/><span style={S.sub}>14:00–15:00</span></th>
          <th style={{ ...S.th, width: 116 }}>6<br/><span style={S.sub}>15:00–16:00</span></th>
        </tr></thead>
        <tbody>
          {DAYS.map(day => (
            <React.Fragment key={day}>
              {day === 'SAT' && (
                <tr>
                  <td style={{ ...S.th, padding:3, fontSize:9 }} colSpan={2}>SAT</td>
                  <td style={{ ...S.th, fontSize:9 }}>1<br/><span style={S.sub}>09:00–10:00</span></td>
                  <td style={{ ...S.th, fontSize:9 }}>2<br/><span style={S.sub}>10:00–11:00</span></td>
                  <td style={{ ...S.th, width:24, background:'#1a1f3a', fontSize:7 }}>BRK</td>
                  <td style={{ ...S.th, fontSize:9 }}>3<br/><span style={S.sub}>11:10–12:10</span></td>
                  <td style={{ ...S.th, fontSize:9 }}>4<br/><span style={S.sub}>12:10–13:10</span></td>
                  <td style={{ ...S.satOff, fontSize:9, fontStyle:'italic' }} colSpan={3}>No classes after 13:10</td>
                </tr>
              )}
              {YEARS.map((year, yi) => (
                <tr key={year} style={{ borderBottom: yi === 2 ? '2px solid #2a2f5a' : 'none' }}>
                  {yi === 0 && <td style={S.dayLbl} rowSpan={3}>{day}</td>}
                  <td style={S.clsLbl}>{YLBL[year]}</td>
                  <RowCells grid={grid} year={year} day={day} yi={yi} handleEdit={handleEdit} handleEmptyClick={handleEmptyClick} />
                </tr>
              ))}
            </React.Fragment>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Legend({ rawSlots }) {
  const m = { SE:{}, TE:{}, BE:{} };
  for (const s of rawSlots) {
    const yr = s.yearClass, code = s.course?.code;
    if (code && !m[yr]?.[code])
      m[yr][code] = { full: s.course.fullName, fac: s.faculty?.shortCode, lab: s.labSession };
  }
  return (
    <div style={{ display:'flex', gap:0, marginTop:14, padding:'10px 14px', background:'#111633', border:'1px solid #2a2f5a', borderRadius:8, flexWrap:'wrap' }}>
      {YEARS.map((yr, i) => (
        <div key={yr} style={{ flex:'1 1 200px', paddingRight:10, borderRight: i<2 ? '1px solid #2a2f5a' : 'none', paddingLeft: i>0 ? 10 : 0 }}>
          <div style={{ fontWeight:600, fontSize:12, marginBottom:6, color:'#6C63FF' }}>{YLBL[yr]}</div>
          {Object.entries(m[yr]).map(([code, info]) => (
            <div key={code} style={{ display:'flex', gap:5, marginBottom:3, fontSize:11, alignItems:'center' }}>
              <span style={{ color: info.lab ? '#10B981' : '#6C63FF', fontSize:10 }}>■</span>
              <span style={{ color:'#fff', fontWeight:600, minWidth:54 }}>{code}</span>
              <span style={{ color:'#9ca3af', fontSize:10 }}>{info.full}</span>
              <span style={{ color:'#6C63FF', fontSize:10 }}>({info.fac})</span>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}

// Styles (unchanged)
const S = {
  table:   { borderCollapse:'collapse', width:'100%', tableLayout:'fixed', background:'#0f1433', fontSize:10 },
  th:      { background:'#0d1024', color:'#fff', fontWeight:600, border:'1px solid #2a2f5a', padding:'5px 3px', textAlign:'center', fontSize:10, lineHeight:1.3 },
  sub:     { fontSize:8, color:'#9ca3af', fontWeight:400 },
  dayLbl:  { background:'#0d1024', color:'#fff', fontWeight:700, fontSize:12, textAlign:'center', writingMode:'vertical-rl', transform:'rotate(180deg)', padding:'4px 2px', border:'1px solid #2a2f5a' },
  clsLbl:  { background:'#111633', color:'#cbd5e1', fontWeight:600, fontSize:9.5, textAlign:'center', border:'1px solid #2a2f5a', whiteSpace:'nowrap', padding:'3px 2px' },
  brk:     { background:'#1a1f3a', width:24, border:'1px solid #2a2f5a', padding:'2px 1px', fontSize:7, color:'#6b7280', textAlign:'center', lineHeight:1.2, verticalAlign:'middle' },
  satOff:  { background:'#1e254d', color:'#555', textAlign:'center', border:'1px solid #2a2f5a', padding:'3px' },
  empty:   { background:'#0d1024', color:'#2a2f5a', textAlign:'center', border:'1px solid #1e254d', fontSize:12, cursor:'pointer' },
  reserve: { background:'rgba(245,158,11,0.08)', color:'#F59E0B', textAlign:'center', fontSize:9, fontStyle:'italic', border:'1px solid rgba(245,158,11,0.2)', verticalAlign:'middle', padding:'4px 2px' },
  theory:  { background:'rgba(67,97,238,0.13)', borderLeft:'3px solid #4361ee', textAlign:'left', padding:'5px 6px', border:'1px solid #2a2f5a', verticalAlign:'top', cursor:'pointer' },
  tc:      { fontWeight:700, fontSize:12, color:'#e0e7ff', lineHeight:1.3 },
  tf:      { fontSize:10, color:'#818cf8', marginTop:2 },
  tr:      { fontSize:9,  color:'#9ca3af', marginTop:1 },
  lab:     { background:'rgba(16,185,129,0.09)', borderLeft:'3px solid #10B981', textAlign:'left', padding:'5px 6px', border:'1px solid #2a2f5a', verticalAlign:'top', cursor:'pointer' },
  labHdr:  { fontSize:8, color:'#34d399', marginBottom:5, fontWeight:600, letterSpacing:0.2, lineHeight:1.4 },
  labRow:  { display:'flex', gap:5, alignItems:'center', paddingTop:2 },
  badge:   { background:'rgba(16,185,129,0.25)', color:'#34d399', fontWeight:700, fontSize:9, padding:'1px 5px', borderRadius:8, minWidth:20, textAlign:'center', flexShrink:0 },
  lc:      { fontWeight:700, color:'#d1fae5', fontSize:11, minWidth:48, flexShrink:0 },
  lr:      { color:'#9ca3af', fontSize:9, minWidth:50, flexShrink:0 },
  lf:      { color:'#6ee7b7', fontSize:9 },
  special: { background:'rgba(245,158,11,0.10)', textAlign:'center', border:'1px solid #2a2f5a', padding:'5px 3px', verticalAlign:'top' },
  spc:     { fontWeight:700, fontSize:11, color:'#fcd34d' },
  spf:     { fontSize:9, color:'#F59E0B', marginTop:2 },
  project: { background:'rgba(126,34,206,0.12)', textAlign:'center', border:'1px solid #2a2f5a', padding:'5px 3px', verticalAlign:'top' },
  pc:      { fontWeight:700, fontSize:10, color:'#c084fc' },
  pr:      { fontSize:9, color:'#9ca3af', marginTop:1 },
  pf:      { fontSize:8, color:'#a78bfa', marginTop:2 },
};

// ─── Main component ────────────────────────────────────────────────────────
export default function TimetableViewer() {
  const toast = useToast();
  const [searchParams] = useSearchParams();
  const [versions, setVersions] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [version, setVersion] = useState(null);
  const [slots, setSlots] = useState([]);
  const [conflicts, setConflicts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState('grid');

  // Edit state
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [courseId, setCourseId] = useState("");
  const [facultyId, setFacultyId] = useState("");
  const [roomId, setRoomId] = useState("");
  const [courses, setCourses] = useState([]);
  const [faculties, setFaculties] = useState([]);
  const [rooms, setRooms] = useState([]);

  // Handlers
  const handleEmptyClick = (year, day, slotNumber) => {
    setSelectedSlot({ id: null, yearClass: year, day, slotNumber, labSession: false });
    setCourseId(""); setFacultyId(""); setRoomId("");
    setShowModal(true);
  };
  const handleEdit = (slot) => {
    setSelectedSlot(slot);
    setCourseId(slot.course?.id || "");
    setFacultyId(slot.faculty?.id || "");
    setRoomId(slot.room?.id || "");
    setShowModal(true);
  };

  // Load versions
  const loadVersions = useCallback(async () => {
    try {
      const res = await timetableApi.getAllVersions();
      setVersions(res.data);
      const pid = searchParams.get('versionId');
      const tgt = pid ? res.data.find(v => v.id === +pid) : res.data.find(v => v.status === 'DEPLOYED') || res.data[0];
      if (tgt) setSelectedId(tgt.id);
    } catch { toast.error('Failed to load versions'); }
    finally { setLoading(false); }
  }, [searchParams, toast]);

  useEffect(() => { loadVersions(); }, [loadVersions]);

  useEffect(() => {
    if (!selectedId) return;
    setVersion(versions.find(v => v.id === selectedId) || null);
    (async () => {
      try {
        const [sRes, cRes] = await Promise.all([
          timetableApi.getSlots(selectedId),
          timetableApi.getConflicts(selectedId),
        ]);
        setSlots(sRes.data);
        setConflicts(cRes.data.conflicts || []);
      } catch { toast.error('Failed to load timetable'); }
    })();
  }, [selectedId, versions, toast]);

  // Load courses, faculties, rooms for dropdowns
  useEffect(() => {
    const loadData = async () => {
      try {
        const [cRes, fRes, rRes] = await Promise.all([
          courseApi.getAll(),
          facultyApi.getAll(),
          roomApi.getAll()
        ]);
        setCourses(cRes.data);
        setFaculties(fRes.data);
        setRooms(rRes.data);
      } catch (err) {
        console.error(err);
        toast.error('Failed to load master data');
      }
    };
    loadData();
  }, [toast]);

  // Save edited slot
  const saveEdit = async () => {
    if (!courseId || !facultyId || !roomId) {
      toast.error("Please fill all fields");
      return;
    }
    try {
      const payload = {
        courseId: Number(courseId),
        facultyId: Number(facultyId),
        roomId: Number(roomId),
      };
      if (selectedSlot.id) {
        // Update existing slot
        await timetableApi.updateSlot(selectedSlot.id, payload);
      } else {
        // Create new slot (for empty cell)
        await timetableApi.createSlot({
          ...payload,
          yearClass: selectedSlot.yearClass,
          day: selectedSlot.day,
          slotNumber: selectedSlot.slotNumber,
          timetableVersionId: selectedId
        });
      }
      toast.success('Saved successfully');
      setShowModal(false);
      // Refresh slots
      const sRes = await timetableApi.getSlots(selectedId);
      setSlots(sRes.data);
    } catch (err) {
      console.error(err);
      toast.error(err.response?.data?.message || 'Save failed');
    }
  };

  const doFinalize = async () => {
    try { await timetableApi.finalize(selectedId); toast.success('Marked Final'); loadVersions(); }
    catch { toast.error('Failed'); }
  };
  const doDeploy = async () => {
    if (!window.confirm('Deploy this timetable?')) return;
    try { await timetableApi.deploy(selectedId); toast.success('Deployed!'); loadVersions(); }
    catch { toast.error('Failed'); }
  };
  const doExport = () => {
    fetch(`/api/timetable/${selectedId}/export/pdf`)
      .then(res => res.blob())
      .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "timetable.pdf";
        a.click();
        window.URL.revokeObjectURL(url);
      })
      .catch(err => toast.error('Export failed'));
  };

  const stCol = { DRAFT:'#3B82F6', FINAL:'#F59E0B', DEPLOYED:'#10B981' };

  if (loading) return (
    <div className="theme-dark"><Topbar/>
      <div className="container loading-container"><div className="spinner"/></div>
    </div>
  );

  return (
    <div className="theme-dark">
      <Topbar/>
      <div className="container" style={{ maxWidth:'100%', padding:'20px 24px' }}>
        <div className="page-header">
          <h2>Timetable Viewer</h2>
          <p>All 3 years · Labs = 2-hr blocks · I1/I2/I3 simultaneous in different rooms · Each batch has own faculty</p>
        </div>

        {/* Version selector */}
        <div className="card" style={{ padding:'14px 18px', marginBottom:14 }}>
          <div style={{ display:'flex', gap:12, alignItems:'center', flexWrap:'wrap' }}>
            <span style={{ fontWeight:600, fontSize:13, color:'#cbd5e1', whiteSpace:'nowrap' }}>Version:</span>
            <select className="filter-select" style={{ flex:1, maxWidth:460 }}
              value={selectedId || ''} onChange={e => setSelectedId(+e.target.value)}>
              {versions.length === 0 && <option>No versions — generate first</option>}
              {versions.map(v => (
                <option key={v.id} value={v.id}>
                  {v.name} — {v.status} — {v.hardConflicts === 0 ? '✓ No conflicts' : `${v.hardConflicts} conflicts`}
                </option>
              ))}
            </select>
            {version && (
              <div style={{ display:'flex', gap:8, flexWrap:'wrap' }}>
                <span className="badge" style={{ background:stCol[version.status]+'22', color:stCol[version.status] }}>{version.status}</span>
               
                <span className={`badge ${version.hardConflicts === 0 ? 'badge-success' : 'badge-danger'}`}>
                  {version.hardConflicts === 0 ? '✓ No Conflicts' : `${version.hardConflicts} Conflicts`}
                </span>
              </div>
            )}
          </div>
        </div>

        {!selectedId || !version ? (
          <div className="empty-state">
            <div className="empty-icon">📅</div>
            <p>No timetable selected. <Link to="/generate" style={{ color:'#6C63FF' }}>Generate one →</Link></p>
          </div>
        ) : (
          <>
            <div style={{ display:'flex', gap:8, marginBottom:14, flexWrap:'wrap', alignItems:'center', justifyContent:'space-between' }}>
              <div className="filter-buttons">
                <button className={`filter-btn ${tab === 'grid' ? 'active' : ''}`} onClick={() => setTab('grid')}>📅 Timetable</button>
               
              </div>
              <div style={{ display:'flex', gap:8 }}>
                <button className="btn btn-secondary btn-sm" onClick={doExport}>📄 Export PDF</button>
                {version.status === 'DRAFT' && <button className="btn btn-secondary btn-sm" onClick={doFinalize}>✓ Mark Final</button>}
                {(version.status === 'DRAFT' || version.status === 'FINAL') && (
                  <button className="btn btn-success btn-sm" onClick={doDeploy} disabled={version.hardConflicts > 0}>🚀 Deploy</button>
                )}
              </div>
            </div>

            {tab === 'grid' && (
              <div style={{ background:'#111633', borderRadius:12, padding:16, border:'1px solid rgba(255,255,255,0.05)' }}>
                <div style={{ display:'flex', gap:16, marginBottom:10, fontSize:11, flexWrap:'wrap', alignItems:'center' }}>
                  <span style={{ fontWeight:600, fontSize:14, color:'#fff' }}>{version.name}</span>
                  <span style={{ color:'#9ca3af' }}>IT Dept · Sem-II · 2025-26</span>
                  <div style={{ display:'flex', gap:14, marginLeft:'auto', flexWrap:'wrap' }}>
                    <span style={{ color:'#9ca3af' }}><span style={{ display:'inline-block', width:10, height:10, background:'rgba(67,97,238,0.5)', borderRadius:2, marginRight:4 }}/>Theory: Code / Faculty / Room</span>
                    <span style={{ color:'#9ca3af' }}><span style={{ display:'inline-block', width:10, height:10, background:'rgba(16,185,129,0.4)', borderRadius:2, marginRight:4 }}/>Lab (2 hrs): I1/I2/I3 parallel, different rooms & faculty</span>
                  </div>
                </div>
                <UnifiedGrid rawSlots={slots} handleEdit={handleEdit} handleEmptyClick={handleEmptyClick} />
                <Legend rawSlots={slots} />
              </div>
            )}

            {tab === 'conflicts' && (
              <div className="card">
                <div className="card-title">{version.hardConflicts === 0 ? '✅ Conflict-Free' : `⚠ ${version.hardConflicts} Conflicts Detected`}</div>
                {version.hardConflicts === 0 ? (
                  <div className="empty-state">
                    <div className="empty-icon">✅</div>
                    <p>This timetable is completely conflict-free.</p>
                    <div style={{ marginTop:14, display:'flex', gap:10, justifyContent:'center' }}>
                      <button className="btn btn-success" onClick={doDeploy}>🚀 Deploy</button>
                      <button className="btn btn-secondary" onClick={doExport}>📄 Export PDF</button>
                    </div>
                  </div>
                ) : (
                  ['FACULTY_CLASH','ROOM_CLASH','CLASS_CLASH'].map(type => {
                    const g = conflicts.filter(c => c.type === type);
                    if (!g.length) return null;
                    return (
                      <div key={type} style={{ marginBottom:16 }}>
                        <div style={{ fontSize:11, fontWeight:700, color:'#ef4444', textTransform:'uppercase', marginBottom:8, letterSpacing:1 }}>
                          {type.replace(/_/g, ' ')} ({g.length})
                        </div>
                        {g.map((c, i) => (
                          <div key={i} className="conflict-item">
                            <div className="conflict-desc">{c.description}</div>
                            <div className="conflict-time">{c.day} · {c.timeLabel}</div>
                          </div>
                        ))}
                      </div>
                    );
                  })
                )}
              </div>
            )}
          </>
        )}

        <div className="nav-actions" style={{ marginTop:16 }}>
          <Link to="/generate"><button className="btn btn-secondary">← Generate</button></Link>
          <Link to="/home"><button className="btn btn-primary">Dashboard →</button></Link>
        </div>
      </div>
      <div className="footer">TimeTableGen · IT Department · P.D.E.A's College of Engineering</div>

      {/* Modal for editing */}
      {showModal && (
        <div className="modal">
          <div className="modal-content">
            <h3>{selectedSlot?.labSession ? "Edit Lab" : "Edit Slot"}</h3>
            <select value={courseId} onChange={(e) => setCourseId(e.target.value)}>
              <option value="">Select Course</option>
              {courses.map(c => (
                <option key={c.id} value={c.id}>{c.code} – {c.fullName}</option>
              ))}
            </select>
            <select value={facultyId} onChange={(e) => setFacultyId(e.target.value)}>
              <option value="">Select Faculty</option>
              {faculties.map(f => (
                <option key={f.id} value={f.id}>{f.name} ({f.shortCode})</option>
              ))}
            </select>
            <select value={roomId} onChange={(e) => setRoomId(e.target.value)}>
              <option value="">Select Room</option>
              {rooms.map(r => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </select>
            <div style={{ marginTop: 20, display: 'flex', gap: 10 }}>
              <button className="btn btn-primary" onClick={saveEdit}>Save</button>
              <button className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}