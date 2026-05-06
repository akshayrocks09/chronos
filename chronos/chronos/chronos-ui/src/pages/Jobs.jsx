import { useState, useEffect, useRef } from 'react';
import { toast } from 'react-hot-toast';
import Layout from '../components/Layout';
import Modal from '../components/Modal';
import api from '../api/client';
import { 
  Plus, Search, Filter, Play, Trash2, FileText,
  Clock, Calendar, AlertCircle, Pencil, RefreshCcw,
  Mail, Globe, MessageSquare, Code, XCircle
} from 'lucide-react';

const POLL_INTERVAL_MS = 10000;

const ActionFieldsForm = ({ type, fields, setFields }) => {
  if (type === 'LOG_MESSAGE') {
    return (
      <div className="input-group">
        <label>Log Message</label>
        <input type="text" value={fields.message} onChange={e=>setFields({...fields, message: e.target.value})} placeholder="Message to write to logs..."/>
      </div>
    );
  }
  if (type === 'SEND_EMAIL') {
    return (
      <>
        <div className="input-group">
          <label>Recipient Email</label>
          <input type="email" required value={fields.to} onChange={e=>setFields({...fields, to: e.target.value})} placeholder="user@example.com"/>
        </div>
        <div className="input-group">
          <label>Subject</label>
          <input type="text" required value={fields.subject} onChange={e=>setFields({...fields, subject: e.target.value})} placeholder="Notification Subject"/>
        </div>
        <div className="input-group">
          <label>Email Body</label>
          <textarea rows="2" required value={fields.body} onChange={e=>setFields({...fields, body: e.target.value})} placeholder="Write your email content here..."></textarea>
        </div>
      </>
    );
  }
  if (type === 'HTTP_REQUEST') {
    return (
      <>
        <div className="flex gap-4">
          <div className="input-group flex-1">
            <label>Method</label>
            <select value={fields.method} onChange={e=>setFields({...fields, method: e.target.value})}>
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
            </select>
          </div>
          <div className="input-group flex-[3]">
            <label>URL</label>
            <input type="url" required value={fields.url} onChange={e=>setFields({...fields, url: e.target.value})} placeholder="https://api.example.com/endpoint"/>
          </div>
        </div>
        {(fields.method === 'POST' || fields.method === 'PUT') && (
          <div className="input-group">
            <label>Request Body (JSON)</label>
            <textarea rows="2" value={fields.body} onChange={e=>setFields({...fields, body: e.target.value})} placeholder='{"key": "value"}'></textarea>
          </div>
        )}
      </>
    );
  }
  return (
    <div className="input-group">
      <label>Custom Payload (JSON)</label>
      <textarea rows="4" required value={fields.customJson} onChange={e=>setFields({...fields, customJson: e.target.value})} placeholder='{"type": "CUSTOM_ACTION", ...}'></textarea>
    </div>
  );
};

const Jobs = () => {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState('');
  const [lastRefreshed, setLastRefreshed] = useState(null);
  const pollRef = useRef(null);

  useEffect(() => {
    const handler = setTimeout(() => {
      if (debouncedSearchQuery !== searchQuery) {
        setPage(0);
        setDebouncedSearchQuery(searchQuery);
      }
    }, 400);
    return () => clearTimeout(handler);
  }, [searchQuery, debouncedSearchQuery]);

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isLogsOpen, setIsLogsOpen] = useState(false);
  const [selectedLogs, setSelectedLogs] = useState([]);
  const [logsLoading, setLogsLoading] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [jobToDelete, setJobToDelete] = useState(null);
  const [isCancelOpen, setIsCancelOpen] = useState(false);
  const [jobToCancel, setJobToCancel] = useState(null);
  const [jobToEdit, setJobToEdit] = useState(null);
  const [formError, setFormError] = useState('');

  const emptyForm = { name: '', description: '', jobType: 'ONE_TIME', cronExpression: '', scheduledAt: '', payload: '', maxRetries: 3 };
  const [formData, setFormData] = useState(emptyForm);
  const [editData, setEditData] = useState({ name: '', description: '', cronExpression: '', scheduledAt: '', payload: '', maxRetries: 3 });

  // New states for simplified form
  const [createActionType, setCreateActionType] = useState('LOG_MESSAGE');
  const [createActionFields, setCreateActionFields] = useState({ url: '', method: 'GET', to: '', subject: '', body: '', message: '', customJson: '' });
  
  const [editActionType, setEditActionType] = useState('LOG_MESSAGE');
  const [editActionFields, setEditActionFields] = useState({ url: '', method: 'GET', to: '', subject: '', body: '', message: '', customJson: '' });

  const fetchJobs = async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      const response = await api.get('/jobs', { params: { page, size: 10, status: statusFilter || undefined, search: debouncedSearchQuery || undefined } });
      setJobs(response.data.data?.content || []);
      setTotalPages(response.data.data?.totalPages || 0);
      setLastRefreshed(new Date());
    } catch (err) {
      console.error('Failed to fetch jobs', err);
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => { fetchJobs(); }, [page, statusFilter, debouncedSearchQuery]);

  useEffect(() => {
    pollRef.current = setInterval(() => fetchJobs(true), POLL_INTERVAL_MS);
    return () => clearInterval(pollRef.current);
  }, [page, statusFilter, debouncedSearchQuery]);

  const composePayload = (type, fields) => {
    if (type === 'CUSTOM') return fields.customJson;
    if (type === 'LOG_MESSAGE') return JSON.stringify({ type: 'LOG_MESSAGE', message: fields.message || 'Job executed' });
    if (type === 'SEND_EMAIL') return JSON.stringify({ type: 'SEND_EMAIL', to: fields.to, subject: fields.subject, body: fields.body });
    if (type === 'HTTP_REQUEST') {
      const payload = { type: 'HTTP_REQUEST', url: fields.url, method: fields.method };
      if (fields.body && (fields.method === 'POST' || fields.method === 'PUT')) {
        try { payload.body = JSON.parse(fields.body); } catch { payload.body = fields.body; }
      }
      return JSON.stringify(payload);
    }
    return '';
  };

  const decomposePayload = (payload) => {
    if (!payload) return { type: 'LOG_MESSAGE', fields: { message: '' } };
    try {
      const json = JSON.parse(payload);
      const type = json.type || 'CUSTOM';
      if (type === 'LOG_MESSAGE') return { type, fields: { message: json.message || '' } };
      if (type === 'SEND_EMAIL') return { type, fields: { to: json.to || '', subject: json.subject || '', body: json.body || '' } };
      if (type === 'HTTP_REQUEST') return { type, fields: { url: json.url || '', method: json.method || 'GET', body: json.body ? JSON.stringify(json.body, null, 2) : '' } };
      return { type: 'CUSTOM', fields: { customJson: payload } };
    } catch {
      return { type: 'CUSTOM', fields: { customJson: payload } };
    }
  };

  const handleCreateJob = async (e) => {
    e.preventDefault();
    setFormError('');
    try {
      const payload = composePayload(createActionType, createActionFields);
      if (createActionType === 'CUSTOM') {
        try { JSON.parse(payload); } catch { setFormError('Custom Payload must be valid JSON'); return; }
      }
      
      const requestData = { ...formData, payload };
      if (!requestData.scheduledAt) delete requestData.scheduledAt;
      if (requestData.jobType !== 'RECURRING') delete requestData.cronExpression;
      
      await api.post('/jobs', requestData);
      toast.success('Task created successfully');
      setIsCreateOpen(false);
      setFormData(emptyForm);
      setCreateActionFields({ url: '', method: 'GET', to: '', subject: '', body: '', message: '', customJson: '' });
      fetchJobs();
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to create job');
    }
  };

  const openEdit = (job) => {
    setJobToEdit(job);
    setFormError('');
    
    const { type, fields } = decomposePayload(job.payload);
    setEditActionType(type);
    setEditActionFields(prev => ({ ...prev, ...fields }));

    setEditData({
      name: job.name || '',
      description: job.description || '',
      cronExpression: job.cronExpression || '',
      scheduledAt: job.scheduledAt ? toDatetimeLocal(job.scheduledAt) : '',
      payload: job.payload || '',
      maxRetries: job.maxRetries ?? 3
    });
    setIsEditOpen(true);
  };

  const handleEditJob = async (e) => {
    e.preventDefault();
    setFormError('');
    try {
      const payload = composePayload(editActionType, editActionFields);
      if (editActionType === 'CUSTOM') {
        try { JSON.parse(payload); } catch { setFormError('Custom Payload must be valid JSON'); return; }
      }

      const body = { ...editData, payload };
      if (!body.scheduledAt) delete body.scheduledAt;
      if (jobToEdit?.jobType !== 'RECURRING') delete body.cronExpression;
      
      await api.put(`/jobs/${jobToEdit.id}`, body);
      toast.success('Task updated successfully');
      setIsEditOpen(false);
      setJobToEdit(null);
      fetchJobs();
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to update job');
    }
  };

  const handleTrigger = async (id) => {
    try { 
      await api.post(`/jobs/${id}/trigger`); 
      toast.success('Job triggered successfully');
      fetchJobs(); 
    }
    catch (err) { 
      toast.error('Failed to trigger job: ' + (err.response?.data?.message || 'Error')); 
    }
  };

  const handleCancel = async () => {
    if (!jobToCancel) return;
    try { 
      await api.patch(`/jobs/${jobToCancel.id}/cancel`); 
      toast.success('Job cancelled successfully');
      setIsCancelOpen(false); setJobToCancel(null); fetchJobs(); 
    }
    catch (err) { 
      toast.error('Failed to cancel job: ' + (err.response?.data?.message || 'Error')); 
    }
  };

  const openCancelConfirm = (job) => { setJobToCancel(job); setIsCancelOpen(true); };
  const openDeleteConfirm = (job) => { setJobToDelete(job); setIsDeleteOpen(true); };

  const handleDelete = async () => {
    if (!jobToDelete) return;
    try {
      await api.delete(`/jobs/${jobToDelete.id}`);
      toast.success('Job deleted successfully');
      setIsDeleteOpen(false); setJobToDelete(null); fetchJobs();
    } catch { 
      toast.error('Failed to delete job'); 
    }
  };

  const viewLogs = async (jobId) => {
    setIsLogsOpen(true); setLogsLoading(true);
    try { const r = await api.get(`/jobs/${jobId}/logs`); setSelectedLogs(r.data.data); }
    catch (err) { console.error('Failed to fetch logs', err); }
    finally { setLogsLoading(false); }
  };

  const formatDate = (dateVal) => {
    if (!dateVal) return 'N/A';
    try {
      if (Array.isArray(dateVal)) { const [y,m,d,h,min,s]=dateVal; return new Date(y,m-1,d,h||0,min||0,s||0).toLocaleString(); }
      return new Date(dateVal).toLocaleString();
    } catch { return 'Invalid Date'; }
  };

  const toDatetimeLocal = (val) => {
    if (!val) return '';
    try {
      if (Array.isArray(val)) { const [y,mo,d,h=0,mi=0]=val; return `${y}-${String(mo).padStart(2,'0')}-${String(d).padStart(2,'0')}T${String(h).padStart(2,'0')}:${String(mi).padStart(2,'0')}`; }
      return new Date(val).toISOString().slice(0,16);
    } catch { return ''; }
  };

  const canEdit = (job) => job.status !== 'RUNNING' && job.status !== 'RETRYING';
  const canCancel = (job) => !['CANCELLED','COMPLETED'].includes(job.status);

  const StatusBadge = ({ status }) => {
    const config = {
      PENDING:{color:'blue',label:'Pending'}, RUNNING:{color:'orange',label:'Running'},
      COMPLETED:{color:'green',label:'Completed'}, FAILED:{color:'red',label:'Failed'},
      RETRYING:{color:'purple',label:'Retrying'}, CANCELLED:{color:'gray',label:'Cancelled'},
      SCHEDULED:{color:'teal',label:'Scheduled'},
    };
    const {color,label} = config[status]||config.PENDING;
    return <span className={`badge ${color}`}>{label}</span>;
  };

  return (
    <Layout>
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="page-title">Schedules</h1>
          <p className="page-subtitle">
            Manage and monitor all your automation tasks
            {lastRefreshed && <span style={{marginLeft:12,fontSize:12,opacity:0.5}}>· refreshed {lastRefreshed.toLocaleTimeString()}</span>}
          </p>
        </div>
        <div className="flex gap-3">
          <button onClick={() => fetchJobs()} className="btn-secondary" title="Refresh now"><RefreshCcw size={18}/></button>
          <button onClick={() => { 
            setFormData(emptyForm); 
            setFormError(''); 
            setCreateActionType('LOG_MESSAGE');
            setCreateActionFields({ url: '', method: 'GET', to: '', subject: '', body: '', message: '', customJson: '' });
            setIsCreateOpen(true); 
          }} className="btn-primary flex items-center gap-2">
            <Plus size={20}/> Create New Task
          </button>
        </div>
      </div>

      <div className="glass-card table-container">
        <div className="table-header mb-6 flex justify-between items-center">
          <div className="search-box">
            <Search size={18} className="search-icon"/>
            <input type="text" placeholder="Search tasks..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
          </div>
          <div className="flex gap-4">
            <div className="filter-select">
              <Filter size={16}/>
              <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}>
                <option value="">All Statuses</option>
                <option value="PENDING">Pending</option>
                <option value="SCHEDULED">Scheduled</option>
                <option value="RUNNING">Running</option>
                <option value="COMPLETED">Completed</option>
                <option value="FAILED">Failed</option>
                <option value="RETRYING">Retrying</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>
          </div>
        </div>

        <div className="table-responsive">
          <table className="custom-table">
            <thead>
              <tr><th>Task Name</th><th>Type</th><th>Status</th><th>Next Run</th><th>Last Run</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {loading ? (
                [...Array(5)].map((_,i) => <tr key={i} className="skeleton-row"><td colSpan="6"></td></tr>)
              ) : !jobs||jobs.length===0 ? (
                <tr><td colSpan="6" className="empty-state"><AlertCircle size={40}/><p>No tasks found. Create your first job to get started!</p></td></tr>
              ) : (
                jobs.map((job) => (
                  <tr key={job.id} className="fade-in">
                    <td><div className="job-name-cell"><span className="job-name">{job.name}</span><span className="job-desc">{job.description||'No description'}</span></div></td>
                    <td><span className="type-tag">{job.jobType}</span></td>
                    <td><StatusBadge status={job.status}/></td>
                    <td><div className="time-cell"><Clock size={14}/>{formatDate(job.nextFireAt)}</div></td>
                    <td><div className="time-cell"><Calendar size={14}/>{formatDate(job.lastExecutedAt)}</div></td>
                    <td>
                      <div className="action-buttons">
                        <button onClick={() => handleTrigger(job.id)} className="icon-btn trigger" title="Trigger Now"><Play size={16}/></button>
                        <button onClick={() => openEdit(job)} className="icon-btn" title={canEdit(job)?'Edit Job':'Cannot edit — '+job.status} disabled={!canEdit(job)}><Pencil size={16}/></button>
                        <button onClick={() => openCancelConfirm(job)} className="icon-btn" title={canCancel(job)?'Cancel Job':'Cannot cancel — '+job.status} disabled={!canCancel(job)}><XCircle size={16}/></button>
                        <button onClick={() => viewLogs(job.id)} className="icon-btn" title="View Logs"><FileText size={16}/></button>
                        <button onClick={() => openDeleteConfirm(job)} className="icon-btn delete" title="Delete Job"><Trash2 size={16}/></button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {totalPages>1 && (
          <div className="pagination">
            <button onClick={() => setPage(p=>Math.max(0,p-1))} disabled={page===0}>Previous</button>
            <span>Page {page+1} of {totalPages}</span>
            <button onClick={() => setPage(p=>Math.min(totalPages-1,p+1))} disabled={page===totalPages-1}>Next</button>
          </div>
        )}
      </div>

      {/* CREATE MODAL */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create New Automation Task">
        <form onSubmit={handleCreateJob} className="create-form">
          {formError && <div className="error-message mb-4">{formError}</div>}
          
          <div className="form-section">
            <h3 className="section-title">General Info</h3>
            <div className="input-group">
              <label>Task Name</label>
              <input type="text" required value={formData.name} onChange={e=>setFormData({...formData,name:e.target.value})} placeholder="e.g. Daily Data Sync"/>
            </div>
            <div className="input-group">
              <label>Description</label>
              <input type="text" value={formData.description} onChange={e=>setFormData({...formData,description:e.target.value})} placeholder="What does this job do?"/>
            </div>
          </div>

          <div className="form-section">
            <h3 className="section-title">Schedule Settings</h3>
            <div className="flex gap-4">
              <div className="input-group flex-1">
                <label>Task Type</label>
                <select value={formData.jobType} onChange={e=>setFormData({...formData,jobType:e.target.value})}>
                  <option value="ONE_TIME">One Time</option>
                  <option value="RECURRING">Recurring (Cron)</option>
                </select>
              </div>
              <div className="input-group flex-1">
                <label>Max Retries</label>
                <input type="number" min="0" max="10" value={formData.maxRetries} onChange={e=>setFormData({...formData,maxRetries:parseInt(e.target.value)})}/>
              </div>
            </div>
            {formData.jobType==='RECURRING' ? (
              <div className="input-group">
                <label>Cron Expression</label>
                <input type="text" required value={formData.cronExpression} onChange={e=>setFormData({...formData,cronExpression:e.target.value})} placeholder="0 0 * * * ?"/>
                <span className="help-text">Quartz cron — e.g. "0 0 12 * * ?" for 12pm daily</span>
              </div>
            ) : (
              <div className="input-group">
                <label>Schedule Time (Optional)</label>
                <input type="datetime-local" value={formData.scheduledAt} onChange={e=>setFormData({...formData,scheduledAt:e.target.value})}/>
                <span className="help-text">Leave empty to run immediately</span>
              </div>
            )}
          </div>

          <div className="form-section">
            <h3 className="section-title">Job Action</h3>
            <div className="action-type-selector mb-4">
              <div 
                className={`action-option ${createActionType === 'LOG_MESSAGE' ? 'active' : ''}`}
                onClick={() => setCreateActionType('LOG_MESSAGE')}
              >
                <MessageSquare size={18}/>
                <span>Log</span>
              </div>
              <div 
                className={`action-option ${createActionType === 'HTTP_REQUEST' ? 'active' : ''}`}
                onClick={() => setCreateActionType('HTTP_REQUEST')}
              >
                <Globe size={18}/>
                <span>HTTP</span>
              </div>
              <div 
                className={`action-option ${createActionType === 'SEND_EMAIL' ? 'active' : ''}`}
                onClick={() => setCreateActionType('SEND_EMAIL')}
              >
                <Mail size={18}/>
                <span>Email</span>
              </div>
              <div 
                className={`action-option ${createActionType === 'CUSTOM' ? 'active' : ''}`}
                onClick={() => setCreateActionType('CUSTOM')}
              >
                <Code size={18}/>
                <span>JSON</span>
              </div>
            </div>

            <ActionFieldsForm 
              type={createActionType} 
              fields={createActionFields} 
              setFields={setCreateActionFields} 
            />
          </div>

          <div className="modal-actions mt-8">
            <button type="button" onClick={() => setIsCreateOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">Create Task</button>
          </div>
        </form>
      </Modal>

      {/* EDIT MODAL */}
      <Modal isOpen={isEditOpen} onClose={() => setIsEditOpen(false)} title={`Edit — ${jobToEdit?.name}`}>
        <form onSubmit={handleEditJob} className="create-form">
          {formError && <div className="error-message mb-4">{formError}</div>}
          
          <div className="form-section">
            <h3 className="section-title">General Info</h3>
            <div className="input-group">
              <label>Task Name</label>
              <input type="text" required value={editData.name} onChange={e=>setEditData({...editData,name:e.target.value})}/>
            </div>
            <div className="input-group">
              <label>Description</label>
              <input type="text" value={editData.description} onChange={e=>setEditData({...editData,description:e.target.value})}/>
            </div>
          </div>

          <div className="form-section">
            <h3 className="section-title">Schedule Settings</h3>
            <div className="input-group">
              <label>Max Retries</label>
              <input type="number" min="0" max="10" value={editData.maxRetries} onChange={e=>setEditData({...editData,maxRetries:parseInt(e.target.value)})}/>
            </div>
            {jobToEdit?.jobType==='RECURRING' ? (
              <div className="input-group">
                <label>Cron Expression</label>
                <input type="text" value={editData.cronExpression} onChange={e=>setEditData({...editData,cronExpression:e.target.value})} placeholder="0 0 * * * ?"/>
              </div>
            ) : (
              <div className="input-group">
                <label>Schedule Time</label>
                <input type="datetime-local" value={editData.scheduledAt} onChange={e=>setEditData({...editData,scheduledAt:e.target.value})}/>
              </div>
            )}
          </div>

          <div className="form-section">
            <h3 className="section-title">Job Action</h3>
            <div className="action-type-selector mb-4">
              <div 
                className={`action-option ${editActionType === 'LOG_MESSAGE' ? 'active' : ''}`}
                onClick={() => setEditActionType('LOG_MESSAGE')}
              >
                <MessageSquare size={18}/>
                <span>Log</span>
              </div>
              <div 
                className={`action-option ${editActionType === 'HTTP_REQUEST' ? 'active' : ''}`}
                onClick={() => setEditActionType('HTTP_REQUEST')}
              >
                <Globe size={18}/>
                <span>HTTP</span>
              </div>
              <div 
                className={`action-option ${editActionType === 'SEND_EMAIL' ? 'active' : ''}`}
                onClick={() => setEditActionType('SEND_EMAIL')}
              >
                <Mail size={18}/>
                <span>Email</span>
              </div>
              <div 
                className={`action-option ${editActionType === 'CUSTOM' ? 'active' : ''}`}
                onClick={() => setEditActionType('CUSTOM')}
              >
                <Code size={18}/>
                <span>JSON</span>
              </div>
            </div>

            <ActionFieldsForm 
              type={editActionType} 
              fields={editActionFields} 
              setFields={setEditActionFields} 
            />
          </div>

          <div className="modal-actions mt-8">
            <button type="button" onClick={() => setIsEditOpen(false)} className="btn-secondary">Cancel</button>
            <button type="submit" className="btn-primary">Save Changes</button>
          </div>
        </form>
      </Modal>

      {/* LOGS MODAL */}
      <Modal isOpen={isLogsOpen} onClose={() => setIsLogsOpen(false)} title="Execution History">
        {logsLoading ? (
          <div className="flex justify-center p-8"><span className="spinner"></span></div>
        ) : (!selectedLogs||selectedLogs.length===0) ? (
          <div className="text-center p-8 color-dim">No execution records found for this task.</div>
        ) : (
          <div className="logs-list">
            {selectedLogs.map(log => (
              <div key={log.id} className={`log-item ${log.status==='COMPLETED'?'success':'failure'}`}>
                <div className="log-dot"></div>
                <div className="log-main">
                  <div className="flex justify-between">
                    <span className="log-time">{formatDate(log.executedAt)}</span>
                    <span className={`log-status-tag ${(log.status||'PENDING').toLowerCase()}`}>{log.status}</span>
                  </div>
                  {log.errorMessage && <p className="log-error">{log.errorMessage}</p>}
                  <div className="log-footer">
                    <span>ID: #{log.id}</span>
                    <span>Duration: {log.durationMs||0}ms</span>
                    <span>Attempt: {log.attemptNumber}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </Modal>

      {/* DELETE MODAL */}
      <Modal isOpen={isDeleteOpen} onClose={() => setIsDeleteOpen(false)} title="Delete Job permanently?">
        <div className="p-4">
          <p className="mb-6 color-dim">
            Are you sure you want to delete <span className="highlight">"{jobToDelete?.name}"</span>?
            This will permanently remove the job and all its execution logs. This cannot be undone.
          </p>
          <div className="modal-actions">
            <button onClick={() => setIsDeleteOpen(false)} className="btn-secondary">Cancel</button>
            <button onClick={handleDelete} className="btn-danger">Yes, Delete Job</button>
          </div>
        </div>
      </Modal>

      {/* CANCEL MODAL */}
      <Modal isOpen={isCancelOpen} onClose={() => setIsCancelOpen(false)} title="Cancel Job Execution?">
        <div className="p-4">
          <p className="mb-6 color-dim">
            Are you sure you want to cancel <span className="highlight">"{jobToCancel?.name}"</span>?
            It will no longer execute as scheduled.
          </p>
          <div className="modal-actions">
            <button onClick={() => setIsCancelOpen(false)} className="btn-secondary">Go Back</button>
            <button onClick={handleCancel} className="btn-primary">Yes, Cancel Job</button>
          </div>
        </div>
      </Modal>
    </Layout>
  );
};

export default Jobs;
