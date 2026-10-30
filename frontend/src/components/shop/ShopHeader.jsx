'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { readFavorites } from './FavoriteButton';
import { readCart, cartCount } from '@/lib/cart';
import Icon from '@/components/ui/Icon';
export default function ShopHeader() {
  const pathname = usePathname();
  const [favoriteCount, setFavoriteCount] = useState(0);
  const [itemsInCart, setItemsInCart] = useState(0);
  useEffect(() => {
    const refresh = () => { setFavoriteCount(readFavorites().length); setItemsInCart(cartCount(readCart())); };
    refresh(); window.addEventListener('favorites:changed', refresh); window.addEventListener('cart:changed', refresh); window.addEventListener('storage', refresh);
    return () => { window.removeEventListener('favorites:changed', refresh); window.removeEventListener('cart:changed', refresh); window.removeEventListener('storage', refresh); };
  }, []);
  const links = [
    { href: '/', label: 'Trang chủ' }, { href: '/thuc-don', label: 'Thực đơn' },
    { href: '/khuyen-mai', label: 'Ưu đãi' }, { href: '/dat-ban', label: 'Đặt bàn' },
    { href: '/gioi-thieu', label: 'Về chúng tôi' }, { href: '/lien-he', label: 'Liên hệ' }, { href: '/don-hang', label: 'Đơn hàng' },
  ];
  return <header className="shop-header"><div className="shop-header-inner">
    <Link className="shop-brand" href="/" aria-label="Han Seoul - Trang chủ"><span className="shop-brand-mark">한</span><span><b>HAN SEOUL</b><small>KOREAN DINING</small></span></Link>
    <nav className="shop-nav" aria-label="Điều hướng chính">{links.map((link) => <Link key={link.href} href={link.href} className={pathname === link.href ? 'active' : ''} aria-current={pathname === link.href ? 'page' : undefined}>{link.label}</Link>)}</nav>
    <div className="shop-header-actions"><Link className="shop-header-favorites" href="/yeu-thich" aria-label="Món yêu thích"><Icon name="heart" size={19}/>{favoriteCount > 0 && <span>{favoriteCount}</span>}</Link><Link className="shop-header-cart" href="/gio-hang" aria-label="Giỏ hàng"><Icon name="bag" size={19}/>{itemsInCart > 0 && <span>{itemsInCart}</span>}</Link><Link className="shop-header-account" href="/tai-khoan" aria-label="Tài khoản"><Icon name="user" size={19}/></Link><Link className="shop-header-cta" href="/dat-ban"><span>Đặt bàn ngay</span><Icon name="arrowUpRight" size={17}/></Link><button className="shop-mobile-menu" type="button" onClick={() => document.querySelector('.shop-nav')?.classList.toggle('mobile-open')} aria-label="Mở menu"><Icon name="menu" size={21}/></button></div>
  </div></header>;
}
