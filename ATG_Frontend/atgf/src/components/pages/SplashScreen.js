import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import '../../styles/global.css';

export default function SplashScreen() {
  const navigate = useNavigate();

  useEffect(() => {
    const t = setTimeout(() => navigate('/home'), 2600);
    return () => clearTimeout(t);
  }, [navigate]);

  return (
    <div style={{
      height: '100vh', display: 'flex', justifyContent: 'center',
      alignItems: 'center', background: '#0B0F2A',
      overflow: 'hidden', position: 'relative'
    }}>
      {/* Glow */}
      <div style={{
        position: 'absolute', width: 500, height: 500,
        background: 'radial-gradient(circle, rgba(108,99,255,0.33), transparent 70%)',
        borderRadius: '50%', animation: 'floatAnim 6s infinite alternate ease-in-out'
      }} />

      <div style={{ textAlign: 'center', animation: 'fadeZoom 2.5s ease-in-out', zIndex: 2 }}>
        <div style={{ fontSize: '3rem', fontWeight: 'bold', color: 'white', letterSpacing: 2 }}>
          Time<span style={{
            background: 'linear-gradient(90deg, #6C63FF, #3B82F6)',
            WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent'
          }}>TableGen</span>
        </div>
        <div style={{ marginTop: 14, color: '#9CA3AF', fontSize: '1rem', letterSpacing: 1 }}>
          IT Department Timetable Generator · SE / TE / BE
        </div>
        <div style={{ marginTop: 30 }}>
          <div className="spinner" style={{ margin: '0 auto' }} />
        </div>
      </div>

      <style>{`
        @keyframes floatAnim { from { transform: translateY(-20px); } to { transform: translateY(20px); } }
        @keyframes fadeZoom  { from { opacity: 0; transform: scale(0.85); } to { opacity: 1; transform: scale(1); } }
      `}</style>
    </div>
  );
}