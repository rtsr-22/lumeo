import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  interface LoginResponse {
    token: string;
  }

  interface LoginCredentials {
    email: string;
    password: string;
  }

    const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      setError("");

      try {
        const response = await axios.post<LoginResponse>(
          "http://localhost:8080/api/auth/login",
          {
            email,
            password,
          } as LoginCredentials
        );

        const token = response.data.token;
        localStorage.setItem("token", token);

        navigate("/"); // ✅ Redirect Home
      } catch {
        setError("Invalid credentials or server error!");
      }
    };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-6">
      <div className="w-full max-w-md bg-white rounded-lg shadow-md p-8">
        <h2 className="text-2xl font-semibold text-gray-800 mb-4">Login</h2>
        {error && <p className="text-sm text-red-600 mb-4">{error}</p>}

        <form onSubmit={handleLogin} className="space-y-4">
          <input
            type="email"
            placeholder="Email"
            required
            onChange={(e) => setEmail(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />

          <input
            type="password"
            placeholder="Password"
            required
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />

          <button
            type="submit"
            className="w-full bg-indigo-600 text-white py-2 rounded-md hover:bg-indigo-700 transition-colors"
          >
            Login
          </button>

          <div className="text-center">
            <Link to="/signup" className="text-indigo-600 hover:underline text-sm">
              Signup
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}

