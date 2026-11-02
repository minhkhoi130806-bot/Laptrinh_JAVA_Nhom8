'use client';

import { useState, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api } from '@/lib/api';

function RegisterForm() {
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

      <section className="shop-auth-wrapper">
        <div className="shop-auth-card" style={{ maxWidth: '520px' }}>
          {/* TABS CHUYỂN ĐỔI NHANH */}
          <div className="shop-auth-tabs">
            <Link
              href={`/dang-nhap${redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''}`}
              className="shop-auth-tab"
            >
              Đăng nhập
            </Link>
            <span className="shop-auth-tab active">Đăng ký mới</span>
          </div>

          {success ? (
            <div style={{ textAlign: 'center', padding: '16px 8px' }}>
              <div style={{ fontSize: '48px', marginBottom: '14px' }}>🎉</div>
              <span className="shop-eyebrow">ĐĂNG KÝ THÀNH CÔNG</span>
              <h2 style={{ fontSize: '24px', margin: '8px 0 14px', color: '#8B2500' }}>
                Chào mừng, {success.fullName}!
              </h2>
              <p style={{ color: '#4a5568', fontSize: '14px', lineHeight: '1.6', marginBottom: '18px' }}>
                Hệ thống đã gửi một <strong>email chào mừng</strong> kèm mã giảm giá 10% tới địa chỉ{' '}
                <b style={{ color: '#2d3748' }}>{success.email}</b>.
              </p>
              <div
                style={{
                  background: '#f8fafc',
                  border: '1px solid #e2e8f0',
                  borderRadius: '12px',
                  padding: '16px',
                  textAlign: 'left',
                  margin: '0 auto 24px',
                  fontSize: '13.5px',
                }}
              >
                <p style={{ margin: '4px 0' }}>• <b>Số điện thoại:</b> {success.phone}</p>
                <p style={{ margin: '4px 0' }}>• <b>Email nhận hóa đơn:</b> {success.email}</p>
                <p style={{ margin: '4px 0', color: '#c53030' }}>• <b>Điểm thưởng khởi đầu:</b> +50 điểm chào mừng</p>
                <p style={{ margin: '4px 0', color: '#276749' }}>• <b>Ưu đãi:</b> Giảm ngay 10% cho đơn hàng trực tuyến đầu tiên.</p>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '20px' }}>
                <Link
                  className="shop-button"
                  href="/"
                  style={{
                    width: '100%',
                    justifyContent: 'center',
                    fontSize: '15px',
                    padding: '14px 20px',
                    borderRadius: '10px',
                    background: '#8B2500',
                    color: '#ffffff',
                    fontWeight: 700,
                  }}
                >
                  Vào trang web khám phá ngay <Icon name="arrowUpRight" size={18} />
                </Link>

                <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap', marginTop: '4px' }}>
                  {redirectUrl ? (
                    <Link className="shop-button shop-button-light" href={redirectUrl}>
                      Tiếp tục đơn hàng đang đặt ↗
                    </Link>
                  ) : (
                    <Link className="shop-button shop-button-light" href="/thuc-don">
                      Khám phá thực đơn ↗
                    </Link>
                  )}
                  <Link className="shop-outline-link" href="/tai-khoan">
                    Vào trang cá nhân của bạn ↗
                  </Link>
                </div>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              {/* BANNER ƯU ĐÃI THÀNH VIÊN MỚI */}
              <div
                style={{
                  background: 'linear-gradient(135deg, #fffaf0 0%, #feebc8 100%)',
                  border: '1px solid #fbd38d',
                  borderRadius: '12px',
                  padding: '14px 16px',
                  marginBottom: '20px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                }}
              >
                <span style={{ fontSize: '28px' }}>🎁</span>
                <div>
                  <strong style={{ color: '#744210', fontSize: '13.5px' }}>ƯU ĐÃI ĐẶC QUYỀN HỘI VIÊN MỚI</strong>
                  <p style={{ margin: '2px 0 0', fontSize: '12.5px', color: '#975a16', lineHeight: '1.4' }}>
                    Tặng ngay <b>50 điểm thưởng</b> + <b>Giảm 10%</b> cho đơn hàng trực tuyến đầu tiên!
                  </p>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '13.5px', fontWeight: 600, color: '#2d3748' }}>
                    Họ và tên *
                  </label>
                  <input
                    required
                    maxLength={100}
                    value={form.fullName}
                    onChange={(e) => change('fullName', e.target.value)}
                    placeholder="Ví dụ: Nguyễn Văn A"
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      border: '1px solid #cbd5e0',
                      borderRadius: '10px',
                      fontSize: '14px',
                      outline: 'none',
                    }}
                  />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '13.5px', fontWeight: 600, color: '#2d3748' }}>
                    Số điện thoại (dùng để tích điểm & nhận diện) *
                  </label>
                  <input
                    required
                    type="tel"
                    maxLength={15}
                    value={form.phone}
                    onChange={(e) => change('phone', e.target.value)}
                    placeholder="Ví dụ: 0912345678"
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      border: '1px solid #cbd5e0',
                      borderRadius: '10px',
                      fontSize: '14px',
                      outline: 'none',
                    }}
                  />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <label style={{ fontSize: '13.5px', fontWeight: 600, color: '#2d3748' }}>
                    Địa chỉ Gmail (nhận thư chào mừng & hóa đơn tự động) *
                  </label>
                  <input
                    required
                    type="email"
                    maxLength={150}
                    value={form.email}
                    onChange={(e) => change('email', e.target.value)}
                    placeholder="Ví dụ: yourname@gmail.com"
                    style={{
                      width: '100%',
                      padding: '11px 14px',
                      border: '1px solid #cbd5e0',
                      borderRadius: '10px',
                      fontSize: '14px',
                      outline: 'none',
                    }}
                  />
                </div>
              </div>

              <p style={{ fontSize: '12.5px', color: '#718096', lineHeight: '1.5', marginTop: '14px' }}>
                Hệ thống sẽ gửi email chào mừng ngay khi đăng ký, và gửi hóa đơn chi tiết mỗi khi bạn thanh toán trực tuyến.
              </p>

              {error && (
                <div
                  style={{
                    background: '#fff5f5',
                    border: '1px solid #feb2b2',
                    borderRadius: '10px',
                    padding: '12px 16px',
                    color: '#c53030',
                    fontSize: '13.5px',
                    marginTop: '16px',
                  }}
                >
                  {error}
                </div>
              )}

              <div style={{ marginTop: '22px' }}>
                <button
                  className="shop-button"
                  type="submit"
                  disabled={busy}
                  style={{ width: '100%', justifyContent: 'center', padding: '12px', fontSize: '15px' }}
                >
                  {busy ? 'Đang kích hoạt tài khoản...' : 'Hoàn tất đăng ký thành viên'}{' '}
                  <Icon name="arrowUpRight" size={17} />
                </button>
              </div>

              <div
                style={{
                  marginTop: '18px',
                  paddingTop: '16px',
                  borderTop: '1px dashed #e2e8f0',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  fontSize: '13.5px',
                  color: '#718096',
                  flexWrap: 'wrap',
                  gap: '10px',
                }}
              >
                <Link
                  href="/"
                  style={{ color: '#718096', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                >
                  ← Vào trang web
                </Link>
                <span>
                  Đã có tài khoản?{' '}
                  <Link
                    href={`/dang-nhap${redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''}`}
                    style={{ color: '#8B2500', fontWeight: 650 }}
                  >
                    Đăng nhập ngay ↗
                  </Link>
                </span>
              </div>
            </form>
          )}
        </div>
      </section>
    </ShopLayout>
  );
}

export default function RegisterPage() {
  return (
    <Suspense fallback={null}>
      <RegisterForm />
    </Suspense>
  );
}
