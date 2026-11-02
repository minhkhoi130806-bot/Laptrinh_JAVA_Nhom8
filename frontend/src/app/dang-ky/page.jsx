'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api } from '@/lib/api';

export default function RegisterPage() {
  const searchParams = useSearchParams();
  const redirectUrl = searchParams.get('redirect');

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
            loyaltyPoints: data.loyaltyPoints || 50,
          })
        );
        window.dispatchEvent(new Event('storage'));
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
          <p>Nhận ngay 50 điểm thưởng chào mừng, ưu đãi giảm 10% và tự động nhận hóa đơn qua Gmail.</p>
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
              Hệ thống đã gửi một <strong>email chào mừng</strong> kèm mã giảm giá 10% tới địa chỉ{' '}
              <b style={{ color: '#2d3748' }}>{success.email}</b>.
            </p>
            <div style={{ background: '#f7fafc', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '18px', textAlign: 'left', margin: '0 auto 28px', maxWidth: '440px', fontSize: '14px' }}>
              <p style={{ margin: '4px 0' }}>• <b>Số điện thoại hội viên:</b> {success.phone}</p>
              <p style={{ margin: '4px 0' }}>• <b>Email nhận hóa đơn:</b> {success.email}</p>
              <p style={{ margin: '4px 0', color: '#c53030' }}>• <b>Điểm thưởng khởi đầu:</b> +50 điểm chào mừng</p>
              <p style={{ margin: '4px 0', color: '#276749' }}>• <b>Ưu đãi hội viên:</b> Giảm ngay 10% cho đơn hàng trực tuyến đầu tiên.</p>
            </div>
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
              {redirectUrl ? (
                <Link className="shop-button" href={redirectUrl}>
                  Tiếp tục đơn hàng đang đặt <Icon name="arrowUpRight" size={17} />
                </Link>
              ) : (
                <Link className="shop-button" href="/thuc-don">
                  Khám phá thực đơn & Đặt món <Icon name="arrowUpRight" size={17} />
                </Link>
              )}
              <Link className="shop-outline-link" href="/tai-khoan">
                Xem trang cá nhân
              </Link>
            </div>
          </div>
        ) : (
          <form className="checkout-form" onSubmit={handleSubmit}>
            {/* BANNER ƯU ĐÃI THÀNH VIÊN MỚI */}
            <div style={{ background: 'linear-gradient(135deg, #fffaf0 0%, #feebc8 100%)', border: '1px solid #fbd38d', borderRadius: '12px', padding: '16px 20px', marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '14px' }}>
              <span style={{ fontSize: '32px' }}>🎁</span>
              <div>
                <strong style={{ color: '#744210', fontSize: '15px' }}>ƯU ĐÃI ĐẶC QUYỀN HỘI VIÊN MỚI</strong>
                <p style={{ margin: '3px 0 0', fontSize: '13px', color: '#975a16', lineHeight: '1.5' }}>
                  Tặng ngay <b>50 điểm thưởng</b> khi đăng ký thành công + <b>Giảm 10%</b> cho đơn hàng trực tuyến đầu tiên!
                </p>
              </div>
            </div>

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
                  placeholder="Ví dụ: 0912345678"
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
                  placeholder="Ví dụ: yourname@gmail.com"
                />
              </label>
            </div>

            <p style={{ fontSize: '13px', color: '#718096', lineHeight: '1.6', marginTop: '14px' }}>
              Hệ thống sẽ gửi email chào mừng ngay khi đăng ký, và gửi hóa đơn chi tiết mỗi khi bạn thanh toán trực tuyến.
            </p>

            {error && (
              <div
                style={{
                  background: '#fff5f5',
                  border: '1px solid #feb2b2',
                  borderRadius: '8px',
                  padding: '12px 16px',
                  color: '#c53030',
                  fontSize: '14px',
                  marginTop: '16px',
                }}
              >
                {error}
              </div>
            )}

            <div style={{ marginTop: '24px' }}>
              <button
                className="shop-button"
                type="submit"
                disabled={busy}
                style={{ width: '100%', justifyContent: 'center' }}
              >
                {busy ? 'Đang kích hoạt tài khoản...' : 'Hoàn tất đăng ký thành viên'} <Icon name="arrowUpRight" size={17} />
              </button>
            </div>

            <div style={{ marginTop: '20px', textAlign: 'center', fontSize: '14px', color: '#718096' }}>
              Đã có tài khoản hội viên?{' '}
              <Link href={`/dang-nhap${redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''}`} style={{ color: '#8B2500', fontWeight: 650 }}>
                Đăng nhập ngay ↗
              </Link>
            </div>
          </form>
        )}
      </section>
    </ShopLayout>
  );
}
