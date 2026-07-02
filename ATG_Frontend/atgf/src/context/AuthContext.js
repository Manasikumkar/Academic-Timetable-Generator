import React, { createContext, useState, useContext, useEffect } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState({
    token: localStorage.getItem('token'),
    role: localStorage.getItem('role'),
    name: localStorage.getItem('name')
  });

  const login = (token, role, name) => {
    localStorage.setItem('token', token);
    localStorage.setItem('role', role);
    localStorage.setItem('name', name);
    setUser({ token, role, name });
  };

  const logout = () => {
    localStorage.clear();
    setUser({ token: null, role: null, name: null });
  };

  // Optional: verify token on app load (could call a /validate endpoint)
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      // You can optionally validate token with backend here
      setUser({
        token,
        role: localStorage.getItem('role'),
        name: localStorage.getItem('name')
      });
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);