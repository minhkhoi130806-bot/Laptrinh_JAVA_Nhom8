'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { FeedbackProvider } from './Feedback';

// Menu bên trái, chia nhóm. Thêm trang admin mới chỉ cần thêm 1 dòng ở đây.
const NAV = [
  { items: [{ href: '/admin', icon: '📊', label: 'Tổng quan', exact: true }] },
  {
    title: 'Bán hàng', items: [
      { href: '/admin/orders', icon: '🧾', label: 'Đơn hàng' },
      { href: '/admin/reservations', icon: '📅', label: 'Đặt bàn' },
      { href: '/admin/invoices', icon: '💳', label: 'Hóa đơn' },
      { href: '/admin/customers', icon: '👥', label: 'Khách hàng' },
    ],
  },
  {
    title: 'Thực đơn', items: [
      { href: '/admin/menu', icon: '🍜', label: 'Món ăn' },
      { href: '/admin/categories', icon: '🗂️', label: 'Danh mục' },
    ],
  },
  {
    title: 'Kho', items: [
      { href: '/admin/ingredients', icon: '🥬', label: 'Nguyên liệu' },
      { href: '/admin/suppliers', icon: '🚚', label: 'Nhà cung cấp' },
      { href: '/admin/receipts', icon: '📥', label: 'Phiếu nhập' },
    ],
  },
  {
    title: 'Nhà hàng', items: [
      { href: '/admin/tables', icon: '🪑', label: 'Sơ đồ bàn' },
      { href: '/admin/employees', icon: '🧑‍🍳', label: 'Nhân viên' },
    ],
  },
  { title: 'Hệ thống', items: [{ href: '/admin/trash', icon: '🗑️', label: 'Thùng rác' }] },
];

const ROLE_ACCESS = {
  '/admin/employees': ['MANAGER'],
  '/admin/invoices': ['MANAGER', 'CASHIER'],
};
const ROLES = ['MANAGER', 'CASHIER', 'WAITER', 'CHEF'];

const canAccess = (path, role) => {
  const restrictedPath = Object.keys(ROLE_ACCESS).find(
    (route) => path === route || path.startsWith(`${route}/`),
  );
  return !restrictedPath || ROLE_ACCESS[restrictedPath].includes(role);
};

export default function AdminShell({ children }) {
  const path = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [user, setUser] = useState(null);
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    if (path === '/admin/employee-access') {
      const token = localStorage.getItem('token');
      const savedUser = localStorage.getItem('currentUser');
      let currentUser = null;

      try {
        currentUser = savedUser ? JSON.parse(savedUser) : null;
      } catch {
        localStorage.removeItem('currentUser');
      }

      if (!token || !ROLES.includes(currentUser?.role)) {
        // Keep the employee-access page available without an active admin session.
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setUser(null);
        setAuthReady(true);
        return;
      }

      // Restore the manager session for the public employee-access page.
      setUser(currentUser);
      setAuthReady(true);
      return;
    }

    const token = localStorage.getItem('token');
    const savedUser = localStorage.getItem('currentUser');
    let currentUser = null;

    if (savedUser) {
      try {
        currentUser = JSON.parse(savedUser);
      } catch {
        localStorage.removeItem('currentUser');
      }
    }

    if (!token || !ROLES.includes(currentUser?.role)) {
      localStorage.removeItem('token');
      localStorage.removeItem('currentUser');
      router.replace('/login');
      return;
    }
    // Restore the saved user after client hydration; localStorage is browser-only.
    // Restore the saved user after client hydration; localStorage is browser-only.
    setUser(currentUser);
    setAuthReady(true);
  }, [path, router]);

  useEffect(() => {
    const handleExpiredSession = () => {
      setUser(null);
      router.replace('/admin/employee-access');
    };
    window.addEventListener('auth:expired', handleExpiredSession);
    return () => window.removeEventListener('auth:expired', handleExpiredSession);
  }, [router]);

  useEffect(() => {
    if (authReady && user && !canAccess(path, user.role)) {
      router.replace('/admin');
    }
  }, [authReady, path, router, user]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    setUser(null);
    router.replace('/login');
  };

  const active = (i) => (i.exact ? path === i.href : path === i.href || path.startsWith(i.href + '/'));

  if (!authReady || !user || !canAccess(path, user.role)) {
    if (path === '/admin/employee-access' && authReady && !user) {
      return children;
    }
    return null;
  }

  return (
    <div className="adm-root">
      <FeedbackProvider>
        <div className="adm-shell">
          {open && <div className="adm-scrim" onClick={() => setOpen(false)} />}
          <aside className={`adm-side ${open ? 'open' : ''}`}>
            <Link href="/admin" className="adm-brand" onClick={() => setOpen(false)}>
              🍜 Nhà hàng Hàn<small>Trang quản trị · Nhóm 8</small>
            </Link>
            {NAV.map((g, gi) => {
              const items = g.items.filter((item) => canAccess(item.href, user.role));
              if (items.length === 0) return null;

              return (
              <div key={gi}>
                {g.title && <div className="adm-nav-title">{g.title}</div>}
                <nav className="adm-nav">
                  {items.map((i) => (
                    <Link key={i.href} href={i.href} className={active(i) ? 'on' : ''} onClick={() => setOpen(false)}>
                      <span>{i.icon}</span>{i.label}
                    </Link>
                  ))}
                </nav>
              </div>
              );
            })}
          </aside>
          <main className="adm-main">
            <div className="adm-topbar">
              <button className="adm-btn ghost sm adm-menu-toggle" onClick={() => setOpen(true)} aria-label="Mở menu">☰</button>
              <b>🍜 Quản trị nhà hàng</b>
              <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 12 }}>
                {user ? (
                  <>
                    <span className="adm-user-name" style={{ fontSize: 13, color: '#4b5563' }}>
                      👤 <b>{user.fullName || user.username}</b> ({user.role})
                    </span>
                    <button
                      className="adm-btn ghost sm"
                      onClick={handleLogout}
                      aria-label="Đăng xuất"
                      style={{ color: '#ef4444', fontSize: 13 }}
                    >
                      Đăng xuất
                    </button>
                  </>
                ) : (
                  <Link href="/login" className="adm-btn sm" style={{ textDecoration: 'none' }}>
                    Đăng nhập
                  </Link>
                )}
              </div>
            </div>
            <div className="adm-content">{children}</div>
          </main>
        </div>
      </FeedbackProvider>
    </div>
  );
}
