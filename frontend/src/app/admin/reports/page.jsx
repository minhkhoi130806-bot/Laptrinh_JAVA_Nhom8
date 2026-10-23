'use client';

import { useState } from 'react';
import Link from 'next/link';
import { api, dt, num, vnd } from '@/lib/api';
import { useApi } from '@/lib/hooks';
import PageHeader from '@/components/admin/PageHeader';
import StateBox from '@/components/admin/StateBox';
import { useToast } from '@/components/admin/Feedback';

export default function ReportsPage() {
  const [tab, setTab] = useState('inventory'); // inventory | purchase | bestSellers | stockStatus
  const toast = useToast();
  const [syncing, setSyncing] = useState(false);

  // APIs dữ liệu báo cáo
  const invApi = useApi('/ingredients/report');
  const purApi = useApi('/receipts/report');
  const bestApi = useApi('/menu-items/best-sellers');
  const stockApi = useApi('/menu-items/stock-status');

  const loading = invApi.loading || purApi.loading || bestApi.loading || stockApi.loading;
  const error = invApi.error || purApi.error || bestApi.error || stockApi.error;

  const reloadAll = () => {
    invApi.reload();
    purApi.reload();
    bestApi.reload();
    stockApi.reload();
  };

  // Đồng bộ trạng thái món hết hàng theo kho
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

  const invData = invApi.data || { totalIngredients: 0, lowStockCount: 0, outOfStockCount: 0, ingredients: [] };
  const purData = purApi.data || { totalSpent: 0, totalReceipts: 0, bySupplier: [], recentReceipts: [] };
  const bestList = bestApi.data || [];
  const stockList = stockApi.data || [];

  return (
    <>
      <PageHeader
        icon="📈"
        title="Báo cáo Kho & Thực đơn"
        subtitle="Thống kê chi phí nhập, tồn kho và món bán chạy (Tuần 4)"
      >
        <button
          type="button"
          className="adm-btn sm"
          disabled={syncing}
          onClick={handleSyncOutOfStock}
          title="Tự động kiểm tra công thức món và tắt bán các món thiếu nguyên liệu"
        >
          {syncing ? 'Đang đồng bộ…' : '⚡ Đồng bộ món hết hàng'}
        </button>
      </PageHeader>

      <StateBox loading={loading && !invApi.data} error={error} onRetry={reloadAll} />

      {/* THANH CHUYỂN TABS */}
      <div className="rep-tabs">
        <button
          className={`rep-tab ${tab === 'inventory' ? 'active' : ''}`}
          onClick={() => setTab('inventory')}
        >
          🥬 Tồn kho ({invData.totalIngredients})
          {invData.lowStockCount > 0 && <span className="rep-badge warn">{invData.lowStockCount}</span>}
          {invData.outOfStockCount > 0 && <span className="rep-badge danger">{invData.outOfStockCount}</span>}
        </button>
        <button
          className={`rep-tab ${tab === 'purchase' ? 'active' : ''}`}
          onClick={() => setTab('purchase')}
        >
          📥 Chi phí nhập hàng ({vnd(purData.totalSpent)})
        </button>
        <button
          className={`rep-tab ${tab === 'bestSellers' ? 'active' : ''}`}
          onClick={() => setTab('bestSellers')}
        >
          🍜 Món bán chạy ({bestList.length})
        </button>
        <button
          className={`rep-tab ${tab === 'stockStatus' ? 'active' : ''}`}
          onClick={() => setTab('stockStatus')}
        >
          ⚠️ Kiểm tra món hết hàng ({stockList.filter((s) => !s.hasEnoughStock).length} món thiếu)
        </button>
      </div>

      {/* TAB 1: BÁO CÁO TỒN KHO */}
      {tab === 'inventory' && (
        <div className="rep-content">
          <div className="adm-stats">
            <div className="adm-stat">
              <span className="adm-muted">Tổng nguyên liệu</span>
              <b>{invData.totalIngredients} loại</b>
            </div>
            <div className="adm-stat">
              <span className="adm-muted">Sắp hết (≤ min)</span>
              <b style={{ color: '#d97706' }}>{invData.lowStockCount} loại</b>
            </div>
            <div className="adm-stat">
              <span className="adm-muted">Đã hết hàng (= 0)</span>
              <b style={{ color: '#dc2626' }}>{invData.outOfStockCount} loại</b>
            </div>
            <Link href="/admin/receipts/create" className="adm-stat" style={{ background: '#fef2f2' }}>
              <span className="adm-muted">Hành động nhanh</span>
              <b style={{ color: '#dc2626' }}>＋ Lập phiếu nhập</b>
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
                        <td style={{ textAlign: 'right', color: '#64748b' }}>{num(ing.minStock)}</td>
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

      {/* TAB 2: BÁO CÁO CHI PHÍ NHẬP HÀNG */}
      {tab === 'purchase' && (
        <div className="rep-content">
          <div className="adm-stats">
            <div className="adm-stat">
              <span className="adm-muted">Tổng chi phí nhập hàng</span>
              <b style={{ color: '#dc2626' }}>{vnd(purData.totalSpent)}</b>
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
                      <div key={s.supplierId} style={{ borderBottom: '1px solid #f1f5f9', paddingBottom: 8 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                          <span><strong>{s.supplierName}</strong> ({s.receiptCount} phiếu)</span>
                          <b>{vnd(s.totalCost)}</b>
                        </div>
                        <div style={{ background: '#f1f5f9', borderRadius: 4, height: 6, overflow: 'hidden' }}>
                          <div style={{ background: '#dc2626', height: '100%', width: `${pct}%` }}></div>
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

      {/* TAB 3: BÁO CÁO MÓN BÁN CHẠY */}
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
                        <td style={{ textAlign: 'right', fontWeight: 700, color: '#dc2626' }}>
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

      {/* TAB 4: KIỂM TRA MÓN HẾT HÀNG THEO KHO */}
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
                    <tr key={s.menuItemId} style={{ background: !s.hasEnoughStock ? '#fff5f5' : undefined }}>
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
                          <span style={{ color: '#dc2626', fontSize: '0.85rem' }}>
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
        .rep-tabs {
          display: flex;
          gap: 8px;
          border-bottom: 2px solid #e2e8f0;
          margin-bottom: 20px;
          overflow-x: auto;
        }
        .rep-tab {
          padding: 10px 18px;
          border: none;
          background: none;
          font-weight: 600;
          color: #64748b;
          cursor: pointer;
          border-bottom: 3px solid transparent;
          margin-bottom: -2px;
          display: flex;
          align-items: center;
          gap: 6px;
          white-space: nowrap;
          transition: all 0.2s;
        }
        .rep-tab:hover {
          color: #dc2626;
        }
        .rep-tab.active {
          color: #dc2626;
          border-bottom-color: #dc2626;
        }
        .rep-badge {
          font-size: 0.75rem;
          padding: 2px 6px;
          border-radius: 999px;
          color: #fff;
        }
        .rep-badge.warn { background: #d97706; }
        .rep-badge.danger { background: #dc2626; }
        .rep-tag {
          display: inline-block;
          padding: 3px 8px;
          border-radius: 6px;
          font-size: 0.78rem;
          font-weight: 700;
        }
        .rep-tag.green { background: #dcfce7; color: #15803d; }
        .rep-tag.amber { background: #fef3c7; color: #b45309; }
        .rep-tag.red { background: #fee2e2; color: #b91c1c; }
        .rep-tag.gray { background: #f1f5f9; color: #475569; }
        .rep-rank {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 26px;
          height: 26px;
          border-radius: 50%;
          font-weight: 700;
          background: #f1f5f9;
          color: #475569;
        }
        .rep-rank.top-1 { background: #fef08a; color: #854d0e; }
        .rep-rank.top-2 { background: #e2e8f0; color: #334155; }
        .rep-rank.top-3 { background: #ffedd5; color: #9a3412; }
      `}</style>
    </>
  );
}
