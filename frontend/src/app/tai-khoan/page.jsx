'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import './account.css';

export default function AccountPage() {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(false);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    try {
      const stored = localStorage.getItem('currentUser');
      if (stored) {
        setUser(JSON.parse(stored));
      }
      setToken(Boolean(localStorage.getItem('token')));
    } catch {}
    setMounted(true);
  }, []);

  function handleLogout() {
    try {
      localStorage.removeItem('currentUser');
      localStorage.removeItem('token');
      setUser(null);
      setToken(false);
      window.dispatchEvent(new Event('auth:expired'));
      window.dispatchEvent(new Event('storage'));
    } catch {}
  }

  const isMember = Boolean(user && (user.fullName || user.name || user.phone || user.email));

  return (
    <ShopLayout>
      <section className="shop-page-hero">
        <div className="shop-container">
          <span className="shop-eyebrow">MY HAN SEOUL</span>
          <h1>
            Tài khoản <em>thành viên</em>
          </h1>
          <p>Quản lý thông tin hội viên, tích điểm đổi quà và tra cứu đơn hàng.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <span>Tài khoản</span>
          </div>
        </div>
      </section>

      <section className="shop-section shop-container account-view">
        {/* CỘT TRÁI: THẺ THÔNG TIN HỘI VIÊN */}
        <div className="account-profile-card">
          <div className="account-profile-cover" aria-hidden="true">
            <span>한</span>
            <small>HAN SEOUL MEMBERSHIP</small>
          </div>

          <div className="account-profile-content">
            <div className="account-avatar">
              {isMember ? (user.fullName || user.name || 'H').slice(0, 1).toUpperCase() : '👤'}
            </div>

            {mounted && isMember ? (
              <>
                <span className="account-member-badge">✨ HỘI VIÊN CHÍNH THỨC</span>
                <h2 className="account-user-name">{user.fullName || user.name}</h2>
                <div className="account-info-box">
                  <div className="account-info-row">
                    <span className="account-info-label">Số điện thoại:</span>
                    <strong className="account-info-value">{user.phone || 'Chưa cập nhật'}</strong>
                  </div>
                  <div className="account-info-row">
                    <span className="account-info-label">Gmail nhận hóa đơn:</span>
                    <strong className="account-info-value">{user.email || 'Chưa cập nhật'}</strong>
                  </div>
                  <div className="account-info-row">
                    <span className="account-info-label">Điểm thưởng tích lũy:</span>
                    <span className="account-points-pill">{user.loyaltyPoints || 0} điểm</span>
                  </div>
                </div>

                <div className="account-member-perks">
                  <small>✓ Tự động nhận hóa đơn chi tiết qua Gmail khi thanh toán</small>
                  <small>✓ Tích lũy 1 điểm cho mỗi 10.000 ₫ thanh toán</small>
                </div>

                <div className="account-actions-wrap">
                  <button type="button" className="account-logout-btn" onClick={handleLogout}>
                    <Icon name="logout" size={16} /> Đổi tài khoản khác
                  </button>
                </div>
              </>
            ) : (
              <>
                <span className="shop-eyebrow">KHÁCH HÀNG THÂN THIẾT</span>
                <h2 className="account-user-name">Chào mừng quý khách!</h2>
                <p className="account-guest-desc">
                  Đăng nhập hoặc đăng ký thành viên Han Seoul ngay để nhận 50 điểm thưởng, giảm 10% đơn hàng và tự động nhận hóa đơn điện tử qua Gmail.
                </p>
                <div className="account-actions-wrap" style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <Link className="shop-button" href="/dang-nhap" style={{ justifyContent: 'center' }}>
                    Đăng nhập hội viên <Icon name="arrowUpRight" size={17} />
                  </Link>
                  <Link className="shop-outline-link" href="/dang-ky" style={{ justifyContent: 'center' }}>
                    Chưa có tài khoản? Đăng ký ngay ↗
                  </Link>
                </div>
              </>
            )}

            {/* DÀNH CHO ADMIN / NHÂN VIÊN (CHỈ HIỂN THỊ KHI ĐÃ ĐĂNG NHẬP NHÂN VIÊN VÀ CÓ TOKEN) */}
            {mounted && token && (
              <div className="account-staff-box">
                <span className="account-staff-tag">DÀNH CHO QUẢN TRỊ VIÊN / NHÂN VIÊN</span>
                <p>Bạn đang đăng nhập phiên làm việc nội bộ.</p>
                <Link className="shop-outline-link account-admin-link" href="/admin">
                  Vào Bảng điều khiển Quản trị (Admin) →
                </Link>
              </div>
            )}
          </div>
        </div>

        {/* CỘT PHẢI: LỐI TẮT DỊCH VỤ (2x2 GRID CÂN ĐỐI) */}
        <div className="account-shortcuts">
          <Link href="/don-hang" className="account-shortcut-item">
            <div className="account-shortcut-top">
              <span className="account-shortcut-icon">
                <Icon name="receipt" size={22} />
              </span>
              <Icon name="arrowUpRight" size={18} className="account-shortcut-arrow" />
            </div>
            <div className="account-shortcut-text">
              <small className="account-shortcut-index">01 / ĐƠN HÀNG</small>
              <b>Đơn hàng của tôi</b>
              <small>Tra cứu trạng thái món ăn & lịch sử đặt hàng</small>
            </div>
          </Link>

          <Link href="/yeu-thich" className="account-shortcut-item">
            <div className="account-shortcut-top">
              <span className="account-shortcut-icon">
                <Icon name="heart" size={22} />
              </span>
              <Icon name="arrowUpRight" size={18} className="account-shortcut-arrow" />
            </div>
            <div className="account-shortcut-text">
              <small className="account-shortcut-index">02 / YÊU THÍCH</small>
              <b>Món ăn yêu thích</b>
              <small>Danh sách các món ăn truyền thống bạn đã lưu</small>
            </div>
          </Link>

          <Link href="/dat-ban" className="account-shortcut-item">
            <div className="account-shortcut-top">
              <span className="account-shortcut-icon">
                <Icon name="calendar" size={22} />
              </span>
              <Icon name="arrowUpRight" size={18} className="account-shortcut-arrow" />
            </div>
            <div className="account-shortcut-text">
              <small className="account-shortcut-index">03 / ĐẶT BÀN</small>
              <b>Đặt bàn nhà hàng</b>
              <small>Giữ chỗ trước cho bữa ăn cùng gia đình & bạn bè</small>
            </div>
          </Link>

          <Link href="/tra-cuu-dat-ban" className="account-shortcut-item">
            <div className="account-shortcut-top">
              <span className="account-shortcut-icon">
                <Icon name="search" size={22} />
              </span>
              <Icon name="arrowUpRight" size={18} className="account-shortcut-arrow" />
            </div>
            <div className="account-shortcut-text">
              <small className="account-shortcut-index">04 / TRA CỨU</small>
              <b>Tra cứu đặt bàn</b>
              <small>Kiểm tra tình trạng xác nhận lịch hẹn của bạn</small>
            </div>
          </Link>
        </div>
      </section>
    </ShopLayout>
  );
}
