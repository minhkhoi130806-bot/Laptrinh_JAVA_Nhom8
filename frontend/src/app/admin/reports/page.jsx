'use client';

import { useState } from 'react';
import Link from 'next/link';
import { api, dt, num, vnd } from '@/lib/api';
import { useApi } from '@/lib/hooks';
import PageHeader from '@/components/admin/PageHeader';
import StateBox from '@/components/admin/StateBox';
import { useToast } from '@/components/admin/Feedback';
import Icon from '@/components/ui/Icon';

/**
 * =========================================================================
 * TRANG BÁO CÁO TOÀN DIỆN CỦA NHÀ HÀNG (ReportsPage)
 * - Module Doanh thu: Phụ trách bởi Ngô Minh Khôi (Tuần 4)
 * - Module Tồn kho, Chi phí nhập & Món bán chạy: Phụ trách bởi Mai Chí Cường (Tuần 4)
 * =========================================================================
 */
function downloadCsv(filename, rows) {
  if (!Array.isArray(rows) || rows.length === 0) return false;
  const columns = [...new Set(rows.flatMap((row) => Object.keys(row || {})))];
  const escapeCell = (value) => {
    const text = value == null ? '' : typeof value === 'object' ? JSON.stringify(value) : String(value);
    return `"${text.replace(/"/g, '""')}"`;
  };
  const csv = '\uFEFF' + [columns.map(escapeCell).join(','), ...rows.map((row) => columns.map((key) => escapeCell(row[key])).join(','))].join('\r\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
  return true;
}

export default function ReportsPage() {
  // Tabs: revenue (Doanh thu - Khôi) | inventory | purchase | bestSellers | stockStatus
  const [tab, setTab] = useState('revenue');
  const toast = useToast();
  const [syncing, setSyncing] = useState(false);
  const [revDays, setRevDays] = useState(30);

  // APIs dữ liệu báo cáo
  const revApi = useApi(`/invoices/report/revenue?days=${revDays}`);
  const invApi = useApi('/ingredients/report');
  const purApi = useApi('/receipts/report');
  const bestApi = useApi('/menu-items/best-sellers');
  const stockApi = useApi('/menu-items/stock-status');

  const loading = revApi.loading || invApi.loading || purApi.loading || bestApi.loading || stockApi.loading;
  const error = revApi.error || invApi.error || purApi.error || bestApi.error || stockApi.error;

  const reloadAll = () => {
    revApi.reload();
    invApi.reload();
    purApi.reload();
    bestApi.reload();
    stockApi.reload();
  };

  // Đồng bộ trạng thái món hết hàng theo kho (Cường)
  const handleSyncOutOfStock = async () => {
    setSyncing(true);
    try {
      const res = await api('/menu-items/sync-out-of-stock', { method: 'POST' });
      toast(res.message || 'Đã đồng bộ trạng thái món ăn thành công');
      stockApi.reload();
    } catch (e) {
      toast(e.message, 'err');
    } finally {
      setSyncing(false);
    }
  };

  const revData = revApi.data || {
    totalRevenue: 0,
    totalInvoices: 0,
    averageInvoiceValue: 0,
    todayRevenue: 0,
    dailyRevenues: [],
    paymentMethodRevenues: []
  };
  const invData = invApi.data || { totalIngredients: 0, lowStockCount: 0, outOfStockCount: 0, ingredients: [] };
  const purData = purApi.data || { totalSpent: 0, totalReceipts: 0, bySupplier: [], recentReceipts: [] };
  const bestList = bestApi.data || [];
  const stockList = stockApi.data || [];

  // Tính giá trị doanh thu cao nhất của các ngày để vẽ biểu đồ thanh trực quan
  const maxDailyRevenue = Math.max(
    ...(revData.dailyRevenues || []).map((d) => Number(d.revenue || 0)),
    100000
  );

  const exportCurrentReport = () => {
    const sources = {
      revenue: revData.dailyRevenues || [],
      inventory: invData.ingredients || [],
      purchase: purData.recentReceipts || [],
      bestSellers: bestList || [],
      stockStatus: stockList || [],
    };
    const labels = {
      revenue: 'doanh-thu-theo-ngay',
      inventory: 'ton-kho-nguyen-lieu',
      purchase: 'phieu-nhap-hang',
      bestSellers: 'mon-ban-chay',
      stockStatus: 'kiem-tra-ton-kho-mon',
    };
    const rows = sources[tab] || [];
    if (!downloadCsv(`${labels[tab] || 'bao-cao'}.csv`, rows)) {
      toast('Không có dữ liệu để xuất ở mục này.', 'err');
    } else {
      toast('Đã xuất báo cáo CSV.', 'ok');
    }
  };

  return (
    <>
      <PageHeader
        icon="📈"
        title="Báo cáo & Thống kê"
        subtitle="Doanh thu bán hàng, chi phí nhập kho và tình trạng thực đơn"
      >
        <button type="button" className="adm-btn ghost sm" onClick={exportCurrentReport}>
          <Icon name="download" size={15} />Xuất CSV
        </button>
        <button
          type="button"
          className="adm-btn sm"
          disabled={syncing}
          onClick={handleSyncOutOfStock}
          title="Tự động kiểm tra công thức món và tắt bán các món thiếu nguyên liệu"
        >
          <Icon name="sparkle" size={15} />{syncing ? 'Đang đồng bộ…' : 'Đồng bộ món hết hàng'}
        </button>
      </PageHeader>

      <StateBox loading={loading && !revApi.data && !invApi.data} error={error} onRetry={reloadAll} />

      {/* THANH CHUYỂN TABS */}
      <div className="rep-tabs">
        <button
          className={`rep-tab ${tab === 'revenue' ? 'active' : ''}`}
          onClick={() => setTab('revenue')}
        >
          <Icon name="chart" size={16} />Doanh thu ({vnd(revData.totalRevenue)})
        </button>
        <button
          className={`rep-tab ${tab === 'inventory' ? 'active' : ''}`}
          onClick={() => setTab('inventory')}
        >
          <Icon name="leaf" size={16} />Tồn kho ({invData.totalIngredients})
          {invData.lowStockCount > 0 && <span className="rep-badge warn">{invData.lowStockCount}</span>}
          {invData.outOfStockCount > 0 && <span className="rep-badge danger">{invData.outOfStockCount}</span>}
        </button>
        <button
          className={`rep-tab ${tab === 'purchase' ? 'active' : ''}`}
          onClick={() => setTab('purchase')}
        >
          <Icon name="download" size={16} />Chi phí nhập hàng ({vnd(purData.totalSpent)})
        </button>
        <button
          className={`rep-tab ${tab === 'bestSellers' ? 'active' : ''}`}
          onClick={() => setTab('bestSellers')}
        >
          <Icon name="bowl" size={16} />Món bán chạy ({bestList.length})
        </button>
        <button
          className={`rep-tab ${tab === 'stockStatus' ? 'active' : ''}`}
          onClick={() => setTab('stockStatus')}
        >
          <Icon name="layers" size={16} />Kiểm tra món hết hàng ({stockList.filter((s) => !s.hasEnoughStock).length} món thiếu)
        </button>
      </div>

      {/* TAB 0: BÁO CÁO DOANH THU (Người phụ trách: Ngô Minh Khôi - Tuần 4) */}
      {tab === 'revenue' && (
        <div className="rep-content">
          {/* CÁC THẺ THỐNG KÊ TỔNG QUAN DOANH THU */}
          <div className="adm-stats">
            <div className="adm-stat">
              <span className="adm-muted">Tổng doanh thu</span>
              <b style={{ color: 'var(--green)' }}>{vnd(revData.totalRevenue)}</b>
            </div>
            <div className="adm-stat">
              <span className="adm-muted">Doanh thu hôm nay</span>
              <b style={{ color: 'var(--blue)' }}>{vnd(revData.todayRevenue)}</b>
            </div>
            <div className="adm-stat">
              <span className="adm-muted">Tổng số hóa đơn</span>
              <b>{num(revData.totalInvoices)} đơn</b>
            </div>
            <div className="adm-stat">
              <span className="adm-muted">Giá trị đơn trung bình</span>
              <b>{vnd(revData.averageInvoiceValue)}</b>
            </div>
          </div>

          {/* BỘ LỌC KHOẢNG THỜI GIAN */}
          <div className="rep-chart-heading">
            <h3 style={{ margin: 0 }}>Biểu đồ doanh thu theo ngày ({revDays} ngày gần nhất)</h3>
            <div style={{ display: 'flex', gap: 6 }}>
              {[7, 14, 30].map((d) => (
                <button
                  key={d}
                  className={`adm-btn sm ${revDays === d ? 'primary' : 'ghost'}`}
                  onClick={() => setRevDays(d)}
                >
                  {d} ngày qua
                </button>
              ))}
            </div>
          </div>

          {/* BIỂU ĐỒ THANH TRỰC QUAN DOANH THU (CSS Bar Chart) */}
          <div className="adm-card adm-section" style={{ padding: '20px 16px' }}>
            {(!revData.dailyRevenues || revData.dailyRevenues.length === 0) ? (
              <p className="adm-muted" style={{ textAlign: 'center' }}>Chưa có dữ liệu doanh thu trong khoảng thời gian này.</p>
            ) : (
              <div className="rev-chart-wrap">
                <div className="rev-chart-bars">
                  {revData.dailyRevenues.map((item) => {
                    const rev = Number(item.revenue || 0);
                    const pct = Math.max((rev / maxDailyRevenue) * 100, 3);
                    const shortDate = item.date.slice(5); // MM-DD
                    return (
                      <div key={item.date} className="rev-bar-col" title={`${item.date}: ${vnd(rev)} (${item.invoiceCount} hóa đơn)`}>
                        <div className="rev-bar-val">{rev > 0 ? vnd(rev) : ''}</div>
                        <div className="rev-bar-track">
                          <div
                            className="rev-bar-fill"
                            style={{
                              height: `${pct}%`,
                              background: rev > 0 ? 'linear-gradient(180deg, var(--green), #82ad91)' : 'var(--line)'
                            }}
                          />
                        </div>
                        <div className="rev-bar-label">{shortDate}</div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          {/* HAI BẢNG THỐNG KÊ CHI TIẾT */}
          <div className="adm-two" style={{ marginTop: 18 }}>
            {/* THỐNG KÊ THEO PHƯƠNG THỨC THANH TOÁN */}
            <section className="adm-panel">
              <h3>Theo phương thức thanh toán</h3>
              {(!revData.paymentMethodRevenues || revData.paymentMethodRevenues.length === 0) ? (
                <p className="adm-muted">Chưa có giao dịch thanh toán nào.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                  {revData.paymentMethodRevenues.map((p) => {
                    const tenPhuongThuc = p.paymentMethod === 'CASH' ? '💵 Tiền mặt (CASH)'
                      : p.paymentMethod === 'BANKING' || p.paymentMethod === 'TRANSFER' ? '🏦 Chuyển khoản ngân hàng'
                      : p.paymentMethod === 'CARD' ? '💳 Thẻ tín dụng / Ghi nợ'
                      : p.paymentMethod === 'MOMO' ? '📱 Ví MoMo'
                      : p.paymentMethod;
                    const tongTien = Number(revData.totalRevenue || 0);
                    const pct = tongTien > 0 ? (Number(p.revenue) / tongTien) * 100 : 0;
                    return (
                      <div key={p.paymentMethod} style={{ borderBottom: '1px solid var(--gray-bg)', paddingBottom: 10 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                          <span><strong>{tenPhuongThuc}</strong> ({p.invoiceCount} đơn)</span>
                          <b style={{ color: 'var(--green)' }}>{vnd(p.revenue)}</b>
                        </div>
                        <div style={{ background: 'var(--gray-bg)', borderRadius: 4, height: 7, overflow: 'hidden' }}>
                          <div style={{ background: 'var(--green)', height: '100%', width: `${pct}%` }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>

            {/* BẢNG CHI TIẾT DOANH THU THEO TỪNG NGÀY */}
            <section className="adm-panel">
              <h3>Danh sách ngày gần đây</h3>
              <div className="adm-table-wrap" style={{ maxHeight: 320, overflowY: 'auto' }}>
                <table className="adm-table">
                  <thead>
                    <tr>
                      <th>Ngày</th>
                      <th style={{ textAlign: 'center' }}>Số đơn</th>
                      <th style={{ textAlign: 'right' }}>Doanh thu</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[...(revData.dailyRevenues || [])].reverse().slice(0, 15).map((d) => (
                      <tr key={d.date}>
                        <td><b>{d.date}</b></td>
                        <td style={{ textAlign: 'center' }}>{d.invoiceCount} đơn</td>
                        <td style={{ textAlign: 'right', fontWeight: 700, color: Number(d.revenue) > 0 ? 'var(--green)' : 'var(--muted)' }}>
                          {vnd(d.revenue)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Link href="/admin/invoices" className="adm-link">Xem tất cả hóa đơn thanh toán →</Link>
            </section>
          </div>
        </div>
      )}

      {/* TAB 1: BÁO CÁO TỒN KHO (Cường) */}
      {tab === 'inventory' && (
        <div className="rep-content">
          <div className="adm-stats">
            <div className="adm-stat">
              <span className="adm-muted">Tổng nguyên liệu</span>
              <b>{invData.totalIngredients} loại</b>
            </div>
            <div className="adm-stat">
              <span className="adm-muted">Sắp hết (≤ min)</span>
              <b style={{ color: 'var(--amber)' }}>{invData.lowStockCount} loại</b>
            </div>
            <div className="adm-stat">
              <span className="adm-muted">Đã hết hàng (= 0)</span>
              <b style={{ color: 'var(--brand)' }}>{invData.outOfStockCount} loại</b>
            </div>
            <Link href="/admin/receipts/create" className="adm-stat" style={{ background: 'var(--brand-soft)' }}>
              <span className="adm-muted">Hành động nhanh</span>
              <b style={{ color: 'var(--brand)', fontSize: 17 }}>＋ Lập phiếu nhập</b>
            </Link>
          </div>

          <div className="adm-card adm-section" style={{ marginTop: 18 }}>
            <h3 style={{ marginBottom: 12 }}>Bảng chi tiết tồn kho nguyên liệu</h3>
            <div className="adm-table-wrap">
              <table className="adm-table">
                <thead>
                  <tr>
                    <th>Mã</th>
                    <th>Tên nguyên liệu</th>
                    <th>Đơn vị</th>
                    <th style={{ textAlign: 'right' }}>Tồn kho</th>
                    <th style={{ textAlign: 'right' }}>Tối thiểu</th>
                    <th>Trạng thái</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {invData.ingredients.map((ing) => {
                    const isOut = Number(ing.stockQty) <= 0;
                    const isLow = Number(ing.stockQty) <= Number(ing.minStock) && !isOut;
                    return (
                      <tr key={ing.id}>
                        <td>#{ing.id}</td>
                        <td><b>{ing.name}</b></td>
                        <td>{ing.unit}</td>
                        <td style={{ textAlign: 'right', fontWeight: 600 }}>{num(ing.stockQty)}</td>
                        <td style={{ textAlign: 'right', color: 'var(--muted)' }}>{num(ing.minStock)}</td>
                        <td>
                          {isOut ? (
                            <span className="rep-tag red">Hết hàng</span>
                          ) : isLow ? (
                            <span className="rep-tag amber">Sắp hết</span>
                          ) : (
                            <span className="rep-tag green">Đủ hàng</span>
                          )}
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <Link href={`/admin/ingredients/${ing.id}`} className="adm-btn ghost sm">Sửa</Link>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: BÁO CÁO CHI PHÍ NHẬP HÀNG (Cường) */}
      {tab === 'purchase' && (
        <div className="rep-content">
          <div className="adm-stats">
            <div className="adm-stat">
              <span className="adm-muted">Tổng chi phí nhập hàng</span>
              <b style={{ color: 'var(--brand)' }}>{vnd(purData.totalSpent)}</b>
            </div>
            <div className="adm-stat">
              <span className="adm-muted">Tổng phiếu nhập</span>
              <b>{purData.totalReceipts} phiếu</b>
            </div>
            <div className="adm-stat">
              <span className="adm-muted">Chi phí TB / phiếu</span>
              <b>{vnd(purData.totalReceipts ? Number(purData.totalSpent) / purData.totalReceipts : 0)}</b>
            </div>
            <div className="adm-stat">
              <span className="adm-muted">Số nhà cung cấp đã giao</span>
              <b>{purData.bySupplier.length} NCC</b>
            </div>
          </div>

          <div className="adm-two" style={{ marginTop: 18 }}>
            <section className="adm-panel">
              <h3>Chi phí theo nhà cung cấp</h3>
              {purData.bySupplier.length === 0 ? (
                <p className="adm-muted">Chưa có dữ liệu nhập hàng.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {purData.bySupplier.map((s) => {
                    const pct = purData.totalSpent > 0 ? (Number(s.totalCost) / Number(purData.totalSpent)) * 100 : 0;
                    return (
                      <div key={s.supplierId} style={{ borderBottom: '1px solid var(--gray-bg)', paddingBottom: 8 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                          <span><strong>{s.supplierName}</strong> ({s.receiptCount} phiếu)</span>
                          <b>{vnd(s.totalCost)}</b>
                        </div>
                        <div style={{ background: 'var(--gray-bg)', borderRadius: 4, height: 6, overflow: 'hidden' }}>
                          <div style={{ background: 'var(--brand)', height: '100%', width: `${pct}%` }}></div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>

            <section className="adm-panel">
              <h3>Phiếu nhập gần đây</h3>
              {purData.recentReceipts.length === 0 ? (
                <p className="adm-muted">Chưa có phiếu nhập nào.</p>
              ) : (
                <div>
                  {purData.recentReceipts.slice(0, 6).map((r) => (
                    <Link key={r.id} href={`/admin/receipts/${r.id}`} className="adm-li">
                      <div>
                        <b>Phiếu #{r.id} · {r.supplierName}</b>
                        <div className="adm-muted">{dt(r.receiptDate)} · {r.employeeName || '—'}</div>
                      </div>
                      <b>{vnd(r.totalAmount)}</b>
                    </Link>
                  ))}
                </div>
              )}
              <Link href="/admin/receipts" className="adm-link">Xem tất cả phiếu nhập →</Link>
            </section>
          </div>
        </div>
      )}

      {/* TAB 3: BÁO CÁO MÓN BÁN CHẠY (Cường) */}
      {tab === 'bestSellers' && (
        <div className="rep-content">
          <div className="adm-card adm-section">
            <h3 style={{ marginBottom: 12 }}>Bảng xếp hạng món bán chạy nhất (Top Sellers)</h3>
            {bestList.length === 0 ? (
              <div className="adm-empty">
                Chưa có dữ liệu gọi món từ các đơn hàng. Khi có đơn hàng được tạo và bán, hệ thống sẽ xếp hạng tại đây!
              </div>
            ) : (
              <div className="adm-table-wrap">
                <table className="adm-table">
                  <thead>
                    <tr>
                      <th style={{ width: 60 }}>Hạng</th>
                      <th>Món ăn</th>
                      <th>Danh mục</th>
                      <th style={{ textAlign: 'right' }}>Giá bán</th>
                      <th style={{ textAlign: 'right' }}>Số lượng đã bán</th>
                      <th style={{ textAlign: 'right' }}>Tổng doanh thu</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bestList.map((m, idx) => (
                      <tr key={m.menuItemId}>
                        <td>
                          <span className={`rep-rank ${idx < 3 ? `top-${idx + 1}` : ''}`}>
                            {idx + 1}
                          </span>
                        </td>
                        <td>
                          <div className="adm-cell-main">{m.nameVi}</div>
                          {m.nameKo && <div className="adm-cell-sub">{m.nameKo}</div>}
                        </td>
                        <td>{m.categoryName}</td>
                        <td style={{ textAlign: 'right' }}>{vnd(m.price)}</td>
                        <td style={{ textAlign: 'right', fontWeight: 700, color: 'var(--brand)' }}>
                          {num(m.totalQuantitySold)} phần
                        </td>
                        <td style={{ textAlign: 'right', fontWeight: 700 }}>
                          {vnd(m.totalRevenue)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 4: KIỂM TRA MÓN HẾT HÀNG THEO KHO (Cường) */}
      {tab === 'stockStatus' && (
        <div className="rep-content">
          <div className="adm-card adm-section">
            <h3 style={{ marginBottom: 12 }}>Tình trạng nguyên liệu của thực đơn</h3>
            <div className="adm-table-wrap">
              <table className="adm-table">
                <thead>
                  <tr>
                    <th>Mã</th>
                    <th>Tên món ăn</th>
                    <th>Trạng thái bán</th>
                    <th>Kiểm tra tồn kho công thức</th>
                    <th>Chi tiết nguyên liệu thiếu</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {stockList.map((s) => (
                    <tr key={s.menuItemId} style={{ background: !s.hasEnoughStock ? 'var(--red-bg)' : undefined }}>
                      <td>#{s.menuItemId}</td>
                      <td>
                        <b>{s.nameVi}</b>
                        {s.nameKo && <span className="adm-cell-sub">{s.nameKo}</span>}
                      </td>
                      <td>
                        {s.currentlyAvailable ? (
                          <span className="rep-tag green">Đang mở bán</span>
                        ) : (
                          <span className="rep-tag gray">Tạm ngưng</span>
                        )}
                      </td>
                      <td>
                        {s.hasEnoughStock ? (
                          <span className="rep-tag green">✓ Đủ nguyên liệu</span>
                        ) : (
                          <span className="rep-tag red">✗ Thiếu nguyên liệu</span>
                        )}
                      </td>
                      <td>
                        {s.missingIngredients && s.missingIngredients.length > 0 ? (
                          <span style={{ color: 'var(--brand)', fontSize: '0.85rem' }}>
                            {s.missingIngredients.join(', ')}
                          </span>
                        ) : (
                          <span className="adm-muted">Đủ để phục vụ</span>
                        )}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <Link href={`/admin/menu/${s.menuItemId}`} className="adm-btn ghost sm">Xem công thức</Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      <style jsx>{`
        .rep-chart-heading { display: flex; flex-wrap: wrap; gap: 16px; justify-content: space-between; align-items: center; margin-top: 22px; margin-bottom: 18px; }
        .rep-chart-heading h3 { font-size: 14px; font-weight: 600; }
        .rep-tabs {
          display: flex;
          gap: 8px;
          border-bottom: 1px solid var(--line);
          margin-bottom: 26px;
          overflow-x: auto;
        }
        .rep-tab {
          padding: 14px 17px;
          border: none;
          background: none;
          font-weight: 600;
          color: var(--muted);
          cursor: pointer;
          border-bottom: 2px solid transparent;
          margin-bottom: -1px;
          font-size: 12px;
          display: flex;
          align-items: center;
          gap: 6px;
          white-space: nowrap;
          transition: all 0.2s;
        }
        .rep-tab:hover {
          color: var(--brand);
        }
        .rep-tab.active {
          color: var(--brand);
          border-bottom-color: var(--brand);
        }
        .rep-badge {
          font-size: 0.75rem;
          padding: 2px 6px;
          border-radius: 999px;
          color: #fff;
        }
        .rep-badge.warn { background: var(--amber); }
        .rep-badge.danger { background: var(--brand); }
        .rep-tag {
          display: inline-block;
          padding: 3px 8px;
          border-radius: 6px;
          font-size: 0.78rem;
          font-weight: 600;
        }
        .rep-tag.green { background: var(--green-bg); color: var(--green); }
        .rep-tag.amber { background: var(--amber-bg); color: var(--amber); }
        .rep-tag.red { background: var(--red-bg); color: var(--red); }
        .rep-tag.gray { background: var(--gray-bg); color: var(--gray); }
        .rep-rank {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 26px;
          height: 26px;
          border-radius: 50%;
          font-weight: 700;
          background: var(--gray-bg);
          color: var(--gray);
        }
        .rep-rank.top-1 { background: #f1e5bd; color: #8a622d; }
        .rep-rank.top-2 { background: var(--line); color: var(--ink); }
        .rep-rank.top-3 { background: #f2e3d4; color: #9e5d37; }

        /* Kiểu dáng cho biểu đồ doanh thu */
        .rev-chart-wrap {
          overflow-x: auto;
          padding: 10px 0;
        }
        .rev-chart-bars {
          display: flex;
          align-items: flex-end;
          gap: 12px;
          min-width: 600px;
          height: 240px;
          border-bottom: 1px solid var(--line);
          padding-bottom: 4px;
        }
        .rev-bar-col {
          flex: 1;
          display: flex;
          flex-direction: column;
          align-items: center;
          height: 100%;
          justify-content: flex-end;
          cursor: pointer;
          transition: transform 0.15s;
        }
        .rev-bar-col:hover {
          transform: translateY(-2px);
        }
        .rev-bar-val {
          font-size: 0.65rem;
          color: var(--muted);
          margin-bottom: 4px;
          white-space: nowrap;
          text-align: center;
          font-weight: 600;
        }
        .rev-bar-track {
          width: 100%;
          max-width: 24px;
          height: 170px;
          display: flex;
          align-items: flex-end;
          background: #f6f8f2;
          border-radius: 4px 4px 0 0;
        }
        .rev-bar-fill {
          width: 100%;
          border-radius: 4px 4px 0 0;
          transition: height 0.3s ease;
        }
        .rev-bar-label {
          font-size: 0.7rem;
          color: var(--muted);
          margin-top: 6px;
          white-space: nowrap;
        }
      `}</style>
    </>
  );
}
