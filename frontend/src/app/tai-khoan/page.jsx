'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api } from '@/lib/api';
import './account.css';

const orderStatusBadge = {
  PAID: { label: 'Đã thanh toán', class: 'status-paid' },
  OPEN: { label: 'Đã tiếp nhận', class: 'status-open' },
  SERVING: { label: 'Đang chuẩn bị', class: 'status-serving' },
  CANCELLED: { label: 'Đã hủy', class: 'status-cancelled' },
};

const orderTypeLabel = {
  DELIVERY: 'Giao tận nơi',
  TAKEAWAY: 'Mang về',
  DINE_IN: 'Dùng tại quán',
};

const paymentMethodLabel = {
  VNPAY: 'VNPay',
  ZALOPAY: 'ZaloPay',
  MOMO: 'MoMo',
  CASH: 'Tiền mặt',
  BANKING: 'Chuyển khoản',
};

export default function AccountPage() {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(false);
  const [mounted, setMounted] = useState(false);
  const [orders, setOrders] = useState([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [menuItems, setMenuItems] = useState([]);

  useEffect(() => {
    try {
      const stored = localStorage.getItem('currentUser');
      if (stored) {
        const u = JSON.parse(stored);
        setUser(u);
        if (u && (u.phone || u.id)) {
          setOrdersLoading(true);
          const params = new URLSearchParams();
          if (u.phone) params.set('phone', u.phone);
          if (u.id) params.set('customerId', u.id);
          Promise.all([
            api(`/public-orders/history?${params.toString()}`),
            api('/menu-items'),
          ])
            .then(([orderList, items]) => {
              setOrders(orderList || []);
              setMenuItems(items || []);
            })
            .catch(() => {})
            .finally(() => setOrdersLoading(false));
        }
      }
      setToken(Boolean(localStorage.getItem('token')));
    } catch {}
    setMounted(true);
  }, []);

  const menuMap = useMemo(() => {
    const map = new Map();
    for (const m of menuItems) {
      map.set(m.id, m);
    }
    return map;
  }, [menuItems]);

  function handleLogout() {
    try {
      localStorage.removeItem('currentUser');
      localStorage.removeItem('token');
      setUser(null);
      setToken(false);
      setOrders([]);
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
          <p>Quản lý thông tin hội viên, tích điểm đổi quà và xem lại lịch sử đặt món.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <span>Tài khoản</span>
          </div>
        </div>
      </section>

      <section className="shop-section shop-container">
        <div className="account-view">
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

              {/* DÀNH CHO ADMIN / NHÂN VIÊN */}
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
        </div>

        {/* PHẦN LỊCH SỬ ĐƠN HÀNG CỦA HỘI VIÊN */}
        {mounted && isMember && (
          <div className="account-orders-section">
            <div className="account-orders-header">
              <div>
                <span className="shop-eyebrow">LỊCH SỬ ĐẶT MÓN · MY ORDERS</span>
                <h2
                  style={{
                    fontSize: '26px',
                    margin: '4px 0 6px',
                    color: '#1a202c',
                    fontFamily: "var(--font-display, 'Palatino Linotype', Georgia, serif)",
                  }}
                >
                  Đơn hàng của bạn
                </h2>
                <p style={{ margin: 0, color: '#718096', fontSize: '14px' }}>
                  Theo dõi trạng thái chế biến, giao nhận và hóa đơn chi tiết các đơn bạn đã đặt tại Han Seoul.
                </p>
              </div>
              {orders.length > 0 && (
                <Link
                  href="/thuc-don"
                  className="shop-button"
                  style={{ minHeight: '40px', padding: '8px 20px', fontSize: '13px' }}
                >
                  Đặt món mới +
                </Link>
              )}
            </div>

            {ordersLoading ? (
              <div style={{ textAlign: 'center', padding: '40px 0', color: '#718096', fontSize: '14px' }}>
                <span className="shop-spinner" style={{ marginRight: '8px' }} /> Đang tải lịch sử đơn hàng...
              </div>
            ) : orders.length === 0 ? (
              <div
                style={{
                  background: '#ffffff',
                  border: '1px solid #ede8de',
                  borderRadius: '16px',
                  padding: '48px 24px',
                  textAlign: 'center',
                  boxShadow: '0 4px 16px rgba(0,0,0,0.03)',
                }}
              >
                <span style={{ fontSize: '42px', display: 'block', marginBottom: '12px' }}>🛍️</span>
                <h3 style={{ fontSize: '18px', color: '#1a202c', margin: '0 0 8px' }}>
                  Bạn chưa có đơn hàng nào
                </h3>
                <p style={{ color: '#718096', fontSize: '14px', margin: '0 0 20px', maxWidth: '420px', marginInline: 'auto' }}>
                  Khám phá thực đơn Han Seoul, chọn các món ngon Hàn Quốc bạn yêu thích để bắt đầu thưởng thức và tích điểm hội viên nhé!
                </p>
                <Link href="/thuc-don" className="shop-button" style={{ display: 'inline-flex' }}>
                  Khám phá thực đơn ngay ↗
                </Link>
              </div>
            ) : (
              <div className="account-orders-list">
                {orders.map((order) => {
                  const st = orderStatusBadge[order.status] || { label: order.status, class: 'status-open' };
                  const typeText = orderTypeLabel[order.orderType] || 'Tại nhà hàng';
                  const payText = paymentMethodLabel[order.paymentPreference] || order.paymentPreference || 'Trực tuyến';
                  return (
                    <div key={order.id} className="account-order-card">
                      <div className="account-order-head">
                        <div className="account-order-meta">
                          <span className="account-order-id">Đơn hàng #{order.id}</span>
                          <span className="account-order-type-badge">{typeText}</span>
                          <span style={{ color: '#cbd5e0' }}>•</span>
                          <span style={{ color: '#718096' }}>
                            {order.createdAt ? new Date(order.createdAt).toLocaleString('vi-VN') : 'Mới đặt'}
                          </span>
                        </div>
                        <span className={`account-order-status-badge ${st.class}`}>{st.label}</span>
                      </div>

                      <div className="account-order-body">
                        <div className="account-order-items">
                          {(order.items || []).map((it, idx) => {
                            const dish = menuMap.get(it.menuItemId);
                            const dishName = dish?.nameVi || `Món ăn #${it.menuItemId}`;
                            const itemTotal = Number(it.unitPrice || 0) * Number(it.quantity || 1);
                            return (
                              <div key={idx} className="account-order-item-row">
                                <div>
                                  <b style={{ color: '#2d3748' }}>{dishName}</b>
                                  <span style={{ color: '#8B2500', marginLeft: '8px', fontSize: '13px', fontWeight: 600 }}>
                                    × {it.quantity}
                                  </span>
                                  {it.note && (
                                    <small style={{ color: '#a0aec0', display: 'block', fontSize: '12px' }}>
                                      Ghi chú: {it.note}
                                    </small>
                                  )}
                                </div>
                                <span style={{ fontWeight: 600, color: '#4a5568' }}>
                                  {itemTotal ? itemTotal.toLocaleString('vi-VN') + ' ₫' : '—'}
                                </span>
                              </div>
                            );
                          })}
                        </div>

                        <div className="account-order-foot">
                          <div style={{ fontSize: '13px', color: '#718096' }}>
                            Thanh toán: <strong style={{ color: '#4a5568' }}>{payText}</strong>
                            {order.deliveryAddress && (
                              <span style={{ marginLeft: '12px' }}>
                                · Giao tới: <strong style={{ color: '#4a5568' }}>{order.deliveryAddress}</strong>
                              </span>
                            )}
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flexWrap: 'wrap' }}>
                            <div className="account-order-total">
                              Tổng thanh toán: <strong>{Number(order.total || 0).toLocaleString('vi-VN')} ₫</strong>
                            </div>
                            <Link
                              href={`/don-hang?orderId=${order.id}&phone=${encodeURIComponent(order.customerPhone || user.phone || '')}`}
                              className="shop-outline-link"
                              style={{
                                minHeight: '34px',
                                padding: '5px 14px',
                                fontSize: '12.5px',
                                borderRadius: '8px',
                                border: '1px solid #cbd5e0',
                                color: '#173c32',
                              }}
                            >
                              Chi tiết đơn →
                            </Link>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </section>
    </ShopLayout>
  );
}
