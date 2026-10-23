'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import { ROLES, STATUS } from '@/lib/constants';

const inputStyle = {
  width: '100%',
  padding: '11px 12px',
  border: '1px solid #dcdfe6',
  borderRadius: 6,
  fontSize: 14,
  boxSizing: 'border-box',
};

export default function EmployeeAccessPage() {
  const router = useRouter();
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);
  const [login, setLogin] = useState({ username: '', password: '' });
  const [employee, setEmployee] = useState({
    fullName: '',
    username: '',
    password: '',
    role: 'WAITER',
    phone: '',
    salary: '',
  });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let savedUser = null;
    try {
      savedUser = JSON.parse(localStorage.getItem('currentUser') || 'null');
    } catch {
      localStorage.removeItem('currentUser');
    }

    if (
      localStorage.getItem('token')
      && savedUser?.role
      && savedUser.role !== 'CUSTOMER'
      && savedUser.accountType !== 'CUSTOMER'
    ) {
      // Restore the signed-in employee after browser hydration.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setUser(savedUser);
    } else if (savedUser?.accountType === 'CUSTOMER' || savedUser?.role === 'CUSTOMER') {
      localStorage.removeItem('token');
      localStorage.removeItem('currentUser');
    }
    setReady(true);
  }, []);

  useEffect(() => {
    const handleExpiredSession = () => {
      setUser(null);
      setError('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    };
    window.addEventListener('auth:expired', handleExpiredSession);
    return () => window.removeEventListener('auth:expired', handleExpiredSession);
  }, []);

  const clearSession = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    setUser(null);
    setError('');
    setMessage('');
  };

  const signIn = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError('');
    setMessage('');
    try {
      const result = await api('/auth/login', { method: 'POST', body: login });
      if (result.accountType === 'CUSTOMER') {
        setError('Đây là tài khoản khách hàng. Nhân viên vui lòng dùng tài khoản do quản lý cấp.');
        return;
      }
      if (!result.token) {
        throw new Error('Đăng nhập thành công nhưng máy chủ không trả về token. Vui lòng thử lại.');
      }

      localStorage.setItem('token', result.token);
      localStorage.setItem('currentUser', JSON.stringify({
        id: result.id,
        username: result.username,
        fullName: result.fullName,
        role: result.role,
        accountType: result.accountType,
      }));
      router.replace('/admin');
    } catch (requestError) {
      setError(requestError.message || 'Không thể đăng nhập');
    } finally {
      setBusy(false);
    }
  };

  const registerEmployee = async (event) => {
    event.preventDefault();
    if (user?.role !== 'MANAGER') {
      setError('Chỉ quản lý mới được tạo tài khoản và phân quyền nhân viên.');
      return;
    }

    setBusy(true);
    setError('');
    setMessage('');
    try {
      await api('/employees', {
        method: 'POST',
        body: { ...employee, salary: Number(employee.salary) },
      });
      setMessage(`Đã tạo hồ sơ và tài khoản cho ${employee.fullName}. Nhân viên có thể đăng nhập ngay.`);
      setEmployee({ fullName: '', username: '', password: '', role: 'WAITER', phone: '', salary: '' });
    } catch (requestError) {
      setError(requestError.message || 'Không thể đăng ký tài khoản nhân viên');
    } finally {
      setBusy(false);
    }
  };

  if (!ready) return null;

  const isManager = user?.role === 'MANAGER';

  return (
    <main style={{ minHeight: '100vh', background: '#f5f7fb', padding: 24, display: 'grid', placeItems: 'center' }}>
      <section style={{ width: '100%', maxWidth: 520, background: '#fff', borderRadius: 12, padding: 28, boxShadow: '0 8px 24px rgba(0,0,0,.08)' }}>
        <header style={{ marginBottom: 22, textAlign: 'center' }}>
          <h1 style={{ margin: 0, fontSize: 25, color: '#1a1f36' }}>Cổng nhân viên nhà hàng</h1>
          <p style={{ marginTop: 8, color: '#697386' }}>Đăng nhập để vào khu vực quản lý và làm việc.</p>
        </header>

        {(error || message) && (
          <div role={error ? 'alert' : 'status'} style={{
            padding: '10px 14px',
            marginBottom: 16,
            borderRadius: 6,
            color: error ? '#b42318' : '#067647',
            background: error ? '#fef3f2' : '#ecfdf3',
          }}>
            {error || message}
          </div>
        )}

        {user ? (
          <>
            <div style={{ padding: 14, background: '#f5f7fb', borderRadius: 8, marginBottom: 18 }}>
              Đang đăng nhập: <b>{user.fullName || user.username}</b> ({user.role})
            </div>
            {isManager ? (
              <>
                <h2 style={{ fontSize: 19, marginBottom: 6 }}>Đăng ký tài khoản nhân viên</h2>
                <p style={{ color: '#697386', marginBottom: 16, fontSize: 14 }}>
                  Tạo hồ sơ nhân viên, tài khoản đăng nhập và quyền làm việc trong một bước.
                </p>
                <form onSubmit={registerEmployee}>
                  <div style={{ display: 'grid', gap: 14 }}>
                    <label>
                      Họ và tên
                      <input style={inputStyle} required maxLength={100} value={employee.fullName}
                        onChange={(event) => setEmployee({ ...employee, fullName: event.target.value })} />
                    </label>
                    <label>
                      Tên đăng nhập
                      <input style={inputStyle} required minLength={3} maxLength={50} pattern="[A-Za-z0-9._-]+"
                        value={employee.username}
                        onChange={(event) => setEmployee({ ...employee, username: event.target.value })} />
                    </label>
                    <label>
                      Mật khẩu (ít nhất 8 ký tự)
                      <input style={inputStyle} type="password" required minLength={8} maxLength={72}
                        value={employee.password}
                        onChange={(event) => setEmployee({ ...employee, password: event.target.value })} />
                    </label>
                    <label>
                      Số điện thoại
                      <input style={inputStyle} type="tel" required maxLength={15} value={employee.phone}
                        onChange={(event) => setEmployee({ ...employee, phone: event.target.value })} />
                    </label>
                    <label>
                      Chức vụ và quyền
                      <select style={inputStyle} value={employee.role}
                        onChange={(event) => setEmployee({ ...employee, role: event.target.value })}>
                        {ROLES.map((role) => (
                          <option key={role} value={role}>{STATUS[role][0]} ({role})</option>
                        ))}
                      </select>
                    </label>
                    <label>
                      Lương (₫)
                      <input style={inputStyle} type="number" min="0" step="1000" required value={employee.salary}
                        onChange={(event) => setEmployee({ ...employee, salary: event.target.value })} />
                    </label>
                  </div>
                  <button type="submit" disabled={busy} style={{
                    width: '100%', marginTop: 20, padding: 12, border: 0, borderRadius: 6,
                    background: '#5469d4', color: '#fff', fontWeight: 600, cursor: busy ? 'wait' : 'pointer',
                  }}>
                    {busy ? 'Đang đăng ký...' : 'Đăng ký tài khoản nhân viên'}
                  </button>
                </form>
              </>
            ) : (
              <p style={{ color: '#697386' }}>
                Chỉ quản lý mới được đăng ký tài khoản mới hoặc đổi quyền. Liên hệ quản lý để được cấp tài khoản.
              </p>
            )}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 20, fontSize: 14 }}>
              <button type="button" onClick={() => router.push('/admin')} style={{ border: 0, background: 'none', color: '#5469d4', cursor: 'pointer' }}>
                Vào trang làm việc
              </button>
              <button type="button" onClick={clearSession} style={{ border: 0, background: 'none', color: '#b42318', cursor: 'pointer' }}>
                Đăng xuất
              </button>
            </div>
          </>
        ) : (
          <>
            <form onSubmit={signIn}>
              <div style={{ display: 'grid', gap: 16 }}>
                <label>
                  Tên đăng nhập
                  <input style={inputStyle} required autoComplete="username" value={login.username}
                    onChange={(event) => setLogin({ ...login, username: event.target.value })} />
                </label>
                <label>
                  Mật khẩu
                  <input style={inputStyle} type="password" required autoComplete="current-password" value={login.password}
                    onChange={(event) => setLogin({ ...login, password: event.target.value })} />
                </label>
              </div>
              <button type="submit" disabled={busy} style={{
                width: '100%', marginTop: 20, padding: 12, border: 0, borderRadius: 6,
                background: '#5469d4', color: '#fff', fontWeight: 600, cursor: busy ? 'wait' : 'pointer',
              }}>
                {busy ? 'Đang đăng nhập...' : 'Đăng nhập nhân viên'}
              </button>
            </form>
            <p style={{ marginTop: 16, fontSize: 13, color: '#697386', textAlign: 'center' }}>
              Chưa có tài khoản nhân viên? Liên hệ quản lý để được đăng ký và phân quyền.
            </p>
          </>
        )}
      </section>
    </main>
  );
}
