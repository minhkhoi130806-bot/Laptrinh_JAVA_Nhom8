'use client';
import { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
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

export default function AdminShell({ children }) {
  const path = usePathname();
  const [open, setOpen] = useState(false);
  const active = (i) => (i.exact ? path === i.href : path === i.href || path.startsWith(i.href + '/'));

  return (
    <div className="adm-root">
      <FeedbackProvider>
        <div className="adm-shell">
          {open && <div className="adm-scrim" onClick={() => setOpen(false)} />}
          <aside className={`adm-side ${open ? 'open' : ''}`}>
            <Link href="/admin" className="adm-brand" onClick={() => setOpen(false)}>
              🍜 Nhà hàng<small>Khu quản trị · Nhóm 8</small>
            </Link>
            {NAV.map((g, gi) => (
              <div key={gi}>
                {g.title && <div className="adm-nav-title">{g.title}</div>}
                <nav className="adm-nav">
                  {g.items.map((i) => (
                    <Link key={i.href} href={i.href} className={active(i) ? 'on' : ''} onClick={() => setOpen(false)}>
                      <span>{i.icon}</span>{i.label}
                    </Link>
                  ))}
                </nav>
              </div>
            ))}
          </aside>
          <main className="adm-main">
            <div className="adm-topbar">
              <button className="adm-btn ghost sm" onClick={() => setOpen(true)} aria-label="Mở menu">☰</button>
              <b>🍜 Quản trị nhà hàng</b>
            </div>
            <div className="adm-content">{children}</div>
          </main>
        </div>
      </FeedbackProvider>
    </div>
  );
}
