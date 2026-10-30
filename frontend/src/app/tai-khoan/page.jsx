'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import './account.css';
export default function AccountPage() {
 const [user, setUser] = useState(null); const [token, setToken] = useState(false);
 useEffect(() => { try { setUser(JSON.parse(localStorage.getItem('currentUser') || 'null')); setToken(Boolean(localStorage.getItem('token'))); } catch {} }, []);
 function logout() { localStorage.removeItem('token'); localStorage.removeItem('currentUser'); setUser(null); setToken(false); window.dispatchEvent(new Event('auth:expired')); }
 return <ShopLayout>
  <section className="shop-page-hero"><div className="shop-container"><span className="shop-eyebrow">MY HAN SEOUL</span><h1>Tài khoản <em>của bạn</em></h1><p>Quản lý thông tin, món yêu thích và đơn hàng.</p><div className="shop-breadcrumb"><Link href="/">Trang chủ</Link><span>/</span><span>Tài khoản</span></div></div></section>
  <section className="shop-section shop-container account-layout account-view">
   <div className="account-profile-card">
    <div className="account-profile-cover" aria-hidden="true"><span>한</span><small>HAN SEOUL MEMBERS</small></div>
    <div className="account-profile-content">
     <div className="account-avatar">{(user?.fullName || user?.name || 'H').slice(0,1).toUpperCase()}</div>
     <span className="shop-eyebrow">THÀNH VIÊN HAN SEOUL</span>
     <h2>{user?.fullName || user?.name || 'Khách ghé thăm'}</h2>
     <p>{user?.email || user?.phone || 'Đăng nhập để quản lý tài khoản của bạn.'}</p>
     {token ? <button className="shop-outline-link account-session-button" onClick={logout}><Icon name="logout" size={17}/>Đăng xuất</button> : <Link className="shop-button" href="/login">Đăng nhập <Icon name="arrowUpRight" size={18}/></Link>}
    </div>
   </div>
   <div className="account-shortcuts">
    <Link href="/don-hang"><span className="account-shortcut-icon"><Icon name="receipt" size={23}/></span><div><small className="account-shortcut-index">01 / ĐƠN HÀNG</small><b>Đơn hàng của tôi</b><small>Tra cứu trạng thái đơn đặt món</small></div><Icon name="arrowUpRight" size={21}/></Link>
    <Link href="/yeu-thich"><span className="account-shortcut-icon"><Icon name="heart" size={23}/></span><div><small className="account-shortcut-index">02 / YÊU THÍCH</small><b>Món ăn yêu thích</b><small>Những món bạn muốn thử lần tới</small></div><Icon name="arrowUpRight" size={21}/></Link>
    <Link href="/dat-ban"><span className="account-shortcut-icon"><Icon name="calendar" size={23}/></span><div><small className="account-shortcut-index">03 / ĐẶT BÀN</small><b>Lịch hẹn tại nhà hàng</b><small>Đặt bàn cho buổi gặp tiếp theo</small></div><Icon name="arrowUpRight" size={21}/></Link>
    <Link href="/tra-cuu-dat-ban"><span className="account-shortcut-icon"><Icon name="search" size={23}/></span><div><small className="account-shortcut-index">04 / TRA CỨU</small><b>Tra cứu đặt bàn</b><small>Kiểm tra yêu cầu đặt chỗ</small></div><Icon name="arrowUpRight" size={21}/></Link>
   </div>
  </section>
 </ShopLayout>;
}
