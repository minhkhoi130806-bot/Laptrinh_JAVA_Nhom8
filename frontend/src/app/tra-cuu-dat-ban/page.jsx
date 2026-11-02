'use client';

import { useEffect, useState, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api, dt } from '@/lib/api';

const statusBadge = {
  CONFIRMED: { label: '✓ Đã xác nhận bàn', class: 'status-paid', bg: '#c6f6d5', color: '#22543d' },
  PENDING: { label: '⏳ Chờ nhà hàng xác nhận', class: 'status-open', bg: '#fef3c7', color: '#92400e' },
  COMPLETED: { label: '🎉 Đã hoàn tất', class: 'status-serving', bg: '#e0f2fe', color: '#0369a1' },
  CANCELLED: { label: '✕ Đã hủy', class: 'status-cancelled', bg: '#fee2e2', color: '#991b1b' },
};

function ReservationLookupContent() {
  const searchParams = useSearchParams();
  const phoneParam = searchParams.get('phone') || '';

  const [phone, setPhone] = useState('');
  const [results, setResults] = useState([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let initialPhone = phoneParam;
    if (!initialPhone) {
      try {
        const stored = localStorage.getItem('currentUser');
        if (stored) {
          const u = JSON.parse(stored);
          if (u.phone) initialPhone = u.phone;
        }
      } catch {}
    }
    if (initialPhone) {
      setPhone(initialPhone);
      performSearch(initialPhone);
    }
  }, [phoneParam]);

  async function performSearch(phoneQuery) {
    const clean = (phoneQuery || '').replace(/[ .-]/g, '').trim();
    if (!clean) {
      setError('Vui lòng nhập số điện thoại để tra cứu');
      return;
    }
    setError('');
    setLoading(true);
    setSearched(true);
    try {
      const data = await api(`/reservations/public/history?phone=${encodeURIComponent(clean)}`);
      setResults(data || []);
    } catch (err) {
      setError(err.message || 'Không thể tra cứu thông tin đặt bàn vào lúc này.');
      setResults([]);
    } finally {
      setLoading(false);
    }
  }

  function handleSubmit(e) {
    e.preventDefault();
    performSearch(phone);
  }

  return (
    <ShopLayout>
      <section className="shop-page-hero">
        <div className="shop-container">
          <span className="shop-eyebrow">RESERVATION DETAILS</span>
          <h1>
            Tra cứu <em>đặt bàn</em>
          </h1>
          <p>Kiểm tra tình trạng xác nhận lịch hẹn và thông tin bàn ăn tại Han Seoul.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <Link href="/tai-khoan">Tài khoản</Link>
            <span>/</span>
            <span>Tra cứu đặt bàn</span>
          </div>
        </div>
      </section>

      <section className="shop-section shop-container shop-lookup-wrap">
        <div className="shop-lookup-card" style={{ maxWidth: '780px', margin: '0 auto' }}>
          <div className="shop-lookup-symbol">⌕</div>
          <h2>Tra cứu lịch hẹn đặt bàn</h2>
          <p>
            Nhập số điện thoại quý khách đã đăng ký khi đặt bàn để xem trạng thái phê duyệt và thông tin chi tiết.
          </p>

          <form onSubmit={handleSubmit} className="shop-lookup-form" style={{ marginBottom: '24px' }}>
            <label className="shop-field" style={{ flex: 1 }}>
              Số điện thoại người đặt
              <input
                type="tel"
                required
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="Nhập số điện thoại (ví dụ: 0912345678)"
              />
            </label>
            <button className="shop-button" type="submit" disabled={loading} style={{ alignSelf: 'flex-end', height: '48px' }}>
              {loading ? 'Đang tìm...' : 'Kiểm tra thông tin'}
            </button>
          </form>

          {error && <div className="shop-form-error" style={{ marginBottom: '20px' }}>{error}</div>}

          {searched && !loading && results.length === 0 && !error && (
            <div className="shop-form-error" style={{ background: '#fffaf0', borderColor: '#feebc8', color: '#c05621' }}>
              Không tìm thấy lịch hẹn đặt bàn nào với số điện thoại <strong>{phone}</strong>. Quý khách vui lòng kiểm tra lại số điện thoại hoặc liên hệ hotline nhà hàng để được hỗ trợ.
            </div>
          )}

          {results.length > 0 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginTop: '20px', textAlign: 'left' }}>
              <span className="shop-eyebrow" style={{ color: '#8B2500' }}>
                TÌM THẤY {results.length} LỊCH HẸN ĐẶT BÀN
              </span>

              {results.map((res) => {
                const st = statusBadge[res.status] || {
                  label: res.status,
                  bg: '#edf2f7',
                  color: '#4a5568',
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
                  <div
                    key={res.id}
                    style={{
                      background: '#ffffff',
                      border: '1px solid #ede8de',
                      borderRadius: '14px',
                      padding: '20px',
                      boxShadow: '0 4px 12px rgba(0,0,0,0.03)',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px', flexWrap: 'wrap', gap: '8px' }}>
                      <span style={{ fontSize: '16px', fontWeight: 700, color: '#173c32' }}>
                        Mã đặt bàn #{res.id}
                      </span>
                      <span
                        style={{
                          padding: '4px 12px',
                          borderRadius: '20px',
                          fontSize: '12px',
                          fontWeight: 700,
                          background: st.bg,
                          color: st.color,
                        }}
                      >
                        {st.label}
                      </span>
                    </div>

                    <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                        gap: '12px',
                        fontSize: '13.5px',
                        marginBottom: '12px',
                      }}
                    >
                      <div>
                        <small style={{ color: '#718096', display: 'block', textTransform: 'uppercase', fontSize: '11px' }}>
                          Thời gian dùng bữa
                        </small>
                        <b style={{ color: '#1a202c' }}>{formattedDate}</b>
                      </div>
                      <div>
                        <small style={{ color: '#718096', display: 'block', textTransform: 'uppercase', fontSize: '11px' }}>
                          Bàn & Khu vực
                        </small>
                        <b style={{ color: '#1a202c' }}>
                          {res.tableCode} {res.zone ? `(${res.zone})` : ''}
                        </b>
                      </div>
                      <div>
                        <small style={{ color: '#718096', display: 'block', textTransform: 'uppercase', fontSize: '11px' }}>
                          Số lượng khách
                        </small>
                        <b style={{ color: '#1a202c' }}>{res.guestCount} người</b>
                      </div>
                      <div>
                        <small style={{ color: '#718096', display: 'block', textTransform: 'uppercase', fontSize: '11px' }}>
                          Người đặt
                        </small>
                        <b style={{ color: '#1a202c' }}>{res.customerName}</b>
                      </div>
                    </div>

                    {res.note && (
                      <div
                        style={{
                          background: '#fcfbf9',
                          border: '1px dashed #ded6c8',
                          borderRadius: '8px',
                          padding: '8px 12px',
                          fontSize: '13px',
                          color: '#4a5568',
                          marginBottom: '10px',
                        }}
                      >
                        <strong>📝 Ghi chú:</strong> {res.note}
                      </div>
                    )}

                    <div style={{ fontSize: '12px', color: '#276749', paddingTop: '8px', borderTop: '1px dashed #e2e8f0' }}>
                      ✓ Hệ thống đã gửi email xác nhận đặt bàn đến Gmail của bạn.
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          <p className="shop-dish-note" style={{ marginTop: '24px' }}>
            Quý khách có nhu cầu thay đổi thời gian hoặc hủy bàn vui lòng liên hệ hotline{' '}
            <a href="tel:0900000888" style={{ color: '#8B2500', fontWeight: 700 }}>
              0900 000 888
            </a>{' '}
            hoặc gửi tin nhắn qua Trợ lý AI ở góc phải màn hình để được hỗ trợ tức thì.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '16px', marginTop: '16px' }}>
            <Link href="/dat-ban" className="shop-button" style={{ minHeight: '38px', fontSize: '13px' }}>
              Đặt thêm bàn mới ↗
            </Link>
            <Link href="/tai-khoan" className="shop-outline-link" style={{ minHeight: '38px', fontSize: '13px' }}>
              Về trang cá nhân
            </Link>
          </div>
        </div>
      </section>
    </ShopLayout>
  );
}

export default function ReservationLookupPage() {
  return (
    <Suspense fallback={null}>
      <ReservationLookupContent />
    </Suspense>
  );
}
