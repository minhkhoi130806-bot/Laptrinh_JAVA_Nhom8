'use client';
import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api, vnd } from '@/lib/api';
import { readCart, clearCart, cartTotal } from '@/lib/cart';

const initialForm = { name: '', phone: '', email: '', orderType: 'TAKEAWAY', address: '', payment: 'CASH', note: '' };
export default function CheckoutPage() {
  const router = useRouter();
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [momoEnabled, setMomoEnabled] = useState(false);
  const [momoWhy, setMomoWhy] = useState('đang kiểm tra...');
  const [vnpayEnabled, setVnpayEnabled] = useState(false);
  const [vnpayWhy, setVnpayWhy] = useState('đang kiểm tra...');
  const total = useMemo(() => cartTotal(items), [items]);
  useEffect(() => { setItems(readCart()); try { const user = JSON.parse(localStorage.getItem('currentUser') || 'null'); if (user) setForm((old) => ({ ...old, name: user.fullName || user.name || '', email: user.email || '', phone: user.phone || '' })); } catch {} }, []);
  // Chỉ hiện MoMo/VNPay khi backend đã cấu hình đủ khóa.
  useEffect(() => { api('/payments/methods').then((m) => { setMomoEnabled(Boolean(m?.momo)); setMomoWhy(m?.momo ? '' : `backend thiếu cấu hình: ${(m?.missing || []).join(', ') || 'không rõ'}`); setVnpayEnabled(Boolean(m?.vnpay)); setVnpayWhy(m?.vnpay ? '' : `backend thiếu cấu hình: ${(m?.vnpayMissing || []).join(', ') || 'không rõ'}`); }).catch((e) => { setMomoEnabled(false); setMomoWhy(`không gọi được /api/payments/methods (${e.message}). Hãy khởi động lại backend bằng code mới.`); setVnpayEnabled(false); setVnpayWhy(`không gọi được /api/payments/methods (${e.message}). Hãy khởi động lại backend bằng code mới.`); }); }, []);
  function change(key, value) { setForm((old) => ({ ...old, [key]: value })); }
  async function submit(e) {
    e.preventDefault(); setError('');
    if (!items.length) { setError('Giỏ hàng đang trống.'); return; }
    if (!/^0\d{9,10}$/.test(form.phone.replace(/[ .-]/g, ''))) { setError('Vui lòng nhập số điện thoại Việt Nam hợp lệ.'); return; }
    if (form.orderType === 'DELIVERY' && form.address.trim().length < 8) { setError('Vui lòng nhập địa chỉ giao hàng đầy đủ.'); return; }
    setBusy(true);
    try {
      // Giá không gửi từ trình duyệt; backend tự lấy đơn giá thực từ database.
      const order = await api('/public-orders', { method: 'POST', body: {
        customerName: form.name.trim(), customerPhone: form.phone.replace(/[ .-]/g, ''), customerEmail: form.email.trim() || null,
        orderType: form.orderType, deliveryAddress: form.orderType === 'DELIVERY' ? form.address.trim() : null,
        customerNote: form.note.trim() || null, paymentPreference: form.payment,
        items: items.map((item) => ({ menuItemId: item.id, quantity: item.quantity, note: item.note || null })),
      }});
      clearCart();
      const phone = form.phone.replace(/[ .-]/g, '');
      try { localStorage.setItem('hanseoul:lastOrderId', String(order.id)); localStorage.setItem('hanseoul:lastPhone', phone); } catch {}
      if (form.payment === 'MOMO') {
        // Đơn đã lưu; xin liên kết thanh toán từ backend (số tiền do server tính) rồi chuyển sang MoMo.
        try {
          const pay = await api('/payments/momo/create', { method: 'POST', body: { orderId: order.id, phone } });
          if (!/^https:\/\//.test(pay?.payUrl || '')) throw new Error('Liên kết thanh toán không hợp lệ.');
          window.location.assign(pay.payUrl);
          return;
        } catch {
          router.push(`/don-hang?orderId=${order.id}&phone=${encodeURIComponent(phone)}&created=1&payError=1`);
          return;
        }
      }
      if (form.payment === 'VNPAY') {
        // Đơn đã lưu; xin URL thanh toán VNPay từ backend rồi chuyển hướng.
        try {
          const pay = await api('/payments/vnpay/create', { method: 'POST', body: { orderId: order.id, phone } });
          if (!/^https?:\/\//.test(pay?.payUrl || '')) throw new Error('Liên kết thanh toán không hợp lệ.');
          window.location.assign(pay.payUrl);
          return;
        } catch {
          router.push(`/don-hang?orderId=${order.id}&phone=${encodeURIComponent(phone)}&created=1&payError=1`);
          return;
        }
      }
      router.push(`/don-hang?orderId=${order.id}&phone=${encodeURIComponent(phone)}&created=1`);
    } catch (e2) {
      setError(`${e2.message || 'Không thể gửi đơn.'} Nếu đơn đã được tạo nhưng thêm món bị lỗi, vui lòng cung cấp mã đơn cho nhân viên để kiểm tra trước khi đặt lại.`);
    } finally { setBusy(false); }
  }
  const isOnline = form.payment === 'MOMO' || form.payment === 'VNPAY';
  const payLabel = form.payment === 'MOMO' ? 'MoMo' : form.payment === 'VNPAY' ? 'VNPay' : null;
  return <ShopLayout><section className="shop-page-hero"><div className="shop-container"><span className="shop-eyebrow">ĐẶT MÓN · HAN SEOUL</span><h1>Hoàn tất <em>đơn hàng</em></h1><p>Điền thông tin bên dưới để chúng tôi chuẩn bị bữa ăn dành cho bạn.</p><div className="shop-breadcrumb"><Link href="/">Trang chủ</Link><span>/</span><Link href="/gio-hang">Giỏ hàng</Link><span>/</span><span>Đặt món</span></div></div></section>
  <section className="shop-section shop-container checkout-layout"><form className="checkout-form" onSubmit={submit}><div className="cart-section-title"><div><span className="shop-eyebrow">BƯỚC 1</span><h2>Thông tin liên hệ</h2></div></div><div className="checkout-fields"><label>Họ và tên *<input required maxLength={100} value={form.name} onChange={(e) => change('name', e.target.value)} placeholder="Tên người nhận" /></label><label>Số điện thoại *<input required type="tel" maxLength={15} value={form.phone} onChange={(e) => change('phone', e.target.value)} placeholder="09xxxxxxxx" /></label><label>Email<input type="email" maxLength={150} value={form.email} onChange={(e) => change('email', e.target.value)} placeholder="ban@email.com" /></label></div>
  <div className="cart-section-title checkout-subtitle"><div><span className="shop-eyebrow">BƯỚC 2</span><h2>Hình thức nhận món</h2></div></div><div className="checkout-type-options"><label className={form.orderType === 'TAKEAWAY' ? 'chosen' : ''}><input type="radio" name="orderType" checked={form.orderType === 'TAKEAWAY'} onChange={() => change('orderType', 'TAKEAWAY')} /><span><Icon name="bag" size={25}/></span><b>Đến lấy</b><small>Nhận tại nhà hàng</small></label><label className={form.orderType === 'DELIVERY' ? 'chosen' : ''}><input type="radio" name="orderType" checked={form.orderType === 'DELIVERY'} onChange={() => change('orderType', 'DELIVERY')} /><span><Icon name="truck" size={25}/></span><b>Giao tận nơi</b><small>Nhập địa chỉ nhận hàng</small></label><label className={form.orderType === 'DINE_IN' ? 'chosen' : ''}><input type="radio" name="orderType" checked={form.orderType === 'DINE_IN'} onChange={() => change('orderType', 'DINE_IN')} /><span><Icon name="utensils" size={25}/></span><b>Dùng tại quán</b><small>Nên đặt bàn trước</small></label></div>
  {form.orderType === 'DELIVERY' && <label className="checkout-full-field">Địa chỉ giao hàng *<textarea required minLength={8} maxLength={500} rows={3} value={form.address} onChange={(e) => change('address', e.target.value)} placeholder="Số nhà, đường, phường/xã, quận/huyện..." /></label>}
  <div className="cart-section-title checkout-subtitle"><div><span className="shop-eyebrow">BƯỚC 3</span><h2>Phương thức thanh toán</h2></div></div><div className="checkout-payment-options"><label><input type="radio" name="payment" checked={form.payment === 'CASH'} onChange={() => change('payment', 'CASH')} /><span><b>Thanh toán khi nhận món</b><small>Tiền mặt tại quầy hoặc khi nhận hàng.</small></span></label><label><input type="radio" name="payment" checked={form.payment === 'BANKING'} onChange={() => change('payment', 'BANKING')} /><span><b>Chuyển khoản thủ công</b><small>Nhân viên xác nhận đơn và hướng dẫn chuyển khoản.</small></span></label><label className={momoEnabled ? (form.payment === 'MOMO' ? 'chosen' : '') : 'checkout-disabled-option'}><input type="radio" name="payment" disabled={!momoEnabled} checked={form.payment === 'MOMO'} onChange={() => change('payment', 'MOMO')} /><span><b>Ví MoMo</b><small>{momoEnabled ? 'Thanh toán online qua ví MoMo (môi trường thử nghiệm).' : 'Tạm thời chưa khả dụng. Vui lòng chọn phương thức khác.'}</small></span></label><label className={vnpayEnabled ? (form.payment === 'VNPAY' ? 'chosen' : '') : 'checkout-disabled-option'}><input type="radio" name="payment" disabled={!vnpayEnabled} checked={form.payment === 'VNPAY'} onChange={() => change('payment', 'VNPAY')} /><span><b>VNPay (Thẻ ATM / Visa / MasterCard / QR)</b><small>{vnpayEnabled ? 'Thanh toán online qua VNPay - hỗ trợ thẻ ngân hàng nội địa, Visa, MasterCard, JCB, QR Code (môi trường thử nghiệm).' : 'Tạm thời chưa khả dụng. Vui lòng chọn phương thức khác.'}</small></span></label></div>
  <label className="checkout-full-field">Ghi chú cho nhà hàng<textarea rows={3} maxLength={1000} value={form.note} onChange={(e) => change('note', e.target.value)} placeholder="Ví dụ: ít cay, không hành, thời gian nhận món..." /></label>
  {error && <div className="checkout-error" role="alert">{error}</div>}
  {form.payment === 'MOMO' && <div className="checkout-info">Sau khi xác nhận, bạn sẽ được chuyển sang MoMo để thanh toán. Đơn chỉ được ghi nhận đã thanh toán khi MoMo xác nhận thành công.</div>}
  {form.payment === 'VNPAY' && <div className="checkout-info">Sau khi xác nhận, bạn sẽ được chuyển sang cổng VNPay để thanh toán bằng thẻ ATM nội địa, Visa, MasterCard, JCB hoặc quét mã QR. Đơn chỉ được ghi nhận đã thanh toán khi VNPay xác nhận thành công.</div>}
  <button className="shop-button checkout-submit" type="submit" disabled={busy || !items.length}>{busy ? (isOnline ? `Đang chuyển sang ${payLabel}...` : 'Đang gửi đơn...') : (isOnline ? `Thanh toán ${payLabel} · ${vnd(total)}` : `Xác nhận đặt món · ${vnd(total)}`)} <span>↗</span></button><p className="checkout-terms">Bằng cách gửi đơn, bạn đồng ý để nhà hàng liên hệ xác nhận thông tin. Giá cuối cùng được xác nhận theo thực đơn của nhà hàng.</p></form>
  <aside className="cart-summary"><span className="shop-eyebrow">ĐƠN CỦA BẠN</span><h2>{items.length} món đã chọn</h2>{items.map((item) => <div className="checkout-order-row" key={`${item.id}-${item.note || ''}`}><span>{item.name} <small>× {item.quantity}</small></span><b>{vnd(item.price * item.quantity)}</b></div>)}<div className="cart-summary-total"><span>Tạm tính</span><strong>{vnd(total)}</strong></div><p>Phí giao hàng (nếu có) và chương trình giảm giá sẽ được nhà hàng xác nhận.</p><Link className="cart-back-link" href="/gio-hang">← Quay lại giỏ hàng</Link></aside></section></ShopLayout>;
}
