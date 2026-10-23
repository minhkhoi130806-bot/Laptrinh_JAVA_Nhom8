'use client';

import { useState } from 'react';
import { api } from '@/lib/api';
import { ROLES, STATUS } from '@/lib/constants';
import { useApi } from '@/lib/hooks';
import { useToast } from './Feedback';
import PageHeader from './PageHeader';
import StateBox from './StateBox';

export default function EmployeeAccountManager() {
  const toast = useToast();
  const { data: employees, error, loading, reload, setData } = useApi('/employees?scope=ACTIVE');
  const [employeeId, setEmployeeId] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('WAITER');
  const [saving, setSaving] = useState(false);
  const selectedEmployee = employees?.find((employee) => String(employee.id) === employeeId);

  const submit = async (event) => {
    event.preventDefault();
    if (!employeeId) {
      toast('Vui lòng chọn nhân viên', 'err');
      return;
    }

    setSaving(true);
    try {
      const savedAccount = await api('/auth/employee-account', {
        method: 'POST',
        body: { employeeId: Number(employeeId), username, password, role },
      });
      toast('Đã cấp/cập nhật tài khoản nhân viên');
      setPassword('');
      setData((current) => current?.map((employee) => (
        employee.id === savedAccount.employeeId
          ? { ...employee, username: savedAccount.username, role: savedAccount.role }
          : employee
      )));
    } catch (requestError) {
      toast(requestError.message, 'err');
    } finally {
      setSaving(false);
    }
  };

  return (
    <section style={{ marginBottom: 24 }}>
      <PageHeader
        title="Cấp tài khoản đăng nhập"
        icon="🔐"
        subtitle="Chỉ quản lý mới được tạo tài khoản nhân viên và phân quyền."
      />
      <StateBox loading={loading} error={error} onRetry={reload} />
      {!loading && !error && (
        <form onSubmit={submit} className="adm-card adm-section">
          <div className="adm-form-grid">
            <label className="adm-field">
              <span className="adm-label">Nhân viên</span>
              <select
                className="adm-input"
                required
                value={employeeId}
                onChange={(event) => {
                  const nextId = event.target.value;
                  const employee = employees.find((item) => String(item.id) === nextId);
                  setEmployeeId(nextId);
                  setUsername(employee?.username || '');
                  setRole(employee?.role || 'WAITER');
                }}
              >
                <option value="">— Chọn nhân viên —</option>
                {(employees || []).map((employee) => (
                  <option key={employee.id} value={employee.id}>
                    {employee.fullName} — {employee.username || 'chưa có tài khoản'}
                  </option>
                ))}
              </select>
            </label>

            <label className="adm-field">
              <span className="adm-label">Tên đăng nhập</span>
              <input
                className="adm-input"
                required
                minLength={3}
                maxLength={50}
                pattern="[A-Za-z0-9._-]+"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
              />
            </label>

            <label className="adm-field">
              <span className="adm-label">Mật khẩu mới (ít nhất 8 ký tự)</span>
              <input
                className="adm-input"
                type="password"
                required
                minLength={8}
                maxLength={72}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>

            <label className="adm-field">
              <span className="adm-label">Quyền đăng nhập</span>
              <select className="adm-input" value={role} onChange={(event) => setRole(event.target.value)}>
                {ROLES.map((value) => (
                  <option key={value} value={value}>{STATUS[value][0]} ({value})</option>
                ))}
              </select>
            </label>
          </div>

          {selectedEmployee?.username && (
            <p className="adm-muted" style={{ marginTop: 12 }}>
              Nhân viên đã có tài khoản; lưu form sẽ đổi tên đăng nhập, quyền và mật khẩu.
            </p>
          )}

          <div className="adm-form-f">
            <button type="submit" className="adm-btn" disabled={saving || !employees?.length}>
              {saving ? 'Đang lưu...' : 'Lưu tài khoản'}
            </button>
          </div>
          {!employees?.length && <p className="adm-muted">Chưa có nhân viên đang hoạt động để cấp tài khoản.</p>}
        </form>
      )}
    </section>
  );
}
