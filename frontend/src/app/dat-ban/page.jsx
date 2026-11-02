'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import { api, toInputDateTime } from '@/lib/api';

function initialTime() {
  const date = new Date(Date.now() + 2 * 60 * 60 * 1000);
  date.setMinutes(0, 0, 0);
  return toInputDateTime(date);
}

export default function ReservationPage() {
  const [form, setForm] = useState({
    fullName: '',
    phone: '',
    email: '',
    reservedAt: initialTime(),
    guestCount: '2',
    note: '',
  });
  const [tables, setTables] = useState([]);
  const [tableId, setTableId] = useState('');
  const [checking, setChecking] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(null);

  const set = (key, value) => setForm((old) => ({ ...old, [key]: value }));

  // Tự động nạp thông tin hội viên đã đăng nhập từ localStorage để khách không phải gõ lại
  useEffect(() => {
    try {
      const user = JSON.parse(localStorage.getItem('currentUser') || 'null');
      if (user) {
        setForm((prev) => ({
          ...prev,
          fullName: prev.fullName || user.fullName || user.name || '',
          phone: prev.phone || user.phone || '',
          email: prev.email || user.email || '',
        }));
      }
    } catch {}
  }, []);

  // Kiểm tra bàn trống theo thời gian và số lượng khách
  useEffect(() => {
    if (!form.reservedAt || Number(form.guestCount) < 1) return;
    let alive = true;
    setChecking(true);
    setError('');
    setTableId('');
    const query = `at=${encodeURIComponent(form.reservedAt)}&guests=${encodeURIComponent(form.guestCount)}`;
    api(`/reservations/available-tables?${query}`)
      .then((rows) => {
        if (alive) setTables(rows || []);
      })
      .catch((e) => {
        if (alive) {
          setTables([]);
          setError(e.message || 'Không thể kiểm tra bàn trống.');
        }
      })
      .finally(() => alive && setChecking(false));
    return () => {
      alive = false;
    };
  }, [form.reservedAt, form.guestCount]);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess(null);
    if (!tableId) {
      setError('Vui lòng chọn một bàn còn trống.');
      return;
    }
    setSaving(true);
    const submittedEmail = form.email.trim();
    try {
      // Đặt bàn công khai: tự động tạo/cập nhật email khách hàng và kích hoạt gửi email xác nhận
      const saved = await api('/reservations/public', {
        method: 'POST',
        body: {
          fullName: form.fullName.trim(),
          phone: form.phone.trim(),
          email: submittedEmail || null,
          tableId: Number(tableId),
          reservedAt: form.reservedAt,
          guestCount: Number(form.guestCount),
          note: form.note.trim() || null,
        },
      });

      const selectedTable = tables.find((item) => item.id === Number(tableId));
      const bookingRecord = {
        id: saved.id,
        fullName: form.fullName.trim(),
        phone: form.phone.trim(),
        email: submittedEmail,
        reservedAt: form.reservedAt,
        guestCount: Number(form.guestCount),
        note: form.note.trim(),
        tableCode: selectedTable?.tableCode || '',
        savedAt: new Date().toISOString(),
      };
      try {
        localStorage.setItem('hanseoul:lastReservation', JSON.stringify(bookingRecord));
      } catch {}

      setSuccess({
        id: saved.id,
        email: submittedEmail,
        reservedAt: form.reservedAt,
        table: selectedTable,
      });

      // Reset form sau khi đặt thành công
      setForm((prev) => ({
        ...prev,
        note: '',
        reservedAt: initialTime(),
      }));
      setTableId('');
    } catch (e) {
      setError(e.message || 'Đặt bàn chưa thành công. Vui lòng kiểm tra lại thông tin.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <ShopLayout>
      <section className="shop-page-hero shop-booking-hero">
        <div className="shop-container">
          <span className="shop-eyebrow">DÀNH RIÊNG MỘT CHỖ CHO BẠN</span>
          <h1>
            Đặt bàn <em>thật dễ dàng</em>
          </h1>
          <p>Chia sẻ thời gian và số lượng khách, Han Seoul sẽ chuẩn bị bàn phù hợp cho bạn.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <span>Đặt bàn</span>
          </div>
        </div>
      </section>

      <section className="shop-section shop-container shop-booking-layout">
        <div className="shop-booking-copy">
          <span className="shop-eyebrow">YOUR TABLE IS WAITING</span>
          <h2>Hẹn nhau bên bàn ăn Hàn Quốc.</h2>
          <p>
            Dù là bữa tối thân mật, gặp gỡ bạn bè hay dịp đặc biệt, hãy để chúng tôi chăm chút từng chi tiết.
          </p>
          <div className="shop-booking-perk">
            <span>01</span>
            <div>
              <b>Chọn đúng thời gian</b>
              <p>Hệ thống kiểm tra bàn trống theo giờ và số khách.</p>
            </div>
          </div>
          <div className="shop-booking-perk">
            <span>02</span>
            <div>
              <b>Giữ chỗ thuận tiện</b>
              <p>Thông tin đặt bàn được gửi trực tiếp đến hệ thống nhà hàng.</p>
            </div>
          </div>
          <div className="shop-booking-note">
            <span>✦</span> Đặt bàn trước giúp chúng tôi phục vụ bạn chu đáo hơn.
          </div>
        </div>

        <form className="shop-booking-form" onSubmit={submit}>
          {/* BANNER ƯU ĐÃI ĐẶT BÀN ONLINE */}
          <div
            style={{
              background: 'linear-gradient(135deg, #fffaf0 0%, #feebc8 100%)',
              border: '1px solid #fbd38d',
              borderRadius: '12px',
              padding: '16px 20px',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'center',
              gap: '14px',
            }}
          >
            <span style={{ fontSize: '32px' }}>🍧</span>
            <div>
              <strong style={{ color: '#744210', fontSize: '15px' }}>ƯU ĐÃI ĐẶT BÀN ONLINE LẦN ĐẦU</strong>
              <p style={{ margin: '3px 0 0', fontSize: '13px', color: '#975a16', lineHeight: '1.5' }}>
                Tặng ngay <b>01 phần Bingsu kem tuyết Hàn Quốc</b> hoặc <b>giảm 10%</b> khi đặt bàn trước và dùng bữa tại nhà hàng!
              </p>
            </div>
          </div>

          <div className="shop-form-heading">
            <span className="shop-eyebrow">HAN SEOUL · RESERVATION</span>
            <h2>Thông tin đặt bàn</h2>
            <p>Vui lòng điền các thông tin có dấu *</p>
          </div>

          {/* THÔNG BÁO XÁC NHẬN THÀNH CÔNG VÀ THÔNG BÁO GỬI EMAIL */}
          {success && (
            <div
              style={{
                background: '#f0fff4',
                border: '1px solid #9ae6b4',
                borderRadius: '12px',
                padding: '20px 24px',
                marginBottom: '24px',
                textAlign: 'left',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                <span style={{ fontSize: '24px' }}>🎉</span>
                <h3 style={{ margin: 0, color: '#22543d', fontSize: '18px', fontWeight: 700 }}>
                  ĐẶT BÀN THÀNH CÔNG!
                </h3>
              </div>
              <p style={{ margin: '4px 0 10px', fontSize: '14.5px', color: '#276749', lineHeight: '1.5' }}>
                Mã đặt bàn của bạn là <strong style={{ color: '#8B2500', fontSize: '16px' }}>#{success.id}</strong>
                {success.table ? ` · ${success.table.tableCode} (${success.table.zone})` : ''}.
              </p>
              {success.email && (
                <div
                  style={{
                    background: '#ffffff',
                    border: '1px solid #c6f6d5',
                    borderRadius: '8px',
                    padding: '12px 16px',
                    marginBottom: '14px',
                    fontSize: '13.5px',
                    color: '#2d3748',
                  }}
                >
                  📧 <strong>Hệ thống đã gửi email xác nhận chi tiết</strong> tới Gmail:{' '}
                  <b style={{ color: '#8B2500' }}>{success.email}</b>. Quý khách vui lòng kiểm tra hộp thư để xem chi tiết lịch hẹn.
                </div>
              )}
              <p style={{ margin: '0 0 14px', fontSize: '13px', color: '#4a5568' }}>
                ✦ Nhà hàng sẽ giữ bàn trong vòng 15 phút. Khi đến nhà hàng, bạn chỉ cần đọc số điện thoại hoặc mã đặt bàn{' '}
                <strong>#{success.id}</strong>.
              </p>
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                <Link
                  href="/tra-cuu-dat-ban"
                  className="shop-button"
                  style={{ minHeight: '40px', padding: '8px 18px', fontSize: '13px' }}
                >
                  Tra cứu lịch hẹn đặt bàn →
                </Link>
                <Link
                  href="/thuc-don"
                  className="shop-button shop-button-light"
                  style={{ minHeight: '40px', padding: '8px 18px', fontSize: '13px' }}
                >
                  Khám phá thực đơn
                </Link>
              </div>
            </div>
          )}

          <div className="shop-form-grid">
            <label className="shop-field shop-field-full">
              Họ và tên *
              <input
                required
                maxLength={100}
                value={form.fullName}
                onChange={(e) => set('fullName', e.target.value)}
                placeholder="Tên của bạn"
              />
            </label>

            <label className="shop-field">
              Số điện thoại *
              <input
                required
                type="tel"
                pattern="[0-9+() .-]{8,20}"
                value={form.phone}
                onChange={(e) => set('phone', e.target.value)}
                placeholder="0900 000 000"
              />
            </label>

            <label className="shop-field">
              Địa chỉ Gmail (nhận thư xác nhận đặt bàn tự động) *
              <input
                required
                type="email"
                value={form.email}
                onChange={(e) => set('email', e.target.value)}
                placeholder="Ví dụ: cuongmai115111@gmail.com"
              />
              <small style={{ fontSize: '11px', color: '#718096', marginTop: '2px', display: 'block' }}>
                Hệ thống sẽ gửi email chi tiết ngày giờ, mã bàn và vị trí bàn về Gmail này.
              </small>
            </label>

            <label className="shop-field">
              Ngày và giờ *
              <input
                required
                type="datetime-local"
                min={toInputDateTime()}
                value={form.reservedAt}
                onChange={(e) => set('reservedAt', e.target.value)}
              />
            </label>

            <label className="shop-field">
              Số khách *
              <select
                required
                value={form.guestCount}
                onChange={(e) => set('guestCount', e.target.value)}
              >
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20].map((n) => (
                  <option key={n} value={n}>
                    {n} người
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="shop-table-availability">
            <div className="shop-availability-heading">
              <b>Chọn bàn phù hợp *</b>
              <span>{checking ? 'Đang kiểm tra...' : `${tables.length} bàn trống`}</span>
            </div>
            {checking ? (
              <div className="shop-table-loading">Đang tìm bàn phù hợp với yêu cầu của bạn...</div>
            ) : tables.length ? (
              <div className="shop-table-grid">
                {tables.map((table) => (
                  <button
                    type="button"
                    key={table.id}
                    className={`shop-table-option ${String(tableId) === String(table.id) ? 'chosen' : ''}`}
                    onClick={() => setTableId(String(table.id))}
                  >
                    <span className="shop-table-icon">⌂</span>
                    <b>{table.tableCode}</b>
                    <small>
                      {table.capacity} chỗ · {table.zone}
                    </small>
                    {String(tableId) === String(table.id) && <span className="shop-table-check">✓</span>}
                  </button>
                ))}
              </div>
            ) : (
              <p className="shop-no-tables">Không tìm thấy bàn trống. Hãy đổi thời gian hoặc số khách.</p>
            )}
          </div>

          <label className="shop-field shop-field-full">
            Ghi chú thêm
            <textarea
              rows="3"
              maxLength={500}
              value={form.note}
              onChange={(e) => set('note', e.target.value)}
              placeholder="Ví dụ: sinh nhật, ghế trẻ em, bàn gần cửa sổ..."
            />
          </label>

          {error && (
            <div className="shop-form-error" role="alert">
              {error}
            </div>
          )}

          <button
            className="shop-button shop-submit"
            type="submit"
            disabled={saving || checking || !tableId}
          >
            {saving ? 'Đang gửi yêu cầu...' : 'Xác nhận đặt bàn'} <span>↗</span>
          </button>
          <p className="shop-form-footnote">
            Bằng việc gửi yêu cầu, bạn đồng ý để nhà hàng liên hệ và gửi email xác nhận đặt bàn.
          </p>
        </form>
      </section>
    </ShopLayout>
  );
}
