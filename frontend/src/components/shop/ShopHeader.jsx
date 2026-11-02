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
  const [hasStaffToken, setHasStaffToken] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);

  useEffect(() => {
    const refresh = () => {
      setFavoriteCount(readFavorites().length);
      setItemsInCart(cartCount(readCart()));
      try {
        setHasStaffToken(Boolean(localStorage.getItem('token')));
        const user = JSON.parse(localStorage.getItem('currentUser') || 'null');
        setCurrentUser(user);
      } catch {}
    };
    refresh();
    window.addEventListener('favorites:changed', refresh);
    window.addEventListener('cart:changed', refresh);
    window.addEventListener('storage', refresh);
    return () => {
      window.removeEventListener('favorites:changed', refresh);
      window.removeEventListener('cart:changed', refresh);
      window.removeEventListener('storage', refresh);
    };
  }, []);

  const links = [
    { href: '/', label: 'Trang chủ' },
    { href: '/thuc-don', label: 'Thực đơn' },
    { href: '/khuyen-mai', label: 'Ưu đãi' },
    { href: '/dat-ban', label: 'Đặt bàn' },
    { href: '/gioi-thieu', label: 'Về chúng tôi' },
    { href: '/lien-he', label: 'Liên hệ' },
    { href: '/don-hang', label: 'Đơn hàng' },
  ];

  return (
    <header className="shop-header">
      <div className="shop-header-inner">
        <Link className="shop-brand" href="/" aria-label="Han Seoul - Trang chủ">
          <span className="shop-brand-mark">한</span>
          <span>
            <b>HAN SEOUL</b>
            <small>KOREAN DINING</small>
          </span>
        </Link>
        <nav className="shop-nav" aria-label="Điều hướng chính">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={pathname === link.href ? 'active' : ''}
              aria-current={pathname === link.href ? 'page' : undefined}
            >
              {link.label}
            </Link>
          ))}
        </nav>
        <div className="shop-header-actions">
          {/* Nút chỉ hiển thị cho Quản trị viên/Nhân viên khi đã đăng nhập */}
          {hasStaffToken && (
            <Link
              href="/admin"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                background: '#173c32',
                color: '#e2c89b',
                padding: '6px 14px',
                borderRadius: '20px',
                fontSize: '12px',
                fontWeight: 650,
                textDecoration: 'none',
                border: '1px solid rgba(226,200,155,0.4)',
                boxShadow: '0 2px 8px rgba(23,60,50,0.2)',
              }}
              title="Vào Bảng điều khiển Quản trị viên"
            >
              <Icon name="shield" size={15} />
              <span>Quản trị</span>
            </Link>
          )}

          <Link className="shop-header-favorites" href="/yeu-thich" aria-label="Món yêu thích">
            <Icon name="heart" size={19} />
            {favoriteCount > 0 && <span>{favoriteCount}</span>}
          </Link>
          <Link className="shop-header-cart" href="/gio-hang" aria-label="Giỏ hàng">
            <Icon name="bag" size={19} />
            {itemsInCart > 0 && <span>{itemsInCart}</span>}
          </Link>

          {/* Hiển thị trạng thái đăng nhập hội viên rõ ràng trên Header */}
          {currentUser ? (
            <Link
              className="shop-header-account"
              href="/tai-khoan"
              aria-label="Tài khoản hội viên"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '4px 10px',
                background: '#fbf9f5',
                border: '1px solid #ebd9c5',
                borderRadius: '20px',
                textDecoration: 'none',
                color: '#8B2500',
                fontSize: '12.5px',
                fontWeight: 650,
              }}
              title={`Tài khoản: ${currentUser.fullName || currentUser.name} (${currentUser.loyaltyPoints || 0} điểm)`}
            >
              <span
                style={{
                  width: '22px',
                  height: '22px',
                  borderRadius: '50%',
                  background: '#8B2500',
                  color: '#ffffff',
                  display: 'grid',
                  placeItems: 'center',
                  fontSize: '11px',
                  fontWeight: 700,
                }}
              >
                {(currentUser.fullName || currentUser.name || 'H').slice(0, 1).toUpperCase()}
              </span>
              <span style={{ maxWidth: '90px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {currentUser.fullName || currentUser.name}
              </span>
            </Link>
          ) : (
            <Link className="shop-header-account" href="/tai-khoan" aria-label="Tài khoản">
              <Icon name="user" size={19} />
            </Link>
          )}

          <Link className="shop-header-cta" href="/dat-ban">
            <span>Đặt bàn ngay</span>
            <Icon name="arrowUpRight" size={17} />
          </Link>
          <button
            className="shop-mobile-menu"
            type="button"
            onClick={() => document.querySelector('.shop-nav')?.classList.toggle('mobile-open')}
            aria-label="Mở menu"
          >
            <Icon name="menu" size={21} />
          </button>
        </div>
      </div>
    </header>
  );
}
