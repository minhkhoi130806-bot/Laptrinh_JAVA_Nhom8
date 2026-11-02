'use client';

import { useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api } from '@/lib/api';

export default function RegisterPage() {
  const [form, setForm] = useState({ fullName: '', phone: '', email: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(null);

  function change(field, val) {
    setForm((old) => ({ ...old, [field]: val }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);

    try {
      const data = await api('/customers/public-register', {
        method: 'POST',
        body: {
          fullName: form.fullName.trim(),
          phone: form.phone.replace(/[ .-]/g, ''),
          email: form.email.trim(),
        },
      });

      // Lưu tài khoản vào trình duyệt để tự động nhận diện khi đặt món
      try {
        localStorage.setItem(
          'currentUser',
          JSON.stringify({
            id: data.id,
            fullName: data.fullName,
            name: data.fullName,
            phone: data.phone,
            email: data.email,
            loyaltyPoints: data.loyaltyPoints || 0,
          })
        );
      } catch {}

      setSuccess(data);
    } catch (err) {
      setError(err.message || 'Không thể đăng ký. Vui lòng kiểm tra lại thông tin.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <ShopLayout>
      <section className="shop-page-hero">
        <div className="shop-container">
          <span className="shop-eyebrow">HAN SEOUL MEMBERSHIP</span>
          <h1>
            Đăng ký <em>thành viên</em>
          </h1>
          <p>Nhận ngay đặc quyền tích điểm và tự động nhận hóa đơn điện tử qua Gmail.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <Link href="/tai-khoan">Tài khoản</Link>
            <span>/</span>
            <span>Đăng ký</span>
          </div>
        </div>
      </section>

      <section className="shop-section shop-container checkout-layout" style={{ maxWidth: '680px', margin: '0 auto' }}>
        {success ? (
          <div className="checkout-form" style={{ textAlign: 'center', padding: '40px 24px' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎉</div>
            <span className="shop-eyebrow">ĐĂNG KÝ THÀNH CÔNG</span>
            <h2 style={{ fontSize: '26px', margin: '8px 0 16px', color: '#8B2500' }}>
              Chào mừng, {success.fullName}!
            </h2>
            <p style={{ color: '#4a5568', fontSize: '15px', lineHeight: '1.7', marginBottom: '20px' }}>
              Hệ thống đã gửi một <strong>email chào mừng</strong> kèm thông tin đặc quyền hội viên tới địa chỉ{' '}
              <b style={{ color: '#2d3748' }}>{success.email}</b>.
            </p>
            <div style={{ background: '#f7fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '16px', textAlign: 'left', margin: '0 auto 28px', maxWidth: '420px', fontSize: '14px' }}>
              <p style={{ margin: '4px 0' }}>• <b>Số điện thoại tích điểm:</b> {success.phone}</p>
              <p style={{ margin: '4px 0' }}>• <b>Email nhận hóa đơn:</b> {success.email}</p>
              <p style={{ margin: '4px 0' }}>• <b>Đặc quyền:</b> Khi bạn thanh toán qua ZaloPay/VNPay/MoMo, hóa đơn chi tiết sẽ được tự động gửi về email này.</p>
            </div>
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
              <Link className="shop-button" href="/thuc-don">
                Khám phá thực đơn & Đặt món <Icon name="arrowUpRight" size={17} />
              </Link>
              <Link className="shop-outline-link" href="/tai-khoan">
                Xem trang cá nhân
              </Link>
            </div>
          </div>
        ) : (
          <form className="checkout-form" onSubmit={handleSubmit}>
            <div className="cart-section-title">
              <div>
                <span className="shop-eyebrow">THÔNG TIN HỘI VIÊN</span>
                <h2>Tạo tài khoản mới</h2>
              </div>
            </div>

            <div className="checkout-fields" style={{ gridTemplateColumns: '1fr', gap: '18px' }}>
              <label>
                Họ và tên *
                <input
                  required
                  maxLength={100}
                  value={form.fullName}
                  onChange={(e) => change('fullName', e.target.value)}
                  placeholder="Ví dụ: Nguyễn Văn A"
                />
              </label>

              <label>
                Số điện thoại (dùng để tích điểm và tra cứu đơn) *
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
                Địa chỉ Gmail (nhận thư chào mừng & hóa đơn tự động) *
                <input
                  required
                  type="email"
                  maxLength={150}
                  value={form.email}
                  onChange={(e) => change('email', e.target.value)}
                  placeholder="email_cua_ban@gmail.com"
                />
                <small style={{ color: '#718096', fontSize: '12.5px', marginTop: '4px', display: 'block' }}>
                  Hệ thống sẽ gửi email chào mừng ngay khi đăng ký, và gửi hóa đơn chi tiết mỗi khi bạn thanh toán.
                </small>
              </label>
            </div>

            {error && (
              <p className="checkout-error" role="alert" style={{ marginTop: '16px' }}>
                {error}
              </p>
            )}

            <button
              className="shop-button checkout-submit"
              type="submit"
              disabled={busy}
              style={{ marginTop: '24px' }}
            >
              {busy ? 'Đang kích hoạt tài khoản...' : 'Hoàn tất đăng ký thành viên'} <span>↗</span>
            </button>

            <p style={{ textAlign: 'center', marginTop: '16px', fontSize: '14px', color: '#718096' }}>
              Đã có tài khoản?{' '}
              <Link href="/tai-khoan" style={{ color: '#8B2500', fontWeight: 600 }}>
                Quay lại trang tài khoản
              </Link>
            </p>
          </form>
        )}
      </section>
    </ShopLayout>
  );
}
