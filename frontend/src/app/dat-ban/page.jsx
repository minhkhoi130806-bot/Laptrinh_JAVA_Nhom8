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
  const [form, setForm] = useState({ fullName: '', phone: '', email: '', reservedAt: initialTime(), guestCount: '2', note: '' });
  const [tables, setTables] = useState([]);
  const [tableId, setTableId] = useState('');
  const [checking, setChecking] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(null);
  const set = (key, value) => setForm((old) => ({ ...old, [key]: value }));

  useEffect(() => {
    if (!form.reservedAt || Number(form.guestCount) < 1) return;
    let alive = true;
    setChecking(true); setError(''); setTableId('');
    const query = `at=${encodeURIComponent(form.reservedAt)}&guests=${encodeURIComponent(form.guestCount)}`;
    api(`/reservations/available-tables?${query}`)
      .then((rows) => { if (alive) setTables(rows || []); })
      .catch((e) => { if (alive) { setTables([]); setError(e.message || 'Không thể kiểm tra bàn trống.'); } })
      .finally(() => alive && setChecking(false));
    return () => { alive = false; };
  }, [form.reservedAt, form.guestCount]);

  const submit = async (event) => {
    event.preventDefault(); setError(''); setSuccess(null);
    if (!tableId) { setError('Vui lòng chọn một bàn còn trống.'); return; }
    setSaving(true);
    try {
      const allCustomers = await api('/customers');
      const phone = form.phone.trim();
      let customer = (allCustomers || []).find((item) => item.phone === phone && !item.deleted);
      if (!customer) {
        customer = await api('/customers', { method: 'POST', body: { fullName: form.fullName.trim(), phone, email: form.email.trim() || null } });
      }
      const saved = await api('/reservations', { method: 'POST', body: { customerId: Number(customer.id), tableId: Number(tableId), reservedAt: form.reservedAt, guestCount: Number(form.guestCount), note: form.note.trim() || null } });
      setSuccess({ id: saved.id, reservedAt: form.reservedAt, table: tables.find((item) => item.id === Number(tableId)) });
      setForm({ fullName: '', phone: '', email: '', reservedAt: initialTime(), guestCount: '2', note: '' });
      setTableId('');
    } catch (e) { setError(e.message || 'Đặt bàn chưa thành công. Vui lòng thử lại.'); }
    finally { setSaving(false); }
  };

  return <ShopLayout>
    <section className="shop-page-hero shop-booking-hero"><div className="shop-container"><span className="shop-eyebrow">DÀNH RIÊNG MỘT CHỖ CHO BẠN</span><h1>Đặt bàn <em>thật dễ dàng</em></h1><p>Chia sẻ thời gian và số lượng khách, Han Seoul sẽ chuẩn bị bàn phù hợp cho bạn.</p><div className="shop-breadcrumb"><Link href="/">Trang chủ</Link><span>/</span><span>Đặt bàn</span></div></div></section>
    <section className="shop-section shop-container shop-booking-layout">
      <div className="shop-booking-copy"><span className="shop-eyebrow">YOUR TABLE IS WAITING</span><h2>Hẹn nhau bên bàn ăn Hàn Quốc.</h2><p>Dù là bữa tối thân mật, gặp gỡ bạn bè hay dịp đặc biệt, hãy để chúng tôi chăm chút từng chi tiết.</p><div className="shop-booking-perk"><span>01</span><div><b>Chọn đúng thời gian</b><p>Hệ thống kiểm tra bàn trống theo giờ và số khách.</p></div></div><div className="shop-booking-perk"><span>02</span><div><b>Giữ chỗ thuận tiện</b><p>Thông tin đặt bàn được gửi trực tiếp đến hệ thống nhà hàng.</p></div></div><div className="shop-booking-note"><span>✦</span> Đặt bàn trước giúp chúng tôi phục vụ bạn chu đáo hơn.</div></div>
      <form className="shop-booking-form" onSubmit={submit}>
        <div className="shop-form-heading"><span className="shop-eyebrow">HAN SEOUL · RESERVATION</span><h2>Thông tin đặt bàn</h2><p>Vui lòng điền các thông tin có dấu *</p></div>
        {success && <div className="shop-success"><span>✓</span><div><b>Đã gửi yêu cầu đặt bàn!</b><p>Mã đặt bàn <strong>#{success.id}</strong>{success.table ? ` · ${success.table.tableCode}` : ''}. Nhà hàng sẽ xác nhận yêu cầu của bạn.</p></div></div>}
        <div className="shop-form-grid"><label className="shop-field shop-field-full">Họ và tên *<input required maxLength={100} value={form.fullName} onChange={(e) => set('fullName', e.target.value)} placeholder="Tên của bạn" /></label><label className="shop-field">Số điện thoại *<input required type="tel" pattern="[0-9+() .-]{8,20}" value={form.phone} onChange={(e) => set('phone', e.target.value)} placeholder="0900 000 000" /></label><label className="shop-field">Email<input type="email" value={form.email} onChange={(e) => set('email', e.target.value)} placeholder="ban@email.com" /></label><label className="shop-field">Ngày và giờ *<input required type="datetime-local" min={toInputDateTime()} value={form.reservedAt} onChange={(e) => set('reservedAt', e.target.value)} /></label><label className="shop-field">Số khách *<select required value={form.guestCount} onChange={(e) => set('guestCount', e.target.value)}>{[1,2,3,4,5,6,7,8,9,10,12,15,20].map((n) => <option key={n} value={n}>{n} người</option>)}</select></label></div>
        <div className="shop-table-availability"><div className="shop-availability-heading"><b>Chọn bàn phù hợp *</b><span>{checking ? 'Đang kiểm tra...' : `${tables.length} bàn trống`}</span></div>{checking ? <div className="shop-table-loading">Đang tìm bàn phù hợp với yêu cầu của bạn...</div> : tables.length ? <div className="shop-table-grid">{tables.map((table) => <button type="button" key={table.id} className={`shop-table-option ${String(tableId) === String(table.id) ? 'chosen' : ''}`} onClick={() => setTableId(String(table.id))}><span className="shop-table-icon">⌂</span><b>{table.tableCode}</b><small>{table.capacity} chỗ · {table.zone}</small>{String(tableId) === String(table.id) && <span className="shop-table-check">✓</span>}</button>)}</div> : <p className="shop-no-tables">Không tìm thấy bàn trống. Hãy đổi thời gian hoặc số khách.</p>}</div>
        <label className="shop-field shop-field-full">Ghi chú thêm<textarea rows="3" maxLength={500} value={form.note} onChange={(e) => set('note', e.target.value)} placeholder="Ví dụ: sinh nhật, ghế trẻ em, yêu cầu đặc biệt..." /></label>
        {error && <div className="shop-form-error" role="alert">{error}</div>}
        <button className="shop-button shop-submit" type="submit" disabled={saving || checking || !tableId}>{saving ? 'Đang gửi yêu cầu...' : 'Xác nhận đặt bàn'} <span>↗</span></button>
        <p className="shop-form-footnote">Bằng việc gửi yêu cầu, bạn đồng ý để nhà hàng liên hệ xác nhận đặt bàn.</p>
      </form>
    </section>
  </ShopLayout>;
}
