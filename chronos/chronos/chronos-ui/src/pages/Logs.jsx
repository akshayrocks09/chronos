import { useState, useEffect } from 'react';
import Layout from '../components/Layout';
import api from '../api/client';
import { 
  Activity, 
  CheckCircle2, 
  XCircle, 
  Clock,
  AlertCircle,
  RefreshCcw,
  Search,
  ExternalLink,
  Filter
} from 'lucide-react';
import { useLocation, Link } from 'react-router-dom';

const Logs = () => {
  const location = useLocation();
  const isAdminView = location.pathname.startsWith('/admin');
  
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');

  const fetchLogs = async (pageNum = 0) => {
    setLoading(true);
    try {
      const endpoint = isAdminView ? '/api/jobs/admin/logs' : '/api/jobs/logs';
      // Note: client.js prepends /api but let's check our client config
      // Usually api.get('/jobs/logs') -> /api/jobs/logs
      // So here we need /jobs/admin/logs if isAdminView
      
       const response = await api.get(isAdminView ? '/admin/logs' : '/jobs/logs', {
        params: {
          page: pageNum,
          size: 20,
          status: statusFilter || undefined
        }
      });
      setLogs(response.data.data?.content || []);
      setTotalPages(response.data.data?.totalPages || 0);
      setPage(pageNum);
      setError(null);
    } catch (err) {
      setError('Failed to load performance logs.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs(0);
  }, [statusFilter, location.pathname]);

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    let date;
    if (Array.isArray(dateString)) {
      // Spring LocalDateTime arrays: [year, month, day, hour, minute, second]
      // Note: JS Date months are 0-indexed, Spring is 1-indexed
      const [year, month, day, hour = 0, minute = 0, second = 0] = dateString;
      date = new Date(year, month - 1, day, hour, minute, second);
    } else {
      date = new Date(dateString);
    }
    return date.toLocaleString();
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <span className="badge green"><CheckCircle2 size={12} className="inline mr-1" /> Success</span>;
      case 'FAILED':
        return <span className="badge red"><XCircle size={12} className="inline mr-1" /> Failed</span>;
      case 'RUNNING':
        return <span className="badge blue animate-pulse"><RefreshCcw size={12} className="inline mr-1 spin" /> Running</span>;
      default:
        return <span className="badge gray">{status}</span>;
    }
  };

  return (
    <Layout>
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="page-title">{isAdminView ? 'System Logs' : 'My Activity'}</h1>
          <p className="page-subtitle">
            {isAdminView 
              ? 'Real-time execution history across the entire system' 
              : 'Detailed history of your personal job executions'}
          </p>
        </div>
        <div className="flex gap-4">
          <div className="filter-select">
            <Filter size={16} />
            <select 
              value={statusFilter} 
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="COMPLETED">Completed</option>
              <option value="FAILED">Failed</option>
              <option value="RUNNING">Running</option>
            </select>
          </div>
          <button onClick={() => fetchLogs(page)} className="btn-secondary" title="Refresh Logs">
            <RefreshCcw size={18} className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      <div className="glass-card fade-in">
        <div className="table-container">
          {error ? (
            <div className="error-boundary py-12">
              <AlertCircle size={40} className="error-icon" />
              <p>{error}</p>
              <button onClick={() => fetchLogs()} className="btn-primary mt-4">Retry</button>
            </div>
          ) : logs.length === 0 && !loading ? (
            <div className="empty-state py-20 text-center">
              <Activity size={60} className="mx-auto mb-4 text-slate-700 opacity-20" />
              <h3 className="text-xl font-semibold mb-2">No logs yet</h3>
              <p className="text-slate-400">Your jobs haven't executed yet. Once they start running, details will appear here.</p>
            </div>
          ) : (
            <>
              <table className="custom-table overflow-hidden">
                <thead>
                  <tr>
                    <th>Execution Time</th>
                    <th>Job Name</th>
                    <th>Status</th>
                    <th>Duration</th>
                    <th>Attempt</th>
                    <th>Message</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    [...Array(5)].map((_, i) => (
                      <tr key={i} className="skeleton-row">
                        <td colSpan="7"></td>
                      </tr>
                    ))
                  ) : (
                    logs.map((log) => (
                      <tr key={log.id} className="hover:bg-slate-800/30 transition-colors">
                        <td className="time-cell">
                          <Clock size={14} className="text-slate-500" />
                          {formatDate(log.executedAt)}
                        </td>
                        <td>
                          <div className="font-semibold text-white">{log.jobName}</div>
                          <div className="text-[10px] text-slate-500 uppercase tracking-wider">{log.jobType}</div>
                        </td>
                        <td>{getStatusBadge(log.status)}</td>
                        <td className="text-slate-300">
                          {log.durationMs ? `${log.durationMs}ms` : '-'}
                        </td>
                        <td className="text-slate-400">
                          Attempt {log.attemptNumber}
                        </td>
                        <td className="max-w-[200px] truncate">
                          {log.errorMessage ? (
                            <span className="text-red-400/80 text-xs italic" title={log.errorMessage}>
                              {log.errorMessage}
                            </span>
                          ) : (
                            <span className="text-slate-500 text-xs">No errors</span>
                          )}
                        </td>
                        <td>
                          <Link to={`/jobs`} className="icon-btn" title="View Job Details">
                            <ExternalLink size={16} />
                          </Link>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>

              {totalPages > 1 && (
                <div className="pagination mt-8 pb-4">
                  <button 
                    disabled={page === 0 || loading} 
                    onClick={() => fetchLogs(page - 1)}
                    className="icon-btn px-4 w-auto"
                  >
                    Previous
                  </button>
                  <span className="text-sm font-medium">Page {page + 1} of {totalPages}</span>
                  <button 
                    disabled={page === totalPages - 1 || loading} 
                    onClick={() => fetchLogs(page + 1)}
                    className="icon-btn px-4 w-auto"
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default Logs;
