import './globals.css';
import './shop.css';

export const metadata = { title: 'Han Seoul | Korean Dining', description: 'Khám phá hương vị Hàn Quốc và đặt bàn tại Han Seoul.' };

export default function RootLayout({ children }) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
