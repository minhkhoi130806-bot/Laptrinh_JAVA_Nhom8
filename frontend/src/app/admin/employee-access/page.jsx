'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import { ROLES, STATUS } from '@/lib/constants';
import Icon from '@/components/ui/Icon';
import '../../auth.css';

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
    <main className="auth-page">
      <aside className="auth-story">
        <div className="auth-brand"><span className="auth-brand-mark" aria-hidden="true">한</span><div><b>HAN SEOUL</b><small>KOREAN DINING</small></div></div>
        <div className="auth-story-content">
          <span className="auth-kicker">THE HAN SEOUL TEAM</span>
          <h2>Cùng nhau tạo nên<br/>{' '}những bữa ăn <em>đáng nhớ.</em></h2>
          <p>Một đội ngũ tận tâm. Một không gian ấm áp.<br/>Một trải nghiệm trọn vẹn cho mỗi vị khách.</p>
        </div>
        <div className="auth-story-art" aria-hidden="true"><span className="auth-art-ring"/><span className="auth-art-ring auth-art-ring-inner"/><div className="auth-art-seal"><Icon name="bowl" size={64}/><span>정성 · 맛 · 함께</span></div><span className="auth-art-caption">PREPARED WITH CARE</span></div>
        <div className="auth-story-footer"><span>HOSPITALITY, FROM THE HEART.</span><span>한 서울</span></div>
      </aside>
      <section className="auth-panel">
        <div className="auth-panel-inner">
        <header className="auth-heading">
          <span className="auth-heading-icon"><Icon name="shield" size={23}/></span>
          <span className="auth-kicker">KHÔNG GIAN LÀM VIỆC</span>
          <h1>Cổng nhân viên nhà hàng</h1>
          <p>Đăng nhập để vào khu vực quản lý và làm việc.</p>
        </header>

        {(error || message) && (
          <div role={error ? 'alert' : 'status'} className={`auth-notice ${error ? 'auth-notice-error' : 'auth-notice-success'}`}>
            {error || message}
          </div>
        )}

        {user ? (
          <>
            <div className="auth-session">
              <span className="auth-session-icon"><Icon name="user" size={20}/></span><div><small>Đang đăng nhập</small><b>{user.fullName || user.username}</b> <span>({user.role})</span></div>
            </div>
            {isManager ? (
              <>
                <h2 className="auth-subheading">Đăng ký tài khoản nhân viên</h2>
                <p className="auth-description">
                  Tạo hồ sơ nhân viên, tài khoản đăng nhập và quyền làm việc trong một bước.
                </p>
                <form onSubmit={registerEmployee} className="auth-form">
                  <div className="auth-fields auth-fields-registration">
                    <label className="auth-field">
                      Họ và tên
                      <input className="auth-input" required maxLength={100} value={employee.fullName}
                        onChange={(event) => setEmployee({ ...employee, fullName: event.target.value })} />
                    </label>
                    <label className="auth-field">
                      Tên đăng nhập
                      <input className="auth-input" required minLength={3} maxLength={50} pattern="[A-Za-z0-9._-]+"
                        value={employee.username}
                        onChange={(event) => setEmployee({ ...employee, username: event.target.value })} />
                    </label>
                    <label className="auth-field">
                      Mật khẩu (ít nhất 8 ký tự)
                      <input className="auth-input" type="password" required minLength={8} maxLength={72}
                        value={employee.password}
                        onChange={(event) => setEmployee({ ...employee, password: event.target.value })} />
                    </label>
                    <label className="auth-field">
                      Số điện thoại
                      <input className="auth-input" type="tel" required maxLength={15} value={employee.phone}
                        onChange={(event) => setEmployee({ ...employee, phone: event.target.value })} />
                    </label>
                    <label className="auth-field">
                      Chức vụ và quyền
                      <select className="auth-input" value={employee.role}
                        onChange={(event) => setEmployee({ ...employee, role: event.target.value })}>
                        {ROLES.map((role) => (
                          <option key={role} value={role}>{STATUS[role][0]} ({role})</option>
                        ))}
                      </select>
                    </label>
                    <label className="auth-field">
                      Lương (₫)
                      <input className="auth-input" type="number" min="0" step="1000" required value={employee.salary}
                        onChange={(event) => setEmployee({ ...employee, salary: event.target.value })} />
                    </label>
                  </div>
                  <button type="submit" disabled={busy} className="auth-submit">
                    {busy ? 'Đang đăng ký...' : 'Đăng ký tài khoản nhân viên'}
                    <Icon name="arrowRight" size={19}/>
                  </button>
                </form>
              </>
            ) : (
              <p className="auth-description">
                Chỉ quản lý mới được đăng ký tài khoản mới hoặc đổi quyền. Liên hệ quản lý để được cấp tài khoản.
              </p>
            )}
            <div className="auth-session-actions">
              <button type="button" onClick={() => router.push('/admin')} className="auth-link">
                Vào trang làm việc
                <Icon name="arrowUpRight" size={17}/>
              </button>
              <button type="button" onClick={clearSession} className="auth-link auth-link-danger">
                <Icon name="logout" size={17}/>
                Đăng xuất
              </button>
            </div>
          </>
        ) : (
          <>
            <form onSubmit={signIn} className="auth-form">
              <div className="auth-fields">
                <label className="auth-field">
                  Tên đăng nhập
                  <input className="auth-input" placeholder="Tên đăng nhập của bạn" required autoComplete="username" value={login.username}
                    onChange={(event) => setLogin({ ...login, username: event.target.value })} />
                </label>
                <label className="auth-field">
                  Mật khẩu
                  <input className="auth-input" placeholder="Nhập mật khẩu" type="password" required autoComplete="current-password" value={login.password}
                    onChange={(event) => setLogin({ ...login, password: event.target.value })} />
                </label>
              </div>
              <button type="submit" disabled={busy} className="auth-submit">
                {busy ? 'Đang đăng nhập...' : 'Đăng nhập nhân viên'}
                <Icon name="arrowRight" size={19}/>
              </button>
            </form>
            <p className="auth-help">
              Chưa có tài khoản nhân viên? Liên hệ quản lý để được đăng ký và phân quyền.
            </p>
          </>
        )}
        <footer className="auth-panel-footer"><span className="auth-footer-line"/><span>HAN SEOUL · KOREAN DINING</span><span className="auth-footer-line"/></footer>
        </div>
      </section>
    </main>
  );
}
