import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { UserPlus, LogIn, ShieldCheck } from 'lucide-react';

const Register = () => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    const result = await register(username, email, password);
    if (result.success) {
      navigate('/login');
    } else {
      if (result.details) {
        setError(
          <div style={{ textAlign: 'left' }}>
            <p style={{ margin: '0 0 5px 0' }}><strong>{result.message}</strong></p>
            <ul style={{ margin: 0, paddingLeft: '20px', fontSize: '0.9em' }}>
              {Object.entries(result.details).map(([field, msg]) => (
                <li key={field}>{field}: {msg}</li>
              ))}
            </ul>
          </div>
        );
      } else {
        setError(result.message);
      }
    }
    setLoading(false);
  };

  return (
    <div className="auth-container">
      <div className="auth-bg"></div>
      <div className="glass-card auth-card fade-in">
        <div className="auth-header">
          <div className="logo-glow">
            <ShieldCheck size={40} className="secondary-icon" />
          </div>
          <h1>Chronos</h1>
          <p>Join the future of job scheduling</p>
        </div>

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="input-group">
            <label htmlFor="username">Username</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              placeholder="Pick a username"
            />
          </div>

          <div className="input-group">
            <label htmlFor="email">Email Address</label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="you@example.com"
            />
          </div>

          <div className="input-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="••••••••"
            />
          </div>

          <button 
            type="submit" 
            className="btn-primary w-full" 
            disabled={loading}
            style={{ background: 'linear-gradient(135deg, var(--secondary), var(--primary))' }}
          >
            {loading ? 'Creating timeline...' : (
              <span className="flex items-center justify-center gap-2">
                <UserPlus size={20} /> Create Account
              </span>
            )}
          </button>
        </form>

        <div className="auth-footer">
          <p>Already have an account?</p>
          <Link to="/login" className="secondary-link">
            <LogIn size={16} /> Sign In
          </Link>
        </div>
      </div>

    </div>
  );
};

export default Register;
