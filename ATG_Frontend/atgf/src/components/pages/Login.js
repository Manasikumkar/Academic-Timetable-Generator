import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import { authApi } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import "../../styles/auth.css";

function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [formData, setFormData] = useState({
    email: "",
    password: ""
  });

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await authApi.login(formData);
      console.log("Login Response:", response.data);

      if (response.data.success) {
        login(response.data.token, response.data.role, response.data.name);
        
        // Role‑based navigation (case‑insensitive)
        const role = response.data.role?.toUpperCase();
        if (role === "ADMIN") {
          navigate("/home");
        } else if (role === "TEACHER" || role === "STUDENT") {
          navigate("/timetable");
        } else {
          alert("Invalid Role");
        }
      } else {
        alert(response.data.message);
      }
    } catch (error) {
      console.error(error);
      alert(error.response?.data?.message || "Login Failed");
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h1>Welcome Back</h1>
        <p>Login to continue</p>
        <form onSubmit={handleSubmit}>
          <input
            type="email"
            name="email"
            placeholder="Email"
            value={formData.email}
            onChange={handleChange}
            required
          />
          <div className="password-box">
            <input
              type={showPassword ? "text" : "password"}
              name="password"
              placeholder="Password"
              value={formData.password}
              onChange={handleChange}
              required
            />
            <span onClick={() => setShowPassword(!showPassword)}>
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </span>
          </div>
          <button type="submit">Login</button>
        </form>
        <p>
          Don't have an account? <Link to="/register">Register</Link>
        </p>
      </div>
    </div>
  );
}

export default Login;