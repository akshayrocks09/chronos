import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  LayoutDashboard, 
  CalendarClock, 
  LogOut, 
  ShieldCheck,
  ChevronRight,
  Activity
} from 'lucide-react';

const Sidebar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isAdmin = user?.role === 'ROLE_ADMIN';

  const navGroups = [
    {
      title: 'Overview',
      items: [
        { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
      ]
    },
    {
      title: 'Personal',
      items: [
        { name: 'My Jobs', path: '/jobs', icon: CalendarClock },
        { name: 'My Activity', path: '/logs', icon: Activity },
      ]
    },
    ...(isAdmin ? [
      {
        title: 'System Admin',
        items: [
          { name: 'System Logs', path: '/admin/logs', icon: Activity },
        ]
      }
    ] : [])
  ];

  return (
    <aside className="sidebar glass">
      <div className="sidebar-header">
        <div className="logo">
          <ShieldCheck size={28} className="primary-icon" />
          <span>Chronos</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        {navGroups.map((group, gIdx) => (
          <div key={gIdx} className="nav-group">
            <div className="nav-group-title">{group.title}</div>
            {group.items.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.path;
              
              return (
                <Link 
                  key={item.path} 
                  to={item.path} 
                  className={`nav-item ${isActive ? 'active' : ''}`}
                >
                  <Icon size={22} />
                  <span>{item.name}</span>
                  {isActive && <ChevronRight size={16} className="active-arrow" />}
                </Link>
              );
            })}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="user-profile">
          <div className="avatar">
            {user?.username?.[0]?.toUpperCase()}
          </div>
          <div className="user-info">
            <span className="username">{user?.username}</span>
            <span className="role">{user?.role?.replace('ROLE_', '')}</span>
          </div>
        </div>
        
        <button onClick={handleLogout} className="logout-btn">
          <LogOut size={20} />
          <span>Sign Out</span>
        </button>
      </div>

    </aside>
  );
};

export default Sidebar;
