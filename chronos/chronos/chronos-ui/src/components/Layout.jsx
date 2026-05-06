import Sidebar from './Sidebar';

const Layout = ({ children }) => {
  return (
    <div className="layout-container">
      <Sidebar />
      <main className="main-content fade-in">
        <div className="content-inner">
          {children}
        </div>
      </main>

    </div>
  );
};

export default Layout;
