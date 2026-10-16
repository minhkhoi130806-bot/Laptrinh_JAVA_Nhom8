import './admin.css';
import AdminShell from '@/components/admin/AdminShell';

export const metadata = { title: 'Quản trị – Nhà hàng Nhóm 8' };

// Mọi trang trong /admin đều nằm trong khung này (menu trái + CSS riêng của admin)
export default function AdminLayout({ children }) {
  return <AdminShell>{children}</AdminShell>;
}
