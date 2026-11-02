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

      <section className="shop-section shop-container checkout-layout" style={{ maxWidth: '580px', margin: '0 auto' }}>
        <form className="checkout-form" onSubmit={handleLogin} style={{ padding: '36px 30px' }}>
          <div style={{ textAlign: 'center', marginBottom: '24px' }}>
            <div style={{ fontSize: '42px', marginBottom: '12px' }}>🎖️</div>
            <h2 style={{ fontSize: '24px', margin: '0 0 8px', color: '#8B2500' }}>
              Chào mừng bạn trở lại!
            </h2>
            <p style={{ color: '#718096', fontSize: '14px', margin: 0 }}>
              Nhập số điện thoại (hoặc email) bạn đã đăng ký thành viên.
            </p>
          </div>

          <div className="checkout-fields" style={{ gridTemplateColumns: '1fr', gap: '16px' }}>
            <label>
              Số điện thoại hội viên *
              <input
                required
                type="text"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="Ví dụ: 0912345678"
                autoFocus
              />
            </label>
          </div>

          {error && (
            <div
              style={{
                background: '#fff5f5',
                border: '1px solid #feb2b2',
                borderRadius: '8px',
                padding: '12px 16px',
                color: '#c53030',
                fontSize: '13.5px',
                marginTop: '16px',
              }}
            >
              <p style={{ margin: '0 0 6px' }}>{error}</p>
              <Link
                href={`/dang-ky${redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''}`}
                style={{ color: '#8B2500', fontWeight: 700, textDecoration: 'underline' }}
              >
                👉 Bạn chưa có tài khoản? Đăng ký ngay để nhận 50 điểm thưởng & giảm 10%!
              </Link>
            </div>
          )}

          <div style={{ marginTop: '24px' }}>
            <button
              className="shop-button"
              type="submit"
              disabled={busy}
              style={{ width: '100%', justifyContent: 'center' }}
            >
              {busy ? 'Đang kiểm tra...' : 'Đăng nhập ngay'} <Icon name="arrowUpRight" size={17} />
            </button>
          </div>

          <div
            style={{
              marginTop: '24px',
              paddingTop: '20px',
              borderTop: '1px dashed #e2e8f0',
              textAlign: 'center',
              fontSize: '14px',
              color: '#718096',
            }}
          >
            Chưa có tài khoản hội viên?{' '}
            <Link
              href={`/dang-ky${redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : ''}`}
              style={{ color: '#8B2500', fontWeight: 700 }}
            >
              Đăng ký thành viên mới ↗
            </Link>
          </div>
        </form>
      </section>
    </ShopLayout>
  );
}
