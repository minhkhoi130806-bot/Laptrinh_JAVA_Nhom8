'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api, vnd } from '@/lib/api';
import { readCart, clearCart, cartTotal } from '@/lib/cart';

const initialForm = {
  name: '',
  phone: '',
  email: '',
  orderType: 'TAKEAWAY',
  address: '',
  payment: 'CASH',
  note: '',
};

export default function CheckoutPage() {
  const router = useRouter();
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const [vnpayEnabled, setVnpayEnabled] = useState(false);
  const [vnpayWhy, setVnpayWhy] = useState('đang kiểm tra...');
  const [zalopayEnabled, setZalopayEnabled] = useState(false);
  const [zalopayWhy, setZalopayWhy] = useState('đang kiểm tra...');

  const [memberUser, setMemberUser] = useState(null);

  // Tạm tính trước giảm giá
  const rawSubtotal = useMemo(() => cartTotal(items), [items]);

  // Ưu đãi thành viên: Giảm ngay 10% (tối đa 50.000 ₫) khi đã là thành viên
  const memberDiscount = useMemo(() => {
    if (!memberUser || rawSubtotal <= 0) return 0;
    return Math.min(Math.round(rawSubtotal * 0.1), 50000);
  }, [memberUser, rawSubtotal]);

  // Tổng tiền thực tế cần thanh toán
  const total = useMemo(() => Math.max(0, rawSubtotal - memberDiscount), [rawSubtotal, memberDiscount]);

  useEffect(() => {
    setItems(readCart());
    try {
      const user = JSON.parse(localStorage.getItem('currentUser') || 'null');
      if (user) {
        setMemberUser(user);
        setForm((old) => ({
          ...old,
          name: user.fullName || user.name || '',
          email: user.email || '',
          phone: user.phone || '',
        }));
      }
    } catch {}
  }, []);

  // Chỉ hiện VNPay/ZaloPay khi backend đã cấu hình đủ khóa.
  useEffect(() => {
    api('/payments/methods')
      .then((m) => {
        setVnpayEnabled(Boolean(m?.vnpay));
        setVnpayWhy(m?.vnpay ? '' : `backend thiếu cấu hình: ${(m?.vnpayMissing || []).join(', ') || 'không rõ'}`);
        setZalopayEnabled(Boolean(m?.zalopay));
        setZalopayWhy(m?.zalopay ? '' : `backend thiếu cấu hình: ${(m?.zalopayMissing || []).join(', ') || 'không rõ'}`);
      })
      .catch((e) => {
        setVnpayEnabled(false);
        setVnpayWhy(`không gọi được /api/payments/methods (${e.message}).`);
        setZalopayEnabled(false);
        setZalopayWhy(`không gọi được /api/payments/methods (${e.message}).`);
      });
  }, []);

  function change(key, value) {
    setForm((old) => ({ ...old, [key]: value }));
  }

  // Kiểm tra đơn hàng trực tuyến / thanh toán online bắt buộc phải có tài khoản
  const requiresMember = form.orderType === 'DELIVERY' || form.payment === 'VNPAY' || form.payment === 'ZALOPAY';

  async function submit(e) {
    e.preventDefault();
    setError('');

    if (!items.length) {
      setError('Giỏ hàng đang trống.');
      return;
    }
    if (!/^0\d{9,10}$/.test(form.phone.replace(/[ .-]/g, ''))) {
      setError('Vui lòng nhập số điện thoại Việt Nam hợp lệ.');
      return;
    }
    if (form.orderType === 'DELIVERY' && form.address.trim().length < 8) {
      setError('Vui lòng nhập địa chỉ giao hàng đầy đủ.');
      return;
    }

    // BẮT BUỘC ĐĂNG KÝ/ĐĂNG NHẬP CHO ĐƠN GIAO HÀNG & THANH TOÁN ONLINE
    if (requiresMember && !memberUser) {
      setError('Đơn hàng giao tận nơi và thanh toán online bắt buộc phải đăng nhập hoặc đăng ký thành viên. Vui lòng đăng nhập hoặc đăng ký bên dưới để tiếp tục.');
      return;
    }

    setBusy(true);
    try {
      let finalNote = form.note ? form.note.trim() : '';
      if (memberDiscount > 0) {
        finalNote = finalNote
          ? `${finalNote} | [Hội viên ưu đãi -10%: -${vnd(memberDiscount)}]`
          : `[Hội viên ưu đãi -10%: -${vnd(memberDiscount)}]`;
      }

      // Giá không gửi từ trình duyệt; backend tự lấy đơn giá thực từ database.
      const order = await api('/public-orders', {
        method: 'POST',
        body: {
          customerName: form.name.trim(),
          customerPhone: form.phone.replace(/[ .-]/g, ''),
          customerEmail: form.email.trim() || null,
          orderType: form.orderType,
          deliveryAddress: form.orderType === 'DELIVERY' ? form.address.trim() : null,
          customerNote: finalNote || null,
          paymentPreference: form.payment,
          items: items.map((item) => ({
            menuItemId: item.id,
            quantity: item.quantity,
            note: item.note || null,
          })),
        },
      });

      clearCart();
      const phone = form.phone.replace(/[ .-]/g, '');
      try {
        localStorage.setItem('hanseoul:lastOrderId', String(order.id));
        localStorage.setItem('hanseoul:lastPhone', phone);
      } catch {}

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

      if (form.payment === 'ZALOPAY') {
        // Đơn đã lưu; xin URL thanh toán ZaloPay từ backend rồi chuyển hướng.
        try {
          const pay = await api('/payments/zalopay/create', { method: 'POST', body: { orderId: order.id, phone } });
          if (!/^https?:\/\//.test(pay?.orderUrl || '')) throw new Error('Liên kết thanh toán ZaloPay không hợp lệ.');
          window.location.assign(pay.orderUrl);
          return;
        } catch {
          router.push(`/don-hang?orderId=${order.id}&phone=${encodeURIComponent(phone)}&created=1&payError=1`);
          return;
        }
      }

      router.push(`/don-hang?orderId=${order.id}&phone=${encodeURIComponent(phone)}&created=1`);
    } catch (e2) {
      setError(
        `${e2.message || 'Không thể gửi đơn.'} Nếu đơn đã được tạo nhưng thêm món bị lỗi, vui lòng cung cấp mã đơn cho nhân viên để kiểm tra trước khi đặt lại.`
      );
    } finally {
      setBusy(false);
    }
  }

  const isOnline = form.payment === 'VNPAY' || form.payment === 'ZALOPAY';
  const payLabel = form.payment === 'VNPAY' ? 'VNPay' : form.payment === 'ZALOPAY' ? 'ZaloPay' : null;

  return (
    <ShopLayout>
      <section className="shop-page-hero">
        <div className="shop-container">
          <span className="shop-eyebrow">ĐẶT MÓN · HAN SEOUL</span>
          <h1>
            Hoàn tất <em>đơn hàng</em>
          </h1>
          <p>Điền thông tin bên dưới để chúng tôi chuẩn bị bữa ăn dành cho bạn.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <Link href="/gio-hang">Giỏ hàng</Link>
            <span>/</span>
            <span>Đặt món</span>
          </div>
        </div>
      </section>

      <section className="shop-section shop-container checkout-layout">
        <form className="checkout-form" onSubmit={submit}>
          {/* TRẠNG THÁI HỘI VIÊN */}
          {memberUser ? (
            <div
              style={{
                background: '#f0fff4',
                border: '1px solid #9ae6b4',
                borderRadius: '12px',
                padding: '14px 18px',
                marginBottom: '22px',
                display: 'flex',
                alignItems: 'center',
                gap: '14px',
              }}
            >
              <div
                style={{
                  fontSize: '26px',
                  background: '#c6f6d5',
                  width: '46px',
                  height: '46px',
                  borderRadius: '50%',
                  display: 'grid',
                  placeItems: 'center',
                  flexShrink: 0,
                }}
              >
                🎖️
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                  <strong style={{ color: '#22543d', fontSize: '15px' }}>{memberUser.fullName || memberUser.name}</strong>
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: 700,
                      background: '#276749',
                      color: '#fff',
                      padding: '2px 8px',
                      borderRadius: '12px',
                    }}
                  >
                    ĐÃ XÁC NHẬN HỘI VIÊN
                  </span>
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: 700,
                      background: '#fbd38d',
                      color: '#744210',
                      padding: '2px 8px',
                      borderRadius: '12px',
                    }}
                  >
                    GIẢM 10% ĐƠN NÀY
                  </span>
                </div>
                <div style={{ margin: '3px 0 0', fontSize: '13px', color: '#2f855a' }}>
                  SĐT: <b>{memberUser.phone}</b> · Email: <b>{memberUser.email || '—'}</b> · Tích lũy: <b>{memberUser.loyaltyPoints || 0} điểm</b>
                </div>
                <div style={{ color: '#38a169', fontSize: '12px', marginTop: '2px' }}>
                  ✓ Đơn hàng được tự động gắn với tài khoản của bạn, tích điểm thưởng và gửi hóa đơn Gmail.
                </div>
              </div>
            </div>
          ) : (
            <div
              style={{
                background: requiresMember ? '#fff5f5' : '#fffaf0',
                border: `1px solid ${requiresMember ? '#feb2b2' : '#feebc8'}`,
                borderRadius: '12px',
                padding: '14px 18px',
                marginBottom: '20px',
              }}
            >
              {requiresMember ? (
                <div>
                  <div style={{ color: '#c53030', fontWeight: 700, fontSize: '14.5px', marginBottom: '4px' }}>
                    🔒 Bắt buộc tài khoản hội viên khi Giao tận nơi & Thanh toán Online
                  </div>
                  <p style={{ margin: '0 0 10px', fontSize: '13px', color: '#742a2a' }}>
                    Đơn hàng online yêu cầu tài khoản thành viên để đảm bảo hóa đơn điện tử tự động qua Gmail và quyền lợi tích điểm. Khách ăn tại quán hoặc thanh toán tại quầy vẫn có thể đặt tự do không cần đăng ký.
                  </p>
                  <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                    <Link
                      href="/dang-nhap?redirect=/thanh-toan"
                      style={{
                        background: '#8B2500',
                        color: '#fff',
                        textDecoration: 'none',
                        padding: '6px 14px',
                        borderRadius: '6px',
                        fontSize: '13px',
                        fontWeight: 650,
                      }}
                    >
                      Đăng nhập bằng SĐT ↗
                    </Link>
                    <Link
                      href="/dang-ky?redirect=/thanh-toan"
                      style={{
                        background: '#fff',
                        color: '#8B2500',
                        border: '1px solid #8B2500',
                        textDecoration: 'none',
                        padding: '6px 14px',
                        borderRadius: '6px',
                        fontSize: '13px',
                        fontWeight: 650,
                      }}
                    >
                      Đăng ký mới (Tặng 50 điểm & Giảm 10%) ↗
                    </Link>
                  </div>
                </div>
              ) : (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
                  <div style={{ fontSize: '13px', color: '#744210' }}>
                    💡 Bạn đang đặt món dạng <b>khách vãng lai</b>. Đăng ký thành viên để được <b>giảm 10%</b> và tích điểm thưởng!
                  </div>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <Link href="/dang-nhap?redirect=/thanh-toan" style={{ color: '#8B2500', fontWeight: 650, fontSize: '13px' }}>
                      Đăng nhập
                    </Link>
                    <span style={{ color: '#cbd5e0' }}>|</span>
                    <Link href="/dang-ky?redirect=/thanh-toan" style={{ color: '#8B2500', fontWeight: 650, fontSize: '13px', textDecoration: 'underline' }}>
                      Đăng ký ngay ↗
                    </Link>
                  </div>
                </div>
              )}
            </div>
          )}

          <div className="cart-section-title">
            <div>
              <span className="shop-eyebrow">BƯỚC 1</span>
              <h2>Thông tin liên hệ</h2>
            </div>
          </div>
          <div className="checkout-fields">
            <label>
              Họ và tên *
              <input
                required
                maxLength={100}
                value={form.name}
                onChange={(e) => change('name', e.target.value)}
                placeholder="Tên người nhận"
              />
            </label>
            <label>
              Số điện thoại *
              <input
                required
                type="tel"
                maxLength={15}
                value={form.phone}
                onChange={(e) => change('phone', e.target.value)}
                placeholder="09xxxxxxxx"
              />
            </label>
            <label>
              Email (nhận hóa đơn)
              <input
                type="email"
                maxLength={150}
                value={form.email}
                onChange={(e) => change('email', e.target.value)}
                placeholder="email@example.com"
              />
            </label>
          </div>

          <div className="cart-section-title checkout-subtitle">
            <div>
              <span className="shop-eyebrow">BƯỚC 2</span>
              <h2>Hình thức nhận món</h2>
            </div>
          </div>
          <div className="checkout-type-options">
            <label className={form.orderType === 'TAKEAWAY' ? 'chosen' : ''}>
              <input
                type="radio"
                name="orderType"
                checked={form.orderType === 'TAKEAWAY'}
                onChange={() => change('orderType', 'TAKEAWAY')}
              />
              <span>
                <Icon name="bag" size={25} />
              </span>
              <b>Đến lấy</b>
              <small>Nhận tại nhà hàng (cho phép khách vãng lai)</small>
            </label>
            <label className={form.orderType === 'DELIVERY' ? 'chosen' : ''}>
              <input
                type="radio"
                name="orderType"
                checked={form.orderType === 'DELIVERY'}
                onChange={() => change('orderType', 'DELIVERY')}
              />
              <span>
                <Icon name="truck" size={25} />
              </span>
              <b>Giao tận nơi</b>
              <small>Yêu cầu tài khoản thành viên</small>
            </label>
            <label className={form.orderType === 'DINE_IN' ? 'chosen' : ''}>
              <input
                type="radio"
                name="orderType"
                checked={form.orderType === 'DINE_IN'}
                onChange={() => change('orderType', 'DINE_IN')}
              />
              <span>
                <Icon name="utensils" size={25} />
              </span>
              <b>Dùng tại quán</b>
              <small>Thanh toán tại quầy (khách vãng lai OK)</small>
            </label>
          </div>
          {form.orderType === 'DELIVERY' && (
            <label className="checkout-full-field">
              Địa chỉ giao hàng *
              <textarea
                required
                minLength={8}
                maxLength={500}
                rows={3}
                value={form.address}
                onChange={(e) => change('address', e.target.value)}
                placeholder="Số nhà, đường, phường/xã, quận/huyện..."
              />
            </label>
          )}

          <div className="cart-section-title checkout-subtitle">
            <div>
              <span className="shop-eyebrow">BƯỚC 3</span>
              <h2>Phương thức thanh toán</h2>
            </div>
          </div>
          <div className="checkout-payment-options">
            <label>
              <input
                type="radio"
                name="payment"
                checked={form.payment === 'CASH'}
                onChange={() => change('payment', 'CASH')}
              />
              <span>
                <b>Thanh toán khi nhận món</b>
                <small>Tiền mặt tại quầy hoặc khi nhận hàng (khách vãng lai bình thường, không gửi mail).</small>
              </span>
            </label>
            <label>
              <input
                type="radio"
                name="payment"
                checked={form.payment === 'BANKING'}
                onChange={() => change('payment', 'BANKING')}
              />
              <span>
                <b>Chuyển khoản thủ công</b>
                <small>Nhân viên xác nhận đơn và hướng dẫn chuyển khoản.</small>
              </span>
            </label>
            <label className={vnpayEnabled ? (form.payment === 'VNPAY' ? 'chosen' : '') : 'checkout-disabled-option'}>
              <input
                type="radio"
                name="payment"
                disabled={!vnpayEnabled}
                checked={form.payment === 'VNPAY'}
                onChange={() => change('payment', 'VNPAY')}
              />
              <span>
                <b>VNPay (Thẻ ATM / Visa / MasterCard / QR)</b>
                <small>
                  {vnpayEnabled
                    ? 'Thanh toán online qua VNPay - yêu cầu tài khoản thành viên.'
                    : `Chưa khả dụng: ${vnpayWhy}`}
                </small>
              </span>
            </label>
            <label className={zalopayEnabled ? (form.payment === 'ZALOPAY' ? 'chosen' : '') : 'checkout-disabled-option'}>
              <input
                type="radio"
                name="payment"
                disabled={!zalopayEnabled}
                checked={form.payment === 'ZALOPAY'}
                onChange={() => change('payment', 'ZALOPAY')}
              />
              <span>
                <b>Ví ZaloPay (Ví ZaloPay / QR / Thẻ ngân hàng)</b>
                <small>
                  {zalopayEnabled
                    ? 'Thanh toán online qua ZaloPay - yêu cầu tài khoản thành viên.'
                    : `Chưa khả dụng: ${zalopayWhy}`}
                </small>
              </span>
            </label>
          </div>

          <label className="checkout-full-field">
            Ghi chú cho nhà hàng
            <textarea
              rows={3}
              maxLength={1000}
              value={form.note}
              onChange={(e) => change('note', e.target.value)}
              placeholder="Ví dụ: ít cay, không hành, thời gian nhận món..."
            />
          </label>

          {error && (
            <div className="checkout-error" role="alert">
              {error}
            </div>
          )}

          {form.payment === 'VNPAY' && (
            <div className="checkout-info">
              Sau khi xác nhận, bạn sẽ được chuyển sang cổng VNPay để thanh toán bằng thẻ ATM nội địa, Visa, MasterCard,
              JCB hoặc quét mã QR. Đơn chỉ được ghi nhận đã thanh toán khi VNPay xác nhận thành công.
            </div>
          )}
          {form.payment === 'ZALOPAY' && (
            <div className="checkout-info">
              Sau khi xác nhận, bạn sẽ được chuyển sang cổng ZaloPay để quét mã QR hoặc liên kết ví ZaloPay/thẻ ngân
              hàng. Đơn chỉ được ghi nhận đã thanh toán khi ZaloPay xác nhận thành công.
            </div>
          )}

          <button
            className="shop-button checkout-submit"
            type="submit"
            disabled={busy || !items.length}
          >
            {busy
              ? isOnline
                ? `Đang chuyển sang ${payLabel}...`
                : 'Đang gửi đơn...'
              : isOnline
              ? `Thanh toán ${payLabel} · ${vnd(total)}`
              : `Xác nhận đặt món · ${vnd(total)}`}{' '}
            <span>↗</span>
          </button>
          <p className="checkout-terms">
            Bằng cách gửi đơn, bạn đồng ý để nhà hàng liên hệ xác nhận thông tin. Khách dùng tại chỗ thanh toán tiền mặt
            sẽ không nhận email làm phiền.
          </p>
        </form>

        <aside className="cart-summary">
          <span className="shop-eyebrow">ĐƠN CỦA BẠN</span>
          <h2>{items.length} món đã chọn</h2>
          {items.map((item) => (
            <div className="checkout-order-row" key={`${item.id}-${item.note || ''}`}>
              <span>
                {item.name} <small>× {item.quantity}</small>
              </span>
              <b>{vnd(item.price * item.quantity)}</b>
            </div>
          ))}

          <div className="cart-summary-total" style={{ borderBottom: memberDiscount > 0 ? '1px dashed #e2e8f0' : 'none' }}>
            <span>Tạm tính</span>
            <strong>{vnd(rawSubtotal)}</strong>
          </div>

          {memberDiscount > 0 && (
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                padding: '8px 0',
                color: '#c53030',
                fontWeight: 650,
                fontSize: '14px',
              }}
            >
              <span>🎁 Ưu đãi Hội viên (-10%)</span>
              <span>- {vnd(memberDiscount)}</span>
            </div>
          )}

          <div
            className="cart-summary-total"
            style={{
              borderTop: '2px solid #8B2500',
              marginTop: '10px',
              paddingTop: '10px',
            }}
          >
            <span>Tổng thanh toán</span>
            <strong style={{ color: '#8B2500', fontSize: '20px' }}>{vnd(total)}</strong>
          </div>

          <p>
            {memberDiscount > 0
              ? '✓ Đã áp dụng ưu đãi giảm 10% đặc quyền dành cho thành viên Han Seoul.'
              : 'Đăng ký thành viên để nhận ngay 50 điểm thưởng và giảm 10% cho đơn hàng.'}
          </p>
          <Link className="cart-back-link" href="/gio-hang">
            ← Quay lại giỏ hàng
          </Link>
        </aside>
      </section>
    </ShopLayout>
  );
}
