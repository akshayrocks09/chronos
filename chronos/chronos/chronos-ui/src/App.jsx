import React from 'react';
import { HashRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Toaster } from 'react-hot-toast';
import './index.css';

// Lazy load pages (to be created)
const Login = React.lazy(() => import('./pages/Login'));
const Register = React.lazy(() => import('./pages/Register'));
const Dashboard = React.lazy(() => import('./pages/Dashboard'));
const Jobs = React.lazy(() => import('./pages/Jobs'));
const Logs = React.lazy(() => import('./pages/Logs'));

const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();
  
  if (loading) return (
    <div className="loading-screen">
      <div className="spinner"></div>
    </div>
  );
  
  if (!user) return <Navigate to="/login" />;
  return children;
};

function App() {
  return (
    <AuthProvider>
      <Toaster 
        position="top-right"
        toastOptions={{
          style: {
            background: 'rgba(30, 41, 59, 0.9)',
            color: '#fff',
            backdropFilter: 'blur(8px)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            padding: '12px 20px',
            borderRadius: '12px',
            fontSize: '14px',
            fontWeight: '500',
            boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.3)',
          },
          success: {
            iconTheme: {
              primary: '#10b981',
              secondary: '#fff',
            },
          },
          error: {
            iconTheme: {
              primary: '#ef4444',
              secondary: '#fff',
            },
          },
        }}
      />
      <Router>
        <Routes>
          <Route path="/login" element={
            <React.Suspense fallback={<div />}>
              <Login />
            </React.Suspense>
          } />
          <Route path="/register" element={
            <React.Suspense fallback={<div />}>
              <Register />
            </React.Suspense>
          } />
          
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <React.Suspense fallback={<div />}>
                <Dashboard />
              </React.Suspense>
            </ProtectedRoute>
          } />
          
          <Route path="/jobs" element={
            <ProtectedRoute>
              <React.Suspense fallback={<div />}>
                <Jobs />
              </React.Suspense>
            </ProtectedRoute>
          } />

          <Route path="/logs" element={
            <ProtectedRoute>
              <React.Suspense fallback={<div />}>
                <Logs />
              </React.Suspense>
            </ProtectedRoute>
          } />

          <Route path="/admin/logs" element={
            <ProtectedRoute>
              <React.Suspense fallback={<div />}>
                <Logs />
              </React.Suspense>
            </ProtectedRoute>
          } />

          <Route path="/" element={<Navigate to="/dashboard" />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
