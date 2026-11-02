'use client';

import { Suspense, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api, vnd, dt } from '@/lib/api';
import '../tai-khoan/account.css';

const orderStatusBadge = {
  PAID: { label: 'Đã thanh toán', class: 'status-paid', desc: 'Đơn hàng đã hoàn tất thanh toán thành công.' },
  OPEN: { label: 'Đã tiếp nhận', class: 'status-open', desc: 'Nhà hàng đã nhận đơn và đang chuẩn bị chế biến.' },
  SERVING: { label: 'Đang chuẩn bị', class: 'status-serving', desc: 'Bếp đang nấu các món ăn của bạn.' },
  CANCELLED: { label: 'Đã hủy', class: 'status-cancelled', desc: 'Đơn hàng đã được hủy.' },
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

function OrderTrackingContent() {
  const searchParams = useSearchParams();
  const queryPhone = searchParams.get('phone') || '';
  const queryOrderId = searchParams.get('orderId') || '';
  const initialTab = searchParams.get('tab') === 'reservations' ? 'reservations' : 'orders';
  const created = searchParams.get('created') === '1';
  const payError = searchParams.get('payError') === '1';

  const [activeTab, setActiveTab] = useState(initialTab);
  const [phone, setPhone] = useState('');

  const [user, setUser] = useState(null);
  const [orders, setOrders] = useState([]);
  const [reservations, setReservations] = useState([]);
  const [menuItems, setMenuItems] = useState([]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [payBusy, setPayBusy] = useState(false);

  function checkLocalBackup() {
    try {
      const lastResRaw = localStorage.getItem('hanseoul:lastReservation');
      if (lastResRaw) {
        const parsed = JSON.parse(lastResRaw);
        if (parsed && parsed.id) {
          setReservations([
            {
              id: parsed.id,
              customerName: parsed.fullName || 'Khách hàng',
              customerPhone: parsed.phone,
              tableCode: parsed.tableCode || 'Bàn đã chọn',
              zone: 'Khu vực nhà hàng',
              capacity: parsed.guestCount || 2,
              guestCount: parsed.guestCount || 2,
              reservedAt: parsed.reservedAt,
              status: 'CONFIRMED',
              note: parsed.note,
            },
          ]);
        }
      }
    } catch {}
  }

  // Tải danh sách đơn hàng và lịch đặt bàn tự động
  async function loadHistory(phoneValue, customerIdValue) {
    if (!phoneValue && !customerIdValue) return;
    setLoading(true);
    setError('');

    const params = new URLSearchParams();
    if (phoneValue) params.set('phone', phoneValue);
    if (customerIdValue) params.set('customerId', customerIdValue);

    try {
      const [orderList, resList, items] = await Promise.all([
        api(`/public-orders/history?${params.toString()}`).catch(() => []),
        api(`/reservations/public/history?${params.toString()}`).catch(() => []),
        api('/menu-items').catch(() => []),
      ]);

      setOrders(orderList || []);

      let finalReservations = resList || [];
      // Nếu API chưa kịp index nhưng máy có lưu lần đặt gần nhất thì bổ sung ngay
      if (finalReservations.length === 0) {
        try {
          const lastResRaw = localStorage.getItem('hanseoul:lastReservation');
          if (lastResRaw) {
            const parsed = JSON.parse(lastResRaw);
            if (parsed && parsed.id) {
              finalReservations = [
                {
                  id: parsed.id,
                  customerName: parsed.fullName || 'Khách hàng',
                  customerPhone: parsed.phone || phoneValue,
                  tableCode: parsed.tableCode || 'Bàn đã chọn',
                  zone: 'Khu vực nhà hàng',
                  capacity: parsed.guestCount || 2,
                  guestCount: parsed.guestCount || 2,
                  reservedAt: parsed.reservedAt,
                  status: 'CONFIRMED',
                  note: parsed.note,
                },
              ];
            }
          }
        } catch {}
      }

      setReservations(finalReservations);
      if (items && items.length > 0) {
        setMenuItems(items);
      }

      if (phoneValue) {
        try {
          localStorage.setItem('hanseoul:lastPhone', phoneValue);
        } catch {}
      }
    } catch (err) {
      setError(err.message || 'Không thể tải lịch sử đơn hàng lúc này.');
    } finally {
      setLoading(false);
    }
  }

  // Tự động nhận diện tài khoản hoặc số điện thoại từ trình duyệt
  useEffect(() => {
    let detectedPhone = queryPhone.trim().replace(/[ .-]/g, '');
    let detectedUser = null;

    try {
      const stored = localStorage.getItem('currentUser');
      if (stored) {
        detectedUser = JSON.parse(stored);
        setUser(detectedUser);
        if (!detectedPhone && detectedUser?.phone) {
          detectedPhone = detectedUser.phone.trim().replace(/[ .-]/g, '');
        }
      }

      if (!detectedPhone) {
        const lastPhone = localStorage.getItem('hanseoul:lastPhone');
        if (lastPhone) {
          detectedPhone = lastPhone.trim().replace(/[ .-]/g, '');
        }
      }

      if (!detectedPhone) {
        const lastResRaw = localStorage.getItem('hanseoul:lastReservation');
        if (lastResRaw) {
          const parsed = JSON.parse(lastResRaw);
          if (parsed?.phone) {
            detectedPhone = parsed.phone.trim().replace(/[ .-]/g, '');
          }
        }
      }
    } catch {}

    if (detectedPhone) {
      setPhone(detectedPhone);
      loadHistory(detectedPhone, detectedUser?.id);
    } else {
      // Tải menu items và kiểm tra reservation cục bộ
      checkLocalBackup();
      api('/menu-items')
        .then((items) => setMenuItems(items || []))
        .catch(() => {});
    }
  }, [queryPhone]);

  const menuMap = useMemo(() => {
    const map = new Map();
    for (const m of menuItems) {
      map.set(m.id, m);
    }
    return map;
  }, [menuItems]);

  // Thanh toán lại cho đơn trực tuyến VNPay
  async function payVnpay(orderId, orderPhone) {
    setPayBusy(true);
    setError('');
    try {
      const pay = await api('/payments/vnpay/create', {
        method: 'POST',
        body: { orderId, phone: orderPhone },
      });
      if (!/^https?:\/\//.test(pay?.payUrl || '')) {
        throw new Error('Liên kết thanh toán VNPay không hợp lệ.');
      }
      window.location.assign(pay.payUrl);
    } catch (e) {
      setError(e.message || 'Không thể tạo lại giao dịch VNPay.');
      setPayBusy(false);
    }
  }

  // Thanh toán lại cho đơn trực tuyến ZaloPay
  async function payZalopay(orderId, orderPhone) {
    setPayBusy(true);
    setError('');
    try {
      const pay = await api('/payments/zalopay/create', {
        method: 'POST',
        body: { orderId, phone: orderPhone },
      });
      if (!/^https?:\/\//.test(pay?.orderUrl || '')) {
        throw new Error('Liên kết thanh toán ZaloPay không hợp lệ.');
      }
      window.location.assign(pay.orderUrl);
    } catch (e) {
      setError(e.message || 'Không thể tạo lại giao dịch ZaloPay.');
      setPayBusy(false);
    }
  }

  return (
    <ShopLayout>
      {/* HERO BANNER */}
      <section className="shop-page-hero">
        <div className="shop-container">
          <span className="shop-eyebrow">ORDER & RESERVATION CARE · HAN SEOUL</span>
          <h1>
            Lịch sử <em>đơn hàng & đặt bàn</em>
          </h1>
          <p>Tự động nhận diện và hiển thị chi tiết các món ăn bạn đã gọi cùng lịch hẹn giữ bàn tại nhà hàng.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <Link href="/tai-khoan">Tài khoản</Link>
            <span>/</span>
            <span>{activeTab === 'reservations' ? 'Lịch sử đặt bàn' : 'Đơn hàng'}</span>
          </div>
        </div>
      </section>

      <section className="shop-section shop-container" style={{ maxWidth: '980px', marginInline: 'auto' }}>
        {/* THÔNG BÁO VỪA ĐẶT ĐƠN THÀNH CÔNG */}
        {created && (
          <div
            style={{
              background: '#f0fff4',
              border: '1px solid #9ae6b4',
              borderRadius: '14px',
              padding: '18px 24px',
              marginBottom: '24px',
              display: 'flex',
              alignItems: 'center',
              gap: '14px',
              boxShadow: '0 4px 14px rgba(39, 103, 73, 0.06)',
            }}
          >
            <span style={{ fontSize: '30px' }}>🎉</span>
            <div>
              <strong style={{ color: '#22543d', fontSize: '16px' }}>ĐẶT ĐƠN THÀNH CÔNG!</strong>
              <p style={{ margin: '2px 0 0', color: '#276749', fontSize: '13.5px' }}>
                Đơn hàng của bạn đã được chuyển đến quầy thu ngân và bếp nhà hàng. Chi tiết món ăn hiển thị ngay bên dưới!
              </p>
            </div>
          </div>
        )}

        {/* THÔNG BÁO LỖI THANH TOÁN TRỰC TUYẾN NẾU CÓ */}
        {payError && (
          <div
            style={{
              background: '#fffaf0',
              border: '1px solid #fbd38d',
              borderRadius: '14px',
              padding: '16px 20px',
              marginBottom: '24px',
              color: '#975a16',
              fontSize: '13.5px',
            }}
          >
            ⚠️ Đơn hàng đã được lưu nhưng thanh toán online chưa hoàn tất. Bạn có thể bấm nút &quot;Thanh toán ngay&quot; ở chi tiết đơn bên dưới.
          </div>
        )}

        {error && (
          <div
            style={{
              background: '#fff5f5',
              border: '1px solid #feb2b2',
              borderRadius: '12px',
              padding: '14px 18px',
              color: '#c53030',
              fontSize: '13.5px',
              marginBottom: '20px',
            }}
          >
            {error}
          </div>
        )}

        {/* THANH CHUYỂN TABS TRỰC TIẾP GIỮA ĐƠN ĐẶT MÓN VÀ LỊCH SỬ ĐẶT BÀN */}
        <div className="account-tabs-bar" style={{ marginBottom: '24px' }}>
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

        {/* ===================== TAB 1: ĐƠN ĐẶT MÓN ===================== */}
        {activeTab === 'orders' && (
          <div>
            {loading ? (
              <div style={{ textAlign: 'center', padding: '50px 0', color: '#718096' }}>
                <span className="shop-spinner" style={{ marginRight: '8px' }} /> Đang tải chi tiết đơn hàng của bạn...
              </div>
            ) : orders.length === 0 ? (
              <div
                style={{
                  background: '#ffffff',
                  border: '1px solid #ede8de',
                  borderRadius: '18px',
                  padding: '50px 24px',
                  textAlign: 'center',
                  boxShadow: '0 4px 16px rgba(0,0,0,0.03)',
                }}
              >
                <span style={{ fontSize: '46px', display: 'block', marginBottom: '12px' }}>🛍️</span>
                <h3 style={{ fontSize: '19px', color: '#1a202c', margin: '0 0 8px' }}>
                  Bạn chưa có đơn đặt món nào
                </h3>
                <p style={{ color: '#718096', fontSize: '14px', margin: '0 0 22px', maxWidth: '440px', marginInline: 'auto' }}>
                  Khám phá thực đơn phong phú với các món nướng BBQ, canh hầm và tráng miệng Hàn Quốc truyền thống của Han Seoul ngay!
                </p>
                <Link href="/thuc-don" className="shop-button" style={{ display: 'inline-flex' }}>
                  Khám phá thực đơn ngay ↗
                </Link>
              </div>
            ) : (
              <div className="account-orders-list">
                {orders.map((order) => {
                  const st = orderStatusBadge[order.status] || {
                    label: order.status,
                    class: 'status-open',
                    desc: 'Đơn hàng đang xử lý.',
                  };
                  const typeText = orderTypeLabel[order.orderType] || 'Tại nhà hàng';
                  const payText = paymentMethodLabel[order.paymentPreference] || order.paymentPreference || 'Trực tiếp';
                  const isHighlighted = queryOrderId && String(order.id) === String(queryOrderId);

                  return (
                    <div
                      key={order.id}
                      className="account-order-card"
                      style={
                        isHighlighted
                          ? { borderColor: '#8B2500', boxShadow: '0 0 0 2px rgba(139, 37, 0, 0.2)' }
                          : undefined
                      }
                    >
                      <div className="account-order-head">
                        <div className="account-order-meta">
                          <span className="account-order-id">Đơn hàng #{order.id}</span>
                          {isHighlighted && (
                            <span
                              style={{
                                background: '#feebc8',
                                color: '#744210',
                                fontSize: '11px',
                                fontWeight: 700,
                                padding: '2px 8px',
                                borderRadius: '10px',
                              }}
                            >
                              ⭐ Đơn vừa tạo
                            </span>
                          )}
                          <span className="account-order-type-badge">{typeText}</span>
                          <span style={{ color: '#cbd5e0' }}>•</span>
                          <span style={{ color: '#718096' }}>
                            {order.createdAt ? dt(order.createdAt) : 'Mới đặt'}
                          </span>
                        </div>
                        <span className={`account-order-status-badge ${st.class}`}>{st.label}</span>
                      </div>

                      <div className="account-order-body">
                        {/* DANH SÁCH CHI TIẾT TỪNG MÓN ĂN ĐÃ ĐẶT */}
                        <div style={{ marginBottom: '14px' }}>
                          <small
                            style={{
                              textTransform: 'uppercase',
                              fontSize: '11px',
                              letterSpacing: '0.6px',
                              color: '#718096',
                              fontWeight: 700,
                              display: 'block',
                              marginBottom: '8px',
                            }}
                          >
                            DANH SÁCH MÓN ĐÃ GỌI:
                          </small>

                          <div className="account-order-items">
                            {(order.items || []).map((it, idx) => {
                              const dish = menuMap.get(it.menuItemId);
                              const dishName = dish?.nameVi || `Món ăn #${it.menuItemId}`;
                              const itemTotal = Number(it.unitPrice || 0) * Number(it.quantity || 1);

                              return (
                                <div key={idx} className="account-order-item-row" style={{ padding: '6px 0' }}>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                    <span
                                      style={{
                                        width: '24px',
                                        height: '24px',
                                        borderRadius: '6px',
                                        background: '#f4ede1',
                                        color: '#8B2500',
                                        display: 'grid',
                                        placeItems: 'center',
                                        fontSize: '12px',
                                        fontWeight: 700,
                                      }}
                                    >
                                      🍽️
                                    </span>
                                    <div>
                                      <b style={{ color: '#2d3748', fontSize: '14px' }}>{dishName}</b>
                                      <span
                                        style={{
                                          color: '#8B2500',
                                          marginLeft: '8px',
                                          fontSize: '13px',
                                          fontWeight: 700,
                                        }}
                                      >
                                        × {it.quantity}
                                      </span>
                                      {it.note && (
                                        <small style={{ color: '#a0aec0', display: 'block', fontSize: '12px' }}>
                                          Ghi chú: {it.note}
                                        </small>
                                      )}
                                    </div>
                                  </div>
                                  <span style={{ fontWeight: 650, color: '#2d3748', fontSize: '13.5px' }}>
                                    {itemTotal ? vnd(itemTotal) : '—'}
                                  </span>
                                </div>
                              );
                            })}
                          </div>
                        </div>

                        {/* CHÂN ĐƠN HÀNG: THÔNG TIN THANH TOÁN VÀ TỔNG TIỀN */}
                        <div className="account-order-foot">
                          <div style={{ fontSize: '13px', color: '#718096', lineHeight: '1.6' }}>
                            <div>
                              Thanh toán: <strong style={{ color: '#2d3748' }}>{payText}</strong>
                              {order.customerPhone && (
                                <span style={{ marginLeft: '12px' }}>
                                  · SĐT: <strong style={{ color: '#2d3748' }}>{order.customerPhone}</strong>
                                </span>
                              )}
                            </div>
                            {order.deliveryAddress && (
                              <div>
                                Địa chỉ giao: <strong style={{ color: '#2d3748' }}>{order.deliveryAddress}</strong>
                              </div>
                            )}
                            {order.customerEmail && (
                              <div style={{ color: '#276749', fontSize: '12px', marginTop: '2px' }}>
                                ✓ Hóa đơn điện tử tự động gửi đến: <b>{order.customerEmail}</b>
                              </div>
                            )}
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flexWrap: 'wrap' }}>
                            <div className="account-order-total">
                              Tổng thanh toán: <strong>{vnd(order.total)}</strong>
                            </div>

                            {/* Nút thanh toán lại nếu đơn trực tuyến chưa thanh toán */}
                            {order.paymentPreference === 'VNPAY' && ['OPEN', 'SERVING'].includes(order.status) && (
                              <button
                                type="button"
                                className="shop-button"
                                disabled={payBusy}
                                onClick={() => payVnpay(order.id, order.customerPhone || phone)}
                                style={{ minHeight: '36px', padding: '6px 16px', fontSize: '12.5px' }}
                              >
                                {payBusy ? 'Đang chuyển...' : 'Thanh toán VNPay ↗'}
                              </button>
                            )}

                            {order.paymentPreference === 'ZALOPAY' && ['OPEN', 'SERVING'].includes(order.status) && (
                              <button
                                type="button"
                                className="shop-button"
                                disabled={payBusy}
                                onClick={() => payZalopay(order.id, order.customerPhone || phone)}
                                style={{ minHeight: '36px', padding: '6px 16px', fontSize: '12.5px' }}
                              >
                                {payBusy ? 'Đang chuyển...' : 'Thanh toán ZaloPay ↗'}
                              </button>
                            )}
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

        {/* ===================== TAB 2: LỊCH SỬ ĐẶT BÀN ===================== */}
        {activeTab === 'reservations' && (
          <div>
            {loading ? (
              <div style={{ textAlign: 'center', padding: '50px 0', color: '#718096' }}>
                <span className="shop-spinner" style={{ marginRight: '8px' }} /> Đang tải lịch sử đặt bàn của bạn...
              </div>
            ) : reservations.length === 0 ? (
              <div
                style={{
                  background: '#ffffff',
                  border: '1px solid #ede8de',
                  borderRadius: '18px',
                  padding: '50px 24px',
                  textAlign: 'center',
                  boxShadow: '0 4px 16px rgba(0,0,0,0.03)',
                }}
              >
                <span style={{ fontSize: '46px', display: 'block', marginBottom: '12px' }}>📅</span>
                <h3 style={{ fontSize: '19px', color: '#1a202c', margin: '0 0 8px' }}>
                  Bạn chưa có lịch hẹn đặt bàn nào
                </h3>
                <p style={{ color: '#718096', fontSize: '14px', margin: '0 0 22px', maxWidth: '440px', marginInline: 'auto' }}>
                  Giữ chỗ trước tại Han Seoul để chúng tôi chuẩn bị không gian ấm cúng và những món ăn trọn vị nhất cho bạn!
                </p>
                <Link href="/dat-ban" className="shop-button" style={{ display: 'inline-flex' }}>
                  Đặt bàn ngay bây giờ ↗
                </Link>
              </div>
            ) : (
              <div className="account-res-list">
                {reservations.map((res) => {
                  const st = reservationStatusBadge[res.status] || {
                    label: res.status,
                    class: 'status-pending',
                  };
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
                            Thời gian: <strong>{formattedDate}</strong>
                          </span>
                        </div>
                        <span className={`account-res-badge ${st.class}`}>{st.label}</span>
                      </div>

                      <div className="account-res-body">
                        <div className="account-res-grid">
                          <div className="account-res-col">
                            <small>Vị trí & Bàn đã chọn</small>
                            <b>
                              {res.tableCode || 'Bàn do nhà hàng sắp xếp'}
                              {res.zone ? ` (${res.zone})` : ''}
                            </b>
                          </div>

                          <div className="account-res-col">
                            <small>Số lượng khách</small>
                            <b>{res.guestCount} người</b>
                          </div>

                          <div className="account-res-col">
                            <small>Sức chứa bàn</small>
                            <b>Tối đa {res.capacity || res.guestCount} chỗ</b>
                          </div>

                          <div className="account-res-col">
                            <small>Người liên hệ</small>
                            <b>
                              {res.customerName || (user?.fullName || user?.name || 'Khách hàng')}{' '}
                              {res.customerPhone || phone ? `(${res.customerPhone || phone})` : ''}
                            </b>
                          </div>
                        </div>

                        {res.note && (
                          <div className="account-res-note-box">
                            <strong>📝 Ghi chú yêu cầu:</strong> {res.note}
                          </div>
                        )}

                        <div className="account-res-foot">
                          <span className="account-res-email-notice">
                            ✓ Xác nhận đặt bàn đã được gửi tự động tới Gmail của bạn.
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
                              href="/dat-ban"
                              className="shop-button"
                              style={{ minHeight: '34px', padding: '5px 14px', fontSize: '12.5px', borderRadius: '8px' }}
                            >
                              Đặt thêm bàn mới +
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

export default function OrderTrackingPage() {
  return (
    <Suspense fallback={<div style={{ minHeight: '60vh' }} />}>
      <OrderTrackingContent />
    </Suspense>
  );
}
