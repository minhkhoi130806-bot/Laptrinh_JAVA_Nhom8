'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { FeedbackProvider } from './Feedback';
import Icon from '@/components/ui/Icon';

// Menu bên trái, chia nhóm. Thêm trang admin mới chỉ cần thêm 1 dòng ở đây.
const NAV = [
  { items: [{ href: '/admin', icon: '📊', label: 'Tổng quan', exact: true }] },
  {
    title: 'Bán hàng', items: [
      { href: '/admin/orders', icon: '🧾', label: 'Đơn hàng' },
      { href: '/admin/kitchen', icon: '👨‍🍳', label: 'Màn hình bếp' },
      { href: '/admin/reservations', icon: '📅', label: 'Đặt bàn' },
      { href: '/admin/calendar', icon: '🗓️', label: 'Lịch đặt bàn' },
      { href: '/admin/waitlist', icon: '🕒', label: 'Danh sách chờ' },
      { href: '/admin/invoices', icon: '💳', label: 'Hóa đơn' },
      { href: '/admin/customers', icon: '👥', label: 'Khách hàng' },
      { href: '/admin/feedback', icon: '💬', label: 'Phản hồi & khiếu nại' },
      { href: '/admin/loyalty', icon: '🏅', label: 'Khách hàng thân thiết' },
      { href: '/admin/marketing', icon: '🎟️', label: 'Khuyến mãi' },
    ],
  },
  {
    title: 'Thực đơn', items: [
      { href: '/admin/menu', icon: '🍜', label: 'Món ăn' },
      { href: '/admin/categories', icon: '🗂️', label: 'Danh mục' },
      { href: '/admin/menu-planner', icon: '🍱', label: 'Điều phối thực đơn' },
    ],
  },
  {
    title: 'Kho', items: [
      { href: '/admin/ingredients', icon: '🥬', label: 'Nguyên liệu' },
      { href: '/admin/suppliers', icon: '🚚', label: 'Nhà cung cấp' },
      { href: '/admin/receipts', icon: '📥', label: 'Phiếu nhập' },
      { href: '/admin/reports', icon: '📈', label: 'Báo cáo kho & món' },
      { href: '/admin/analytics', icon: '📊', label: 'Phân tích tổng hợp' },
    ],
  },
  {
    title: 'Nhà hàng', items: [
      { href: '/admin/tables', icon: '🪑', label: 'Quản lý bàn' },
      { href: '/admin/floor', icon: '🗺️', label: 'Sơ đồ bàn nhanh' },
      { href: '/admin/employees', icon: '🧑‍🍳', label: 'Nhân viên' },
      { href: '/admin/shifts', icon: '🕐', label: 'Lịch ca làm' },
      { href: '/admin/cleaning', icon: '🧽', label: 'Checklist vệ sinh' },
      { href: '/admin/maintenance', icon: '🛠️', label: 'Bảo trì thiết bị' },
    ],
  },
  { title: 'Hệ thống', items: [{ href: '/admin/settings', icon: '⚙️', label: 'Cấu hình nhà hàng' }, { href: '/admin/trash', icon: '🗑️', label: 'Thùng rác' }] },
];

const ROLE_ACCESS = {
  '/admin/kitchen': ['MANAGER', 'CHEF'],
  '/admin/employees': ['MANAGER'],
  '/admin/invoices': ['MANAGER', 'CASHIER'],
  '/admin/analytics': ['MANAGER', 'CASHIER'],
  '/admin/marketing': ['MANAGER'],
  '/admin/shifts': ['MANAGER'],
  '/admin/settings': ['MANAGER'],
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
              <span className="adm-brand-mark" aria-hidden="true">한</span>
              <span><b>HAN SEOUL</b><small>Không gian quản trị</small></span>
            </Link>
            {NAV.map((g, gi) => {
              const items = g.items.filter((item) => canAccess(item.href, user.role));
              if (items.length === 0) return null;

              return (
              <div key={gi}>
                {g.title && <div className="adm-nav-title">{g.title}</div>}
                <nav className="adm-nav">
                  {items.map((i) => (
                    <Link key={i.href} href={i.href} className={active(i) ? 'on' : ''} aria-current={active(i) ? 'page' : undefined} onClick={() => setOpen(false)}>
                      <Icon name={i.icon} size={18} />{i.label}
                    </Link>
                  ))}
                </nav>
              </div>
              );
            })}
            <div className="adm-side-footer"><i aria-hidden="true" />HAN SEOUL · KOREAN DINING</div>
          </aside>
          <main className="adm-main">
            <div className="adm-topbar">
              <button className="adm-btn ghost sm adm-menu-toggle" onClick={() => setOpen(true)} aria-label="Mở menu"><Icon name="menu" size={19} /></button>
              <div className="adm-topbar-title"><b>Quản trị nhà hàng</b><small>Han Seoul / Không gian làm việc</small></div>
              <div className="adm-user">
                {user ? (
                  <>
                    <span className="adm-avatar"><Icon name="user" size={17} /></span>
                    <span className="adm-user-name"><b>{user.fullName || user.username}</b><small>{user.role}</small></span>
                    <button
                      className="adm-btn ghost sm adm-logout"
                      onClick={handleLogout}
                      aria-label="Đăng xuất"
                    >
                      <Icon name="logout" size={15} /><span>Đăng xuất</span>
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
