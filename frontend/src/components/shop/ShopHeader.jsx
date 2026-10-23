'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

export default function ShopHeader() {
  const pathname = usePathname();
  const links = [
    { href: '/', label: 'Trang chủ' },
    { href: '/thuc-don', label: 'Thực đơn' },
    { href: '/dat-ban', label: 'Đặt bàn' },
  ];
  return (
    <header className="shop-header">
      <div className="shop-header-inner">
        <Link className="shop-brand" href="/" aria-label="Han Seoul - Trang chủ">
          <span className="shop-brand-mark">한</span>
          <span><b>HAN SEOUL</b><small>KOREAN DINING</small></span>
        </Link>
        <nav className="shop-nav" aria-label="Điều hướng chính">
          {links.map((link) => <Link key={link.href} href={link.href} className={pathname === link.href ? 'active' : ''}>{link.label}</Link>)}
        </nav>
        <Link className="shop-header-cta" href="/dat-ban"><span>Đặt bàn ngay</span><span aria-hidden="true">↗</span></Link>
      </div>
    </header>
  );
}
