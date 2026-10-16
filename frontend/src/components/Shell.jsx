'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

const NAV = [
  ['/admin', '📊', 'Tổng quan'], ['/admin/tables', '🪑', 'Sơ đồ bàn'], ['/admin/reservations', '📅', 'Đặt bàn'],
  ['/admin/orders', '🧾', 'Đơn hàng'], ['/admin/order-items', '🍽️', 'Chi tiết đơn'], ['/admin/invoices', '💳', 'Hóa đơn'],
  ['/admin/customers', '👥', 'Khách hàng'], ['/admin/employees', '🧑‍🍳', 'Nhân viên'],
];

export default function Shell({ children }) {
  const path = usePathname();
  return (
    <div className="shell">
      <aside className="side">
        <div className="brand">🍜 Nhà hàng<small>Nhóm 8</small></div>
        <nav>
          {NAV.map(([href, icon, label]) => (
            <Link key={href} href={href} className={(href === '/admin' ? path === '/admin' : path.startsWith(href)) ? 'on' : ''}>
              <span>{icon}</span>{label}
            </Link>
          ))}
        </nav>
      </aside>
      <main className="main">{children}</main>
    </div>
  );
}
