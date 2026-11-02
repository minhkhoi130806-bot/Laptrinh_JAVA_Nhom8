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

const reservationStatusBadge = {
  CONFIRMED: { label: '✓ Đã xác nhận bàn', class: 'status-confirmed' },
  PENDING: { label: '⏳ Chờ nhà hàng xác nhận', class: 'status-pending' },
  COMPLETED: { label: '🎉 Đã hoàn tất', class: 'status-completed' },
  CANCELLED: { label: '✕ Đã hủy', class: 'status-cancelled' },
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
  const [activeTab, setActiveTab] = useState('orders'); // 'orders' | 'reservations'

  const [orders, setOrders] = useState([]);
  const [ordersLoading, setOrdersLoading] = useState(false);

  const [reservations, setReservations] = useState([]);
  const [reservationsLoading, setReservationsLoading] = useState(false);

  const [menuItems, setMenuItems] = useState([]);

  useEffect(() => {
    try {
      const stored = localStorage.getItem('currentUser');
      if (stored) {
        const u = JSON.parse(stored);
        setUser(u);
        if (u && (u.phone || u.id)) {
          const params = new URLSearchParams();
          if (u.phone) params.set('phone', u.phone);
          if (u.id) params.set('customerId', u.id);

          // 1. Tải lịch sử đơn hàng & danh mục món ăn
          setOrdersLoading(true);
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

          // 2. Tải lịch sử đặt bàn của khách hàng
          setReservationsLoading(true);
          api(`/reservations/public/history?${params.toString()}`)
            .then((resList) => {
              setReservations(resList || []);
            })
            .catch(() => {})
            .finally(() => setReservationsLoading(false));
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
      setReservations([]);
      window.dispatchEvent(new Event('auth:expired'));
      window.dispatchEvent(new Event('storage'));
    } catch {}
  }

  function scrollToHistory(tab) {
    setActiveTab(tab);
    const el = document.getElementById('account-history-section');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
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
          <p>Quản lý thông tin hội viên, tích điểm đổi quà và xem lại lịch sử đặt món & đặt bàn.</p>
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
                    <small>✓ Tự động nhận hóa đơn & xác nhận bàn qua Gmail</small>
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
            <button
              type="button"
              onClick={() => scrollToHistory('orders')}
              className="account-shortcut-item"
              style={{ textAlign: 'left', background: 'none', border: '1px solid #ede8de', cursor: 'pointer', width: '100%' }}
            >
              <div className="account-shortcut-top">
                <span className="account-shortcut-icon">
                  <Icon name="receipt" size={22} />
                </span>
                <span className="account-tab-badge" style={{ fontSize: '11px' }}>{orders.length} đơn</span>
              </div>
              <div className="account-shortcut-text">
                <small className="account-shortcut-index">01 / ĐƠN HÀNG</small>
                <b>Đơn hàng của tôi</b>
                <small>Tra cứu trạng thái món ăn & lịch sử đặt hàng</small>
              </div>
            </button>

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
                <small className="account-shortcut-index">03 / ĐẶT BÀN MỚI</small>
                <b>Đặt bàn nhà hàng</b>
                <small>Giữ chỗ trước cho bữa ăn cùng gia đình & bạn bè</small>
              </div>
            </Link>

            <button
              type="button"
              onClick={() => scrollToHistory('reservations')}
              className="account-shortcut-item"
              style={{ textAlign: 'left', background: 'none', border: '1px solid #ede8de', cursor: 'pointer', width: '100%' }}
            >
              <div className="account-shortcut-top">
                <span className="account-shortcut-icon">
                  <Icon name="search" size={22} />
                </span>
                <span className="account-tab-badge" style={{ fontSize: '11px' }}>{reservations.length} lịch hẹn</span>
              </div>
              <div className="account-shortcut-text">
                <small className="account-shortcut-index">04 / LỊCH SỬ ĐẶT BÀN</small>
                <b>Lịch sử đặt bàn</b>
                <small>Kiểm tra tình trạng xác nhận lịch hẹn của bạn</small>
              </div>
            </button>
          </div>
        </div>

        {/* PHẦN LỊCH SỬ ĐƠN HÀNG & LỊCH SỬ ĐẶT BÀN CỦA HỘI VIÊN */}
        {mounted && isMember && (
          <div className="account-orders-section" id="account-history-section" style={{ scrollMarginTop: '100px' }}>
            <div className="account-orders-header">
              <div>
                <span className="shop-eyebrow">QUẢN LÝ LỊCH SỬ DỊCH VỤ · MY ACTIVITY</span>
                <h2
                  style={{
                    fontSize: '26px',
                    margin: '4px 0 6px',
                    color: '#1a202c',
                    fontFamily: "var(--font-display, 'Palatino Linotype', Georgia, serif)",
                  }}
                >
                  {activeTab === 'orders' ? 'Đơn hàng của bạn' : 'Lịch sử đặt bàn của bạn'}
                </h2>
                <p style={{ margin: 0, color: '#718096', fontSize: '14px' }}>
                  {activeTab === 'orders'
                    ? 'Theo dõi trạng thái chế biến, giao nhận và hóa đơn chi tiết các đơn bạn đã đặt tại Han Seoul.'
                    : 'Xem danh sách và tiến độ xác nhận các lịch hẹn đặt bàn bạn đã đăng ký với nhà hàng.'}
                </p>
              </div>

              <div>
                {activeTab === 'orders' ? (
                  <Link
                    href="/thuc-don"
                    className="shop-button"
                    style={{ minHeight: '40px', padding: '8px 20px', fontSize: '13px' }}
                  >
                    Đặt món mới +
                  </Link>
                ) : (
                  <Link
                    href="/dat-ban"
                    className="shop-button"
                    style={{ minHeight: '40px', padding: '8px 20px', fontSize: '13px' }}
                  >
                    Đặt bàn mới +
                  </Link>
                )}
              </div>
            </div>

            {/* THANH TABS CHUYỂN ĐỔI GIỮA ĐƠN ĐẶT MÓN VÀ LỊCH SỬ ĐẶT BÀN */}
            <div className="account-tabs-bar">
              <button
                type="button"
                className={`account-tab-btn ${activeTab === 'orders' ? 'active' : ''}`}
                onClick={() => setActiveTab('orders')}
              >
                📦 Đơn đặt món <span className="account-tab-badge">{orders.length}</span>
              </button>
              <button
                type="button"
                className={`account-tab-btn ${activeTab === 'reservations' ? 'active' : ''}`}
                onClick={() => setActiveTab('reservations')}
              >
                📅 Lịch sử đặt bàn <span className="account-tab-badge">{reservations.length}</span>
              </button>
            </div>

            {/* TAB 1: DANH SÁCH ĐƠN HÀNG */}
            {activeTab === 'orders' && (
              <div style={{ marginTop: '20px' }}>
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

            {/* TAB 2: DANH SÁCH LỊCH SỬ ĐẶT BÀN */}
            {activeTab === 'reservations' && (
              <div style={{ marginTop: '20px' }}>
                {reservationsLoading ? (
                  <div style={{ textAlign: 'center', padding: '40px 0', color: '#718096', fontSize: '14px' }}>
                    <span className="shop-spinner" style={{ marginRight: '8px' }} /> Đang tải lịch sử đặt bàn...
                  </div>
                ) : reservations.length === 0 ? (
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
                    <span style={{ fontSize: '42px', display: 'block', marginBottom: '12px' }}>📅</span>
                    <h3 style={{ fontSize: '18px', color: '#1a202c', margin: '0 0 8px' }}>
                      Bạn chưa có lịch hẹn đặt bàn nào
                    </h3>
                    <p style={{ color: '#718096', fontSize: '14px', margin: '0 0 20px', maxWidth: '420px', marginInline: 'auto' }}>
                      Hãy đặt bàn trước để Han Seoul chuẩn bị vị trí ngồi đẹp nhất và đón tiếp chu đáo cho bạn cùng gia đình!
                    </p>
                    <Link href="/dat-ban" className="shop-button" style={{ display: 'inline-flex' }}>
                      Đặt bàn ngay bây giờ ↗
                    </Link>
                  </div>
                ) : (
                  <div className="account-res-list">
                    {reservations.map((res) => {
                      const st = reservationStatusBadge[res.status] || { label: res.status, class: 'status-pending' };
                      const dateObj = res.reservedAt ? new Date(res.reservedAt) : null;
                      const formattedDate = dateObj
                        ? dateObj.toLocaleString('vi-VN', {
                            weekday: 'long',
                            year: 'numeric',
                            month: '2-digit',
                            day: '2-digit',
                            hour: '2-digit',
                            minute: '2-digit',
                          })
                        : 'Chưa xác định';

                      return (
                        <div key={res.id} className="account-res-card">
                          <div className="account-res-head">
                            <div className="account-res-meta">
                              <span className="account-res-id">Mã đặt bàn #{res.id}</span>
                              <span style={{ color: '#cbd5e0' }}>•</span>
                              <span style={{ color: '#718096', fontSize: '13px' }}>
                                Thời gian hẹn: <strong>{formattedDate}</strong>
                              </span>
                            </div>
                            <span className={`account-res-badge ${st.class}`}>{st.label}</span>
                          </div>

                          <div className="account-res-body">
                            <div className="account-res-grid">
                              <div className="account-res-col">
                                <small>Vị trí & Bàn đã chọn</small>
                                <b>
                                  {res.tableCode || 'Bàn nhà hàng sắp xếp'}
                                  {res.zone ? ` (${res.zone})` : ''}
                                </b>
                              </div>

                              <div className="account-res-col">
                                <small>Số lượng khách</small>
                                <b>{res.guestCount} người</b>
                              </div>

                              <div className="account-res-col">
                                <small>Thông tin liên hệ</small>
                                <b>
                                  {res.customerName || user.fullName || user.name} · {res.customerPhone || user.phone}
                                </b>
                              </div>

                              <div className="account-res-col">
                                <small>Sức chứa bàn</small>
                                <b>Tối đa {res.capacity || res.guestCount} chỗ</b>
                              </div>
                            </div>

                            {res.note && (
                              <div className="account-res-note-box">
                                <strong>📝 Ghi chú của khách:</strong> {res.note}
                              </div>
                            )}

                            <div className="account-res-foot">
                              <span className="account-res-email-notice">
                                ✓ Email xác nhận đặt bàn đã được gửi tự động tới Gmail của bạn.
                              </span>

                              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <a
                                  href="tel:0900000888"
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
                                  Hotline: 0900 000 888
                                </a>
                                <Link
                                  href={`/don-hang?tab=reservations&phone=${encodeURIComponent(res.customerPhone || user.phone || '')}`}
                                  className="shop-button"
                                  style={{
                                    minHeight: '34px',
                                    padding: '5px 14px',
                                    fontSize: '12.5px',
                                    borderRadius: '8px',
                                  }}
                                >
                                  Xem chi tiết đặt bàn ↗
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
          </div>
        )}
      </section>
    </ShopLayout>
  );
}
