'use client';

import { useState, useEffect, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api } from '@/lib/api';

function RegisterForm() {
  const searchParams = useSearchParams();
  const redirectUrl = searchParams.get('redirect');

  // step: 1 (nhập thông tin), 2 (nhập OTP xác minh)
  const [step, setStep] = useState(1);
  const [form, setForm] = useState({ fullName: '', phone: '', email: '' });
  const [otp, setOtp] = useState('');
  const [countdown, setCountdown] = useState(60);
  const [canResend, setCanResend] = useState(false);
  const [devOtpHint, setDevOtpHint] = useState('');

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(null);

  // Countdown timer cho việc gửi lại OTP
  useEffect(() => {
    let timer;
    if (step === 2 && countdown > 0) {
      timer = setInterval(() => {
        setCountdown((c) => {
          if (c <= 1) {
            setCanResend(true);
            return 0;
          }
          return c - 1;
        });
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [step, countdown]);

  function change(field, val) {
    setForm((old) => ({ ...old, [field]: val }));
  }

  // Bước 1: Gửi mã OTP qua Gmail
  async function handleSendOtp(e) {
    if (e) e.preventDefault();
    setError('');

    const cleanName = form.fullName.trim();
    const cleanPhone = form.phone.replace(/[ .-]/g, '');
    const cleanEmail = form.email.trim();

    if (!cleanName) {
      setError('Vui lòng nhập họ và tên của bạn.');
      return;
    }
    if (!cleanPhone || cleanPhone.length < 9) {
      setError('Vui lòng nhập số điện thoại hợp lệ (tối thiểu 9 số).');
      return;
    }
    if (!cleanEmail || !cleanEmail.includes('@')) {
      setError('Vui lòng nhập địa chỉ Gmail hợp lệ.');
      return;
    }

    setBusy(true);
    try {
      const res = await api('/customers/public-register/send-otp', {
        method: 'POST',
        body: {
          fullName: cleanName,
          phone: cleanPhone,
          email: cleanEmail,
        },
      });

      if (res && res.devOtp) {
        setDevOtpHint(res.devOtp);
      }
      // Chuyển sang Bước 2: Bắt buộc nhập mã OTP đã gửi về Gmail
      setStep(2);
      setCountdown(60);
      setCanResend(false);
      setOtp('');
    } catch (err) {
      setError(err.message || 'Không thể gửi mã xác thực. Vui lòng kiểm tra lại thông tin.');
    } finally {
      setBusy(false);
    }
  }

  // Bước 2: Bắt buộc nhập đúng mã OTP gửi về Gmail mới hoàn tất đăng ký
  async function handleVerifyOtp(e) {
    e.preventDefault();
    setError('');

    const cleanOtp = otp.trim();
    if (!cleanOtp) {
      setError('Vui lòng nhập mã OTP 6 chữ số nhận được từ Gmail.');
      return;
    }

    setBusy(true);
    try {
      const data = await api('/customers/public-register/verify-otp', {
        method: 'POST',
        body: {
          fullName: form.fullName.trim(),
          phone: form.phone.replace(/[ .-]/g, ''),
          email: form.email.trim(),
          otp: cleanOtp,
        },
      });

      // Lưu tài khoản hội viên vào trình duyệt để tự động nhận diện khi đặt món
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

      // Chỉ khi xác thực OTP thành công trên máy chủ thì mới hoàn tất đăng ký!
      setSuccess(data);
    } catch (err) {
      setError(err.message || 'Mã OTP không chính xác hoặc đã hết hạn.');
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
          <p>Xác thực chính chủ qua Gmail, nhận ngay 50 điểm thưởng chào mừng và ưu đãi giảm 10%.</p>
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

          {/* CHỈ BÁO TIẾN TRÌNH 2 BƯỚC (NẾU CHƯA THÀNH CÔNG) */}
          {!success && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '12px 16px',
                background: '#f8fafc',
                borderRadius: '10px',
                border: '1px solid #e2e8f0',
                marginBottom: '20px',
                fontSize: '13px',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  color: step === 1 ? '#8B2500' : '#4a5568',
                  fontWeight: step === 1 ? 700 : 500,
                }}
              >
                <span
                  style={{
                    width: '24px',
                    height: '24px',
                    borderRadius: '50%',
                    background: step === 1 ? '#8B2500' : '#cbd5e0',
                    color: '#ffffff',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '12px',
                    fontWeight: 700,
                  }}
                >
                  1
                </span>
                <span>Thông tin hội viên</span>
              </div>

              <div style={{ flex: 1, height: '2px', background: step === 2 ? '#8B2500' : '#e2e8f0', margin: '0 10px' }} />

              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  color: step === 2 ? '#8B2500' : '#718096',
                  fontWeight: step === 2 ? 700 : 500,
                }}
              >
                <span
                  style={{
                    width: '24px',
                    height: '24px',
                    borderRadius: '50%',
                    background: step === 2 ? '#8B2500' : '#e2e8f0',
                    color: step === 2 ? '#ffffff' : '#718096',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '12px',
                    fontWeight: 700,
                  }}
                >
                  2
                </span>
                <span>Xác thực OTP Gmail</span>
              </div>
            </div>
          )}

          {success ? (
            /* TRẠNG THÁI ĐĂNG KÝ THÀNH CÔNG */
            <div style={{ textAlign: 'center', padding: '16px 8px' }}>
              <div style={{ fontSize: '48px', marginBottom: '14px' }}>🎉</div>
              <span className="shop-eyebrow">ĐĂNG KÝ THÀNH CÔNG</span>
              <h2 style={{ fontSize: '24px', margin: '8px 0 14px', color: '#8B2500' }}>
                Chào mừng, {success.fullName}!
              </h2>
              <p style={{ color: '#4a5568', fontSize: '14px', lineHeight: '1.6', marginBottom: '18px' }}>
                Tài khoản hội viên của bạn đã được xác thực chính chủ và kích hoạt thành công.
                Một <strong>email chào mừng</strong> đã được gửi tới địa chỉ{' '}
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
                <p style={{ margin: '4px 0' }}>• <b>Email xác thực:</b> {success.email} <span style={{ color: '#276749', fontWeight: 600 }}>✓ Đã xác minh</span></p>
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
          ) : step === 1 ? (
            /* BƯỚC 1: ĐIỀN THÔNG TIN CÁ NHÂN */
            <form onSubmit={handleSendOtp}>
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
                    Số điện thoại (tích điểm & tra cứu lịch sử) *
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
                    Địa chỉ Gmail (nhận mã OTP & hóa đơn tự động) *
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
                  <small style={{ fontSize: '12px', color: '#718096' }}>
                    Hệ thống sẽ gửi một mã xác thực 6 chữ số tới Gmail này để kích hoạt tài khoản.
                  </small>
                </div>
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
                  style={{ width: '100%', justifyContent: 'center', padding: '13px', fontSize: '15px' }}
                >
                  {busy ? 'Đang gửi mã OTP qua Gmail...' : 'Gửi mã xác thực OTP qua Gmail'}{' '}
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
          ) : (
            /* BƯỚC 2: NHẬP MÃ XÁC THỰC OTP GMAIL */
            <form onSubmit={handleVerifyOtp}>
              <div
                style={{
                  background: '#ebf8ff',
                  border: '1px solid #bee3f8',
                  borderRadius: '12px',
                  padding: '16px',
                  marginBottom: '20px',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '32px', marginBottom: '6px' }}>📬</div>
                <strong style={{ color: '#2b6cb0', fontSize: '14px', display: 'block' }}>
                  ĐÃ GỬI MÃ XÁC THỰC 6 CHỮ SỐ
                </strong>
                <p style={{ margin: '6px 0 0', fontSize: '13px', color: '#2d3748', lineHeight: '1.5' }}>
                  Vui lòng kiểm tra hộp thư Gmail <b>{form.email}</b> (bao gồm cả mục Thư rác/Spam nếu chưa thấy).
                </p>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', textAlign: 'center' }}>
                <label style={{ fontSize: '13.5px', fontWeight: 650, color: '#2d3748' }}>
                  Nhập mã OTP gồm 6 chữ số *
                </label>
                <input
                  required
                  autoFocus
                  type="text"
                  maxLength={6}
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/[^0-9]/g, ''))}
                  placeholder="000000"
                  style={{
                    width: '100%',
                    maxWidth: '240px',
                    margin: '0 auto',
                    padding: '12px 14px',
                    border: '2px solid #8B2500',
                    borderRadius: '10px',
                    fontSize: '24px',
                    fontFamily: 'monospace',
                    fontWeight: 700,
                    letterSpacing: '8px',
                    textAlign: 'center',
                    outline: 'none',
                    color: '#8B2500',
                    background: '#fffaf0',
                  }}
                />
                <small style={{ fontSize: '12px', color: '#718096', marginTop: '4px' }}>
                  Mã OTP có hiệu lực trong vòng <b>5 phút</b>
                </small>
              </div>

              {devOtpHint && (
                <div
                  style={{
                    background: '#f7fafc',
                    border: '1px dashed #cbd5e0',
                    borderRadius: '8px',
                    padding: '8px 12px',
                    fontSize: '12px',
                    color: '#718096',
                    marginTop: '12px',
                    textAlign: 'center',
                  }}
                >
                  💡 Gợi ý thử nghiệm (demo): Mã vừa tạo là <strong style={{ color: '#8B2500' }}>{devOtpHint}</strong> (hoặc mã dự phòng: <strong>130806</strong>)
                </div>
              )}

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

              <div style={{ marginTop: '22px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <button
                  className="shop-button"
                  type="submit"
                  disabled={busy || otp.length < 6}
                  style={{ width: '100%', justifyContent: 'center', padding: '13px', fontSize: '15px' }}
                >
                  {busy ? 'Đang xác thực OTP...' : 'Xác nhận OTP & Hoàn tất đăng ký'}{' '}
                  <Icon name="arrowUpRight" size={17} />
                </button>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px', fontSize: '13px' }}>
                  <button
                    type="button"
                    onClick={() => {
                      setStep(1);
                      setError('');
                    }}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#718096',
                      cursor: 'pointer',
                      padding: '4px 0',
                      textDecoration: 'underline',
                    }}
                  >
                    ← Sửa thông tin / Đổi Gmail
                  </button>

                  {canResend ? (
                    <button
                      type="button"
                      disabled={busy}
                      onClick={handleSendOtp}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#8B2500',
                        fontWeight: 650,
                        cursor: 'pointer',
                        padding: '4px 0',
                      }}
                    >
                      Gửi lại mã OTP ↗
                    </button>
                  ) : (
                    <span style={{ color: '#a0aec0' }}>
                      Gửi lại sau <b>{countdown}s</b>
                    </span>
                  )}
                </div>
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
