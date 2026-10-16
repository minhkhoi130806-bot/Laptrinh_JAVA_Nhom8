import './globals.css';

export const metadata = { title: 'Quản lý nhà hàng – Nhóm 8', description: 'Frontend Next.js cho hệ thống quản lý nhà hàng' };

export default function RootLayout({ children }) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
