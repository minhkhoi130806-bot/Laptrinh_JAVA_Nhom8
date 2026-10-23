'use client';

import { useState } from 'react';
import { api, dt, num, vnd } from '@/lib/api';
import { useApi } from '@/lib/hooks';
import PageHeader from './PageHeader';
import StateBox from './StateBox';
import { useToast } from './Feedback';

export default function ReceiptView({ id }) {
  const { data: r, error, loading, reload } = useApi(`/receipts/${id}`);
  const toast = useToast();
  const [auditNote, setAuditNote] = useState('');
  const [savingNote, setSavingNote] = useState(false);

  const handleAddAuditNote = async (e) => {
    e.preventDefault();
    if (!auditNote.trim()) return;
    setSavingNote(true);
    try {
      await api(`/receipts/${id}/note`, {
        method: 'PATCH',
        body: { note: auditNote.trim() },
      });
      toast('Đã ghi nhận nhật ký kiểm toán vào phiếu nhập');
      setAuditNote('');
      reload();
    } catch (err) {
      toast(err.message, 'err');
    } finally {
      setSavingNote(false);
    }
  };

  // Tách các dòng nhật ký kiểm toán từ chuỗi note
  const noteEntries = (r?.note || '')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);

  return (
    <>
      <PageHeader
        icon="📥"
        title={`Phiếu nhập #${id}`}
        back={{ href: '/admin/receipts', label: 'Phiếu nhập kho' }}
      >
        <button
          type="button"
          className="adm-btn ghost sm"
          onClick={() => window.print()}
          title="In biên bản nhập kho lưu trữ"
        >
          🖨️ In biên bản kiểm kho
        </button>
      </PageHeader>

      <StateBox loading={loading} error={error} onRetry={reload} />

      {r && (
        <div className="adm-detail">
          {/* CỘT TRÁI: DANH SÁCH NGUYÊN LIỆU ĐÃ NHẬP */}
          <section className="adm-card adm-section">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <h3 style={{ margin: 0 }}>Nguyên liệu đã nhập ({r.details.length} dòng)</h3>
              <span style={{ fontSize: '0.8rem', color: '#16a34a', background: '#dcfce7', padding: '3px 8px', borderRadius: 6, fontWeight: 700 }}>
                ✓ Đã ghi nhận vào kho
              </span>
            </div>

            <div className="adm-table-wrap" style={{ boxShadow: 'none' }}>
              <table className="adm-table">
                <thead>
                  <tr>
                    <th>Nguyên liệu</th>
                    <th className="adm-right">Số lượng nhập</th>
                    <th className="adm-right">Đơn giá nhập</th>
                    <th className="adm-right">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {r.details.map((d) => (
                    <tr key={d.ingredientId}>
                      <td className="adm-cell-main">
                        <b>{d.ingredientName}</b>
                        <div style={{ fontSize: '0.78rem', color: '#64748b' }}>Mã NL: #{d.ingredientId}</div>
                      </td>
                      <td className="adm-num">
                        <strong>{num(d.quantity)}</strong> {d.unit}
                      </td>
                      <td className="adm-num">{vnd(d.unitPrice)}</td>
                      <td className="adm-num">
                        <b>{vnd(d.lineTotal)}</b>
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={3} style={{ fontWeight: 700 }}>Tổng tiền phiếu nhập</td>
                    <td className="adm-num" style={{ fontSize: '1.15rem', color: '#dc2626', fontWeight: 800 }}>
                      {vnd(r.totalAmount)}
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </section>

          {/* CỘT PHẢI: THÔNG TIN PHIẾU & NHẬT KÝ KIỂM TOÁN CHỐNG GIAN LẬN */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {/* THÔNG TIN CHUNG */}
            <section className="adm-card adm-section">
              <h3 style={{ marginBottom: 12 }}>Thông tin phiếu nhập</h3>
              <dl className="adm-kv">
                <dt>Nhà cung cấp</dt>
                <dd><b>{r.supplierName}</b></dd>
                <dt>Người lập phiếu</dt>
                <dd>{r.employeeName ? `🧑‍🍳 ${r.employeeName}` : '—'}</dd>
                <dt>Ngày giờ nhập</dt>
                <dd>{dt(r.receiptDate)}</dd>
                <dt>Mã số phiếu</dt>
                <dd><code>PR-{r.id}</code></dd>
              </dl>
            </section>

            {/* NHẬT KÝ KIỂM TOÁN & GIẢI TRÌNH CHỐNG GIAN LẬN */}
            <section className="adm-card adm-section">
              <h3 style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 10 }}>
                🛡️ Nhật ký kiểm toán & Giải trình
              </h3>
              <p style={{ fontSize: '0.83rem', color: '#64748b', margin: '0 0 14px' }}>
                Lưu vết vĩnh viễn các ghi chú kiểm kho, giải trình khi phát hiện sai sót hoặc biên bản đối soát chứng từ.
              </p>

              {/* DANH SÁCH CÁC GHI CHÚ ĐÃ LƯU */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 16 }}>
                {noteEntries.length === 0 ? (
                  <div className="adm-muted" style={{ padding: '8px 12px', background: '#f8fafc', borderRadius: 8, fontStyle: 'italic', fontSize: '0.85rem' }}>
                    Chưa có ghi chú kiểm toán nào cho phiếu này.
                  </div>
                ) : (
                  noteEntries.map((entry, idx) => (
                    <div
                      key={idx}
                      style={{
                        padding: '10px 12px',
                        background: entry.includes('Kiểm toán') ? '#fef2f2' : '#f8fafc',
                        borderLeft: entry.includes('Kiểm toán') ? '3px solid #dc2626' : '3px solid #cbd5e1',
                        borderRadius: '0 8px 8px 0',
                        fontSize: '0.85rem',
                        lineHeight: 1.5,
                      }}
                    >
                      {entry}
                    </div>
                  ))
                )}
              </div>

              {/* FORM BỔ SUNG GHI CHÚ KIỂM TOÁN */}
              <form onSubmit={handleAddAuditNote}>
                <label className="adm-field">
                  <span className="adm-label" style={{ fontSize: '0.83rem', fontWeight: 600 }}>
                    ＋ Bổ sung ghi chú kiểm toán / Giải trình sai lệch:
                  </span>
                  <textarea
                    className="adm-input"
                    rows={3}
                    placeholder="VD: [Quản lý]: Đã đối chiếu hóa đơn VAT số 0042, xác nhận cân lại đúng số lượng; hoặc: Phát hiện nhập thừa 2kg, đã lập biên bản điều chỉnh..."
                    value={auditNote}
                    onChange={(e) => setAuditNote(e.target.value)}
                    required
                  />
                </label>
                <div style={{ marginTop: 8, display: 'flex', justifyContent: 'flex-end' }}>
                  <button
                    type="submit"
                    className="adm-btn sm"
                    disabled={savingNote || !auditNote.trim()}
                  >
                    {savingNote ? 'Đang lưu vết…' : 'Ghi nhận kiểm toán'}
                  </button>
                </div>
              </form>
            </section>
          </div>
        </div>
      )}
    </>
  );
}
