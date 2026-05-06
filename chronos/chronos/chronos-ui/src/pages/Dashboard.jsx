import { useState, useEffect, useRef } from 'react';
import Layout from '../components/Layout';
import api from '../api/client';
import { 
  Activity, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  PlayCircle,
  AlertCircle,
  RefreshCcw,
  Plus
} from 'lucide-react';
import { Link } from 'react-router-dom';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastRefreshed, setLastRefreshed] = useState(null);
  const pollRef = useRef(null);

  const fetchStats = async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const response = await api.get('/jobs/stats');
      setStats(response.data.data);
      setError(null);
      setLastRefreshed(new Date());
    } catch (err) {
      setError('Failed to load dashboard statistics.');
      console.error(err);
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
    pollRef.current = setInterval(() => fetchStats(true), 15000);
    return () => clearInterval(pollRef.current);
  }, []);

  const StatCard = ({ title, value, icon: Icon, color, delay }) => (
    <div className="glass-card stat-card fade-in" style={{ animationDelay: `${delay}s` }}>
      <div className={`stat-icon-wrapper ${color}`}>
        <Icon size={24} />
      </div>
      <div className="stat-info">
        <h3>{title}</h3>
        <p className="stat-value">{value ?? '0'}</p>
      </div>
    </div>
  );

  return (
    <Layout>
      <div className="dashboard-header flex justify-between items-center mb-8">
        <div>
          <h1 className="page-title">Dashboard</h1>
          <p className="page-subtitle">
            Overview of your scheduled jobs and system status
            {lastRefreshed && <span style={{marginLeft:12,fontSize:12,opacity:0.5}}>· live · updated {lastRefreshed.toLocaleTimeString()}</span>}
          </p>
        </div>
        <div className="flex gap-4">
          <button onClick={fetchStats} className="btn-secondary" title="Refresh Stats">
            <RefreshCcw size={18} className={loading ? 'animate-spin' : ''} />
          </button>
          <Link to="/jobs" className="btn-primary flex items-center gap-2">
            <Plus size={18} /> New Job
          </Link>
        </div>
      </div>

      {error ? (
        <div className="glass-card error-boundary">
          <AlertCircle size={40} className="error-icon" />
          <p>{error}</p>
          <button onClick={fetchStats} className="btn-primary mt-4">Retry</button>
        </div>
      ) : loading && !stats ? (
        <div className="stats-grid">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="glass-card stat-card skeleton" />
          ))}
        </div>
      ) : (
        <>
          <div className="stats-grid">
            <StatCard 
              title="Total Jobs" 
              value={stats?.totalJobs} 
              icon={Activity} 
              color="blue" 
              delay={0.1}
            />
            <StatCard 
              title="Scheduled" 
              value={stats?.scheduledJobs} 
              icon={Clock} 
              color="purple" 
              delay={0.2}
            />
            <StatCard 
              title="Running" 
              value={stats?.runningJobs} 
              icon={PlayCircle} 
              color="orange" 
              delay={0.3}
            />
            <StatCard 
              title="Completed" 
              value={stats?.completedJobs} 
              icon={CheckCircle2} 
              color="green" 
              delay={0.4}
            />
            <StatCard 
              title="Failed" 
              value={stats?.failedJobs} 
              icon={XCircle} 
              color="red" 
              delay={0.5}
            />
            <StatCard 
              title="Cancelled" 
              value={stats?.cancelledJobs} 
              icon={AlertCircle} 
              color="gray" 
              delay={0.6}
            />
          </div>

          <div className="mt-12">
            <h2 className="section-title">Quick Actions</h2>
            <div className="actions-grid mt-6">
              <Link to="/jobs" className="glass-card action-card">
                <div className="action-icon purple"><Clock /></div>
                <h4>Manage Schedules</h4>
                <p>View and edit your recurring and one-time jobs.</p>
              </Link>
              <Link to="/logs" className="glass-card action-card">
                <div className="action-icon blue"><Activity /></div>
                <h4>Performance Logs</h4>
                <p>Analyze recent execution throughput and errors.</p>
              </Link>
            </div>
          </div>
        </>
      )}

    </Layout>
  );
};

export default Dashboard;
