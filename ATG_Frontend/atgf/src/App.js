import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastProvider } from './components/common/ToastContext';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

// Auth pages
import Login from './components/pages/Login';
import Register from './components/pages/Register';

// Existing pages
import SplashScreen    from './components/pages/SplashScreen';
import Home            from './components/pages/Home';
import Faculty         from './components/pages/Faculty';
import Rooms           from './components/pages/Rooms';
import Courses         from './components/pages/Courses';
import Constraints     from './components/pages/Constraints';
import Generate        from './components/pages/Generate';
import TimetableViewer from './components/pages/TimetableViewer';

import './styles/global.css';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            {/* Public routes */}
            <Route path="/" element={<SplashScreen />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Admin only routes */}
            <Route path="/home" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Home />
              </ProtectedRoute>
            } />
            <Route path="/faculty" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Faculty />
              </ProtectedRoute>
            } />
            <Route path="/rooms" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Rooms />
              </ProtectedRoute>
            } />
            <Route path="/courses" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Courses />
              </ProtectedRoute>
            } />
            <Route path="/constraints" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Constraints />
              </ProtectedRoute>
            } />
            <Route path="/generate" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <Generate />
              </ProtectedRoute>
            } />

            {/* All authenticated users (Admin, Teacher, Student) can view timetable */}
            <Route path="/timetable" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'TEACHER', 'STUDENT']}>
                <TimetableViewer />
              </ProtectedRoute>
            } />

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}