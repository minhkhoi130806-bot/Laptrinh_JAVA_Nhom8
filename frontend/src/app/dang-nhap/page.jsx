'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api } from '@/lib/api';

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirectUrl = searchParams.get('redirect') || '/tai-khoan';

  const [phone, setPhone] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function handleLogin(e) {
    e.preventDefault();
    setError('');
    const cleanPhone = phone.replace(/[ .-]/g, '').trim();

    if (!cleanPhone) {
      setError('Vui lòng nhập số điện thoại hoặc email đã đăng ký.');
      return;
    }

    setBusy(true);
    try {
      const data = await api('/customers/public-login', {
        method: 'POST',
        body: { phone: cleanPhone },
      });

      // Lưu tài khoản vào localStorage
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
        window.dispatchEvent(new Event('storage'));
      } catch {}

      router.push(redirectUrl);
    } catch (err) {
      setError(err.message || 'Không tìm thấy tài khoản. Vui lòng kiểm tra lại thông tin hoặc đăng ký mới.');
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
            Đăng nhập <em>hội viên</em>
          </h1>
          <p>Nhận diện tài khoản để tích điểm đổi quà và tự động nhận hóa đơn qua Gmail.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <Link href="/tai-khoan">Tài khoản</Link>
            <span>/</span>
            <span>Đăng nhập</span>
          </div>
        </div>
      </section>

      <section className="shop-auth-wrapper">
        <div className="shop-auth-card">
          {/* TABS CHUYỂN ĐỔI NHANH GIỮA ĐĂNG NHẬP VÀ ĐĂNG KÝ */}
          <div className="shop-auth-tabs">
            <span className="shop-auth-tab active">Đăng nhập</span>
            <Link
              href={`/dang-ky${redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''}`}
              className="shop-auth-tab"
            >
              Đăng ký mới
            </Link>
          </div>

          <form onSubmit={handleLogin}>
            <div style={{ textAlign: 'center', marginBottom: '24px' }}>
              <div style={{ fontSize: '38px', marginBottom: '8px' }}>🎖️</div>
              <h2 style={{ fontSize: '22px', margin: '0 0 6px', color: '#1a202c' }}>
                Chào mừng bạn trở lại!
              </h2>
              <p style={{ color: '#718096', fontSize: '13.5px', margin: 0 }}>
                Nhập số điện thoại (hoặc email) bạn đã đăng ký thành viên.
              </p>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '18px' }}>
              <label style={{ fontSize: '13.5px', fontWeight: 600, color: '#2d3748' }}>
                Số điện thoại hội viên *
              </label>
              <input
                required
                type="text"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="Ví dụ: 0912345678"
                autoFocus
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  border: '1px solid #cbd5e0',
                  borderRadius: '10px',
                  fontSize: '14px',
                  outline: 'none',
                }}
              />
            </div>

            {error && (
              <div
                style={{
                  background: '#fff5f5',
                  border: '1px solid #feb2b2',
                  borderRadius: '10px',
                  padding: '12px 16px',
                  color: '#c53030',
                  fontSize: '13.5px',
                  marginBottom: '18px',
                }}
              >
                <p style={{ margin: '0 0 6px' }}>{error}</p>
                <Link
                  href={`/dang-ky${redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''}`}
                  style={{ color: '#8B2500', fontWeight: 700, textDecoration: 'underline' }}
                >
                  👉 Chưa có tài khoản? Đăng ký ngay để nhận 50 điểm & giảm 10%!
                </Link>
              </div>
            )}

            <button
              className="shop-button"
              type="submit"
              disabled={busy}
              style={{ width: '100%', justifyContent: 'center', padding: '12px', fontSize: '15px' }}
            >
              {busy ? 'Đang kiểm tra...' : 'Đăng nhập ngay'} <Icon name="arrowUpRight" size={17} />
            </button>

            <div
              style={{
                marginTop: '22px',
                paddingTop: '18px',
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
                Chưa có tài khoản?{' '}
                <Link
                  href={`/dang-ky${redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''}`}
                  style={{ color: '#8B2500', fontWeight: 700 }}
                >
                  Đăng ký ngay (30 giây) ↗
                </Link>
              </span>
            </div>
          </form>
        </div>
      </section>
    </ShopLayout>
  );
}
