'use client';
import { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { api, toInputDateTime } from '@/lib/api';

function initialTime() {
  const date = new Date(Date.now() + 2 * 60 * 60 * 1000);
  date.setMinutes(0, 0, 0);
  return toInputDateTime(date);
}

function formatVnd(val) {
  return Number(val || 0).toLocaleString('vi-VN') + ' ₫';
}

export default function ReservationPage() {
  const [form, setForm] = useState({
    fullName: '',
    phone: '',
    email: '',
    reservedAt: initialTime(),
    guestCount: '2',
    note: '',
  });
  const [tables, setTables] = useState([]);
  const [tableId, setTableId] = useState('');
  const [checking, setChecking] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(null);

  // Dữ liệu món ăn để khách tùy chọn đặt món trước (giống đời thực)
  const [menuItems, setMenuItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loadingMenu, setLoadingMenu] = useState(false);
  const [showPreOrder, setShowPreOrder] = useState(false);
  const [preOrderQuery, setPreOrderQuery] = useState('');
  const [selectedCat, setSelectedCat] = useState('all');
  const [preOrders, setPreOrders] = useState({}); // { [itemId]: quantity }

  const set = (key, value) => setForm((old) => ({ ...old, [key]: value }));

  // Tự động nạp thông tin hội viên đã đăng nhập từ localStorage để khách không phải gõ lại
  useEffect(() => {
    try {
      const user = JSON.parse(localStorage.getItem('currentUser') || 'null');
      if (user) {
        setForm((prev) => ({
          ...prev,
          fullName: prev.fullName || user.fullName || user.name || '',
          phone: prev.phone || user.phone || '',
          email: prev.email || user.email || '',
        }));
      }
    } catch {}
  }, []);

  // Tải danh sách thực đơn & danh mục để phục vụ tính năng đặt món trước
  useEffect(() => {
    let alive = true;
    setLoadingMenu(true);
    Promise.all([api('/menu-items'), api('/categories')])
      .then(([menu, cats]) => {
        if (alive) {
          setMenuItems((menu || []).filter((item) => item.available && !item.deleted));
          setCategories((cats || []).filter((item) => !item.deleted));
        }
      })
      .catch(() => {
        // Nếu lỗi mạng thực đơn vẫn cho phép khách đặt bàn bình thường
      })
      .finally(() => alive && setLoadingMenu(false));
    return () => {
      alive = false;
    };
  }, []);

  // Kiểm tra bàn trống theo thời gian và số lượng khách
  useEffect(() => {
    if (!form.reservedAt || Number(form.guestCount) < 1) return;
    let alive = true;
    setChecking(true);
    setError('');
    setTableId('');
    const query = `at=${encodeURIComponent(form.reservedAt)}&guests=${encodeURIComponent(form.guestCount)}`;
    api(`/reservations/available-tables?${query}`)
      .then((rows) => {
        if (alive) setTables(rows || []);
      })
      .catch((e) => {
        if (alive) {
          setTables([]);
          setError(e.message || 'Không thể kiểm tra bàn trống.');
        }
      })
      .finally(() => alive && setChecking(false));
    return () => {
      alive = false;
    };
  }, [form.reservedAt, form.guestCount]);

  // Bộ lọc món ăn khi khách mở phần đặt món trước
  const filteredDishes = useMemo(() => {
    const q = preOrderQuery.trim().toLowerCase();
    return menuItems.filter((dish) => {
      const matchCat = selectedCat === 'all' || String(dish.categoryId) === String(selectedCat);
      const matchQuery =
        !q ||
        `${dish.nameVi || ''} ${dish.nameKo || ''} ${dish.description || ''}`
          .toLowerCase()
          .includes(q);
      return matchCat && matchQuery;
    });
  }, [menuItems, selectedCat, preOrderQuery]);

  // Danh sách các món đã được chọn trước
  const chosenItemsList = useMemo(() => {
    return Object.entries(preOrders)
      .filter(([_, qty]) => qty > 0)
      .map(([id, qty]) => {
        const dish = menuItems.find((m) => String(m.id) === String(id));
        return {
          id: Number(id),
          menuItemId: Number(id),
          nameVi: dish?.nameVi || 'Món ăn',
          nameKo: dish?.nameKo || '',
          price: dish?.price || 0,
          quantity: qty,
          total: (dish?.price || 0) * qty,
        };
      });
  }, [preOrders, menuItems]);

  const totalPreOrderCount = useMemo(() => {
    return chosenItemsList.reduce((sum, item) => sum + item.quantity, 0);
  }, [chosenItemsList]);

  const totalPreOrderAmount = useMemo(() => {
    return chosenItemsList.reduce((sum, item) => sum + item.total, 0);
  }, [chosenItemsList]);

  const updateQuantity = (dishId, delta) => {
    setPreOrders((prev) => {
      const current = prev[dishId] || 0;
      const next = Math.max(0, current + delta);
      const copy = { ...prev };
      if (next === 0) {
        delete copy[dishId];
      } else {
        copy[dishId] = next;
      }
      return copy;
    });
  };

  const clearAllPreOrders = () => {
    setPreOrders({});
  };

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess(null);
    if (!tableId) {
      setError('Vui lòng chọn một bàn còn trống.');
      return;
    }
    setSaving(true);
    const submittedEmail = form.email.trim();

    // Chuẩn bị danh sách món đặt trước (nếu có)
    const formattedPreOrders = chosenItemsList.map((item) => ({
      menuItemId: item.menuItemId,
      itemName: item.nameVi,
      quantity: item.quantity,
      price: item.price,
    }));

    try {
      // Đặt bàn công khai: tự động tạo/cập nhật email khách hàng, ghi chú các món đặt trước và gửi email xác nhận
      const saved = await api('/reservations/public', {
        method: 'POST',
        body: {
          fullName: form.fullName.trim(),
          phone: form.phone.trim(),
          email: submittedEmail || null,
          tableId: Number(tableId),
          reservedAt: form.reservedAt,
          guestCount: Number(form.guestCount),
          note: form.note.trim() || null,
          preOrderItems: formattedPreOrders,
        },
      });

      const selectedTable = tables.find((item) => item.id === Number(tableId));
      const bookingRecord = {
        id: saved.id,
        fullName: form.fullName.trim(),
        phone: form.phone.trim(),
        email: submittedEmail,
        reservedAt: form.reservedAt,
        guestCount: Number(form.guestCount),
        note: form.note.trim(),
        tableCode: selectedTable?.tableCode || '',
        preOrderItems: formattedPreOrders,
        preOrderTotal: totalPreOrderAmount,
        savedAt: new Date().toISOString(),
      };
      try {
        localStorage.setItem('hanseoul:lastReservation', JSON.stringify(bookingRecord));
        localStorage.setItem('hanseoul:lastPhone', form.phone.trim().replace(/[ .-]/g, ''));
      } catch {}

      setSuccess({
        id: saved.id,
        email: submittedEmail,
        reservedAt: form.reservedAt,
        table: selectedTable,
        preOrderItems: formattedPreOrders,
        preOrderTotal: totalPreOrderAmount,
      });

      // Reset form sau khi đặt thành công
      setForm((prev) => ({
        ...prev,
        note: '',
        reservedAt: initialTime(),
      }));
      setTableId('');
      setPreOrders({});
      setShowPreOrder(false);
    } catch (e) {
      setError(e.message || 'Đặt bàn chưa thành công. Vui lòng kiểm tra lại thông tin.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <ShopLayout>
      <section className="shop-page-hero shop-booking-hero">
        <div className="shop-container">
          <span className="shop-eyebrow">DÀNH RIÊNG MỘT CHỖ CHO BẠN</span>
          <h1>
            Đặt bàn <em>thật dễ dàng</em>
          </h1>
          <p>Chia sẻ thời gian, số lượng khách và tùy chọn thêm món trước để Han Seoul chuẩn bị chu đáo nhất.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <span>Đặt bàn</span>
          </div>
        </div>
      </section>

      <section className="shop-section shop-container shop-booking-layout">
        <div className="shop-booking-copy">
          <span className="shop-eyebrow">YOUR TABLE IS WAITING</span>
          <h2>Hẹn nhau bên bàn ăn Hàn Quốc.</h2>
          <p>
            Dù là bữa tối thân mật, gặp gỡ bạn bè hay dịp đặc biệt, hãy để chúng tôi chăm chút từng chi tiết.
          </p>
          <div className="shop-booking-perk">
            <span>01</span>
            <div>
              <b>Chọn đúng thời gian & vị trí bàn</b>
              <p>Hệ thống tự động kiểm tra bàn trống theo giờ và số khách.</p>
            </div>
          </div>
          <div className="shop-booking-perk">
            <span>02</span>
            <div>
              <b>Thêm món trước tiệc (Tùy chọn)</b>
              <p>Đến nơi có món nóng hổi ngay mà không cần chờ đợi chuẩn bị nguyên liệu.</p>
            </div>
          </div>
          <div className="shop-booking-perk">
            <span>03</span>
            <div>
              <b>Xác nhận tự động qua Gmail</b>
              <p>Email chi tiết ngày giờ, mã bàn và thông tin đặt trước gửi ngay về hòm thư của bạn.</p>
            </div>
          </div>
          <div className="shop-booking-note">
            <span>✦</span> Đặt bàn trước giúp chúng tôi phục vụ bạn chu đáo và trọn vẹn hơn.
          </div>
        </div>

        <form className="shop-booking-form" onSubmit={submit}>
          {/* BANNER ƯU ĐÃI ĐẶT BÀN ONLINE */}
          <div
            style={{
              background: 'linear-gradient(135deg, #fffaf0 0%, #feebc8 100%)',
              border: '1px solid #fbd38d',
              borderRadius: '12px',
              padding: '16px 20px',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'center',
              gap: '14px',
            }}
          >
            <span style={{ fontSize: '32px' }}>🍧</span>
            <div>
              <strong style={{ color: '#744210', fontSize: '15px' }}>ƯU ĐÃI ĐẶC BIỆT KHI ĐẶT BÀN TRƯỚC</strong>
              <p style={{ margin: '3px 0 0', fontSize: '13px', color: '#975a16', lineHeight: '1.5' }}>
                Tặng ngay <b>01 phần Bingsu kem tuyết Hàn Quốc</b> hoặc <b>giảm 10%</b> khi đặt bàn và dùng bữa tại nhà hàng!
              </p>
            </div>
          </div>

          <div className="shop-form-heading">
            <span className="shop-eyebrow">HAN SEOUL · RESERVATION</span>
            <h2>Thông tin đặt bàn</h2>
            <p>Vui lòng điền các thông tin có dấu *</p>
          </div>

          {/* THÔNG BÁO XÁC NHẬN THÀNH CÔNG VÀ THÔNG BÁO GỬI EMAIL */}
          {success && (
            <div
              style={{
                background: '#f0fff4',
                border: '1px solid #9ae6b4',
                borderRadius: '12px',
                padding: '20px 24px',
                marginBottom: '24px',
                textAlign: 'left',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                <span style={{ fontSize: '24px' }}>🎉</span>
                <h3 style={{ margin: 0, color: '#22543d', fontSize: '18px', fontWeight: 700 }}>
                  ĐẶT BÀN THÀNH CÔNG!
                </h3>
              </div>
              <p style={{ margin: '4px 0 10px', fontSize: '14.5px', color: '#276749', lineHeight: '1.5' }}>
                Mã đặt bàn của bạn là <strong style={{ color: '#8B2500', fontSize: '16px' }}>#{success.id}</strong>
                {success.table ? ` · Bàn ${success.table.tableCode} (${success.table.zone})` : ''}.
              </p>

              {/* HIỂN THỊ MÓN ĐÃ ĐẶT TRƯỚC NẾU CÓ */}
              {success.preOrderItems && success.preOrderItems.length > 0 && (
                <div
                  style={{
                    background: '#ffffff',
                    border: '1px dashed #68d391',
                    borderRadius: '8px',
                    padding: '12px 16px',
                    marginBottom: '14px',
                  }}
                >
                  <strong style={{ color: '#22543d', fontSize: '13.5px', display: 'block', marginBottom: '6px' }}>
                    🍽️ Món ăn đã đặt trước cho bữa tiệc ({success.preOrderItems.length} món):
                  </strong>
                  <ul style={{ margin: '0 0 8px 18px', padding: 0, fontSize: '13px', color: '#2d3748' }}>
                    {success.preOrderItems.map((item, idx) => (
                      <li key={idx} style={{ margin: '4px 0' }}>
                        {item.quantity}x <b>{item.itemName}</b> ({formatVnd(item.price * item.quantity)})
                      </li>
                    ))}
                  </ul>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: '#8B2500' }}>
                    Tổng tiền món ước tính: {formatVnd(success.preOrderTotal)}
                  </div>
                </div>
              )}

              {success.email && (
                <div
                  style={{
                    background: '#ffffff',
                    border: '1px solid #c6f6d5',
                    borderRadius: '8px',
                    padding: '12px 16px',
                    marginBottom: '14px',
                    fontSize: '13.5px',
                    color: '#2d3748',
                  }}
                >
                  📧 <strong>Hệ thống đã gửi email xác nhận chi tiết</strong> tới Gmail:{' '}
                  <b style={{ color: '#8B2500' }}>{success.email}</b>. Quý khách vui lòng kiểm tra hộp thư để xem chi tiết lịch hẹn.
                </div>
              )}
              <p style={{ margin: '0 0 14px', fontSize: '13px', color: '#4a5568' }}>
                ✦ Nhà hàng sẽ giữ bàn trong vòng 15 phút. Khi đến nhà hàng, bạn chỉ cần đọc số điện thoại hoặc mã đặt bàn{' '}
                <strong>#{success.id}</strong>.
              </p>
              <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                <Link
                  href="/don-hang?tab=reservations"
                  className="shop-button"
                  style={{ minHeight: '40px', padding: '8px 18px', fontSize: '13px' }}
                >
                  Xem lịch sử đặt bàn của bạn →
                </Link>
                <Link
                  href="/thuc-don"
                  className="shop-button shop-button-light"
                  style={{ minHeight: '40px', padding: '8px 18px', fontSize: '13px' }}
                >
                  Khám phá thêm thực đơn
                </Link>
              </div>
            </div>
          )}

          <div className="shop-form-grid">
            <label className="shop-field shop-field-full">
              Họ và tên *
              <input
                required
                maxLength={100}
                value={form.fullName}
                onChange={(e) => set('fullName', e.target.value)}
                placeholder="Tên của bạn"
              />
            </label>

            <label className="shop-field">
              Số điện thoại *
              <input
                required
                type="tel"
                pattern="[0-9+() .-]{8,20}"
                value={form.phone}
                onChange={(e) => set('phone', e.target.value)}
                placeholder="0900 000 000"
              />
            </label>

            <label className="shop-field">
              Địa chỉ Gmail (nhận thư xác nhận đặt bàn tự động) *
              <input
                required
                type="email"
                value={form.email}
                onChange={(e) => set('email', e.target.value)}
                placeholder="Ví dụ: cuongmai115111@gmail.com"
              />
              <small style={{ fontSize: '11px', color: '#718096', marginTop: '2px', display: 'block' }}>
                Hệ thống sẽ gửi email chi tiết ngày giờ, mã bàn và vị trí bàn về Gmail này.
              </small>
            </label>

            <label className="shop-field">
              Ngày và giờ *
              <input
                required
                type="datetime-local"
                min={toInputDateTime()}
                value={form.reservedAt}
                onChange={(e) => set('reservedAt', e.target.value)}
              />
            </label>

            <label className="shop-field">
              Số khách *
              <select
                required
                value={form.guestCount}
                onChange={(e) => set('guestCount', e.target.value)}
              >
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20].map((n) => (
                  <option key={n} value={n}>
                    {n} người
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="shop-table-availability">
            <div className="shop-availability-heading">
              <b>Chọn bàn phù hợp *</b>
              <span>{checking ? 'Đang kiểm tra...' : `${tables.length} bàn trống`}</span>
            </div>
            {checking ? (
              <div className="shop-table-loading">Đang tìm bàn phù hợp với yêu cầu của bạn...</div>
            ) : tables.length ? (
              <div className="shop-table-grid">
                {tables.map((table) => (
                  <button
                    type="button"
                    key={table.id}
                    className={`shop-table-option ${String(tableId) === String(table.id) ? 'chosen' : ''}`}
                    onClick={() => setTableId(String(table.id))}
                  >
                    <span className="shop-table-icon">⌂</span>
                    <b>{table.tableCode}</b>
                    <small>
                      {table.capacity} chỗ · {table.zone}
                    </small>
                    {String(tableId) === String(table.id) && <span className="shop-table-check">✓</span>}
                  </button>
                ))}
              </div>
            ) : (
              <p className="shop-no-tables">Không tìm thấy bàn trống. Hãy đổi thời gian hoặc số khách.</p>
            )}
          </div>

          {/* PHẦN ĐẶT MÓN TRƯỚC CHO BỮA TIỆC (TÙY CHỌN - GIỐNG ĐỜI THỰC HƠN) */}
          <div
            style={{
              margin: '24px 0',
              border: totalPreOrderCount > 0 ? '1.5px solid #8B2500' : '1px solid #e2e8f0',
              borderRadius: '12px',
              background: totalPreOrderCount > 0 ? '#fffaf8' : '#fafafa',
              overflow: 'hidden',
              transition: 'all 0.25s ease',
            }}
          >
            {/* Header Accordion Toggle */}
            <div
              style={{
                padding: '16px 20px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                cursor: 'pointer',
                background: totalPreOrderCount > 0 ? '#fef3ee' : '#f8fafc',
                borderBottom: showPreOrder ? '1px solid #e2e8f0' : 'none',
              }}
              onClick={() => setShowPreOrder(!showPreOrder)}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <span style={{ fontSize: '26px' }}>🍽️</span>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    <strong style={{ fontSize: '15px', color: '#1a202c' }}>
                      Chọn món trước cho bàn đặt (Tùy chọn)
                    </strong>
                    <span
                      style={{
                        fontSize: '11.5px',
                        fontWeight: 600,
                        padding: '2px 8px',
                        borderRadius: '99px',
                        background: totalPreOrderCount > 0 ? '#8B2500' : '#edf2f7',
                        color: totalPreOrderCount > 0 ? '#ffffff' : '#718096',
                      }}
                    >
                      {totalPreOrderCount > 0 ? `Đã chọn ${totalPreOrderCount} món` : 'Tùy chọn'}
                    </span>
                  </div>
                  <p style={{ margin: '2px 0 0', fontSize: '12.5px', color: '#718096' }}>
                    Khách có thể đặt món trước để nhà hàng chuẩn bị sẵn, hoặc để trống và gọi món trực tiếp tại bàn.
                  </p>
                </div>
              </div>

              <button
                type="button"
                style={{
                  background: totalPreOrderCount > 0 ? '#8B2500' : '#ffffff',
                  color: totalPreOrderCount > 0 ? '#ffffff' : '#4a5568',
                  border: '1px solid #cbd5e0',
                  borderRadius: '8px',
                  padding: '7px 14px',
                  fontSize: '13px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  whiteSpace: 'nowrap',
                }}
              >
                {showPreOrder ? 'Thu gọn ▲' : totalPreOrderCount > 0 ? 'Chỉnh sửa món ▼' : '+ Thêm món ngay ▼'}
              </button>
            </div>

            {/* Nội dung danh sách món ăn khi mở rộng */}
            {showPreOrder && (
              <div style={{ padding: '18px 20px', background: '#ffffff' }}>
                {/* Bộ lọc món ăn */}
                <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '14px' }}>
                  <input
                    type="text"
                    value={preOrderQuery}
                    onChange={(e) => setPreOrderQuery(e.target.value)}
                    placeholder="Tìm tên món (Galbi, Tokbokki, Kim chi...)"
                    style={{
                      flex: 1,
                      minWidth: '200px',
                      padding: '8px 12px',
                      border: '1px solid #cbd5e0',
                      borderRadius: '8px',
                      fontSize: '13.5px',
                      outline: 'none',
                    }}
                  />
                  <select
                    value={selectedCat}
                    onChange={(e) => setSelectedCat(e.target.value)}
                    style={{
                      padding: '8px 12px',
                      border: '1px solid #cbd5e0',
                      borderRadius: '8px',
                      fontSize: '13.5px',
                      outline: 'none',
                      background: '#fff',
                    }}
                  >
                    <option value="all">Tất cả danh mục</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Grid các món ăn */}
                {loadingMenu ? (
                  <p style={{ textAlign: 'center', color: '#718096', padding: '20px' }}>Đang nạp thực đơn...</p>
                ) : filteredDishes.length === 0 ? (
                  <p style={{ textAlign: 'center', color: '#718096', padding: '20px' }}>Không tìm thấy món ăn phù hợp.</p>
                ) : (
                  <div
                    style={{
                      display: 'grid',
                      gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
                      gap: '12px',
                      maxHeight: '380px',
                      overflowY: 'auto',
                      paddingRight: '4px',
                    }}
                  >
                    {filteredDishes.map((dish) => {
                      const qty = preOrders[dish.id] || 0;
                      return (
                        <div
                          key={dish.id}
                          style={{
                            display: 'flex',
                            gap: '10px',
                            padding: '10px',
                            borderRadius: '10px',
                            border: qty > 0 ? '1.5px solid #8B2500' : '1px solid #e2e8f0',
                            background: qty > 0 ? '#fff9f7' : '#ffffff',
                            alignItems: 'center',
                          }}
                        >
                          <div
                            style={{
                              width: '56px',
                              height: '56px',
                              borderRadius: '8px',
                              overflow: 'hidden',
                              flexShrink: 0,
                              background: '#edf2f7',
                            }}
                          >
                            {/* eslint-disable-next-line @next/next/no-img-element */}
                            <img
                              src={dish.imageUrl || '/images/hero-banquet.jpg'}
                              alt={dish.nameVi}
                              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                            />
                          </div>

                          <div style={{ flex: 1, minWidth: 0 }}>
                            <div
                              style={{
                                fontSize: '13.5px',
                                fontWeight: 650,
                                color: '#1a202c',
                                whiteSpace: 'nowrap',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                              }}
                            >
                              {dish.nameVi}
                            </div>
                            <div style={{ fontSize: '13px', color: '#8B2500', fontWeight: 700, marginTop: '2px' }}>
                              {formatVnd(dish.price)}
                            </div>
                          </div>

                          {/* Bộ tăng giảm số lượng */}
                          {qty > 0 ? (
                            <div
                              style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '6px',
                                background: '#ffffff',
                                border: '1px solid #cbd5e0',
                                borderRadius: '6px',
                                padding: '2px 4px',
                              }}
                            >
                              <button
                                type="button"
                                onClick={() => updateQuantity(dish.id, -1)}
                                style={{
                                  border: 'none',
                                  background: 'none',
                                  cursor: 'pointer',
                                  fontWeight: 700,
                                  fontSize: '14px',
                                  color: '#718096',
                                  width: '20px',
                                  height: '24px',
                                }}
                              >
                                −
                              </button>
                              <span style={{ fontSize: '13px', fontWeight: 700, minWidth: '16px', textAlign: 'center' }}>
                                {qty}
                              </span>
                              <button
                                type="button"
                                onClick={() => updateQuantity(dish.id, 1)}
                                style={{
                                  border: 'none',
                                  background: 'none',
                                  cursor: 'pointer',
                                  fontWeight: 700,
                                  fontSize: '14px',
                                  color: '#8B2500',
                                  width: '20px',
                                  height: '24px',
                                }}
                              >
                                +
                              </button>
                            </div>
                          ) : (
                            <button
                              type="button"
                              onClick={() => updateQuantity(dish.id, 1)}
                              style={{
                                background: '#f7fafc',
                                border: '1px solid #cbd5e0',
                                borderRadius: '6px',
                                padding: '6px 10px',
                                fontSize: '12px',
                                fontWeight: 600,
                                cursor: 'pointer',
                                color: '#2d3748',
                              }}
                            >
                              + Thêm
                            </button>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* Thanh tóm tắt món đã chọn */}
                {totalPreOrderCount > 0 && (
                  <div
                    style={{
                      marginTop: '16px',
                      padding: '12px 16px',
                      background: '#fef3ee',
                      border: '1px solid #fed7c7',
                      borderRadius: '8px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      flexWrap: 'wrap',
                      gap: '10px',
                    }}
                  >
                    <div>
                      <span style={{ fontSize: '13px', color: '#7b341e' }}>
                        Đã chọn <b>{totalPreOrderCount}</b> món · Ước tính: <b style={{ fontSize: '15px', color: '#8B2500' }}>{formatVnd(totalPreOrderAmount)}</b>
                      </span>
                      <p style={{ margin: '2px 0 0', fontSize: '11.5px', color: '#9c4221' }}>
                        Tiền món sẽ thanh toán trực tiếp tại nhà hàng khi quý khách dùng bữa.
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={clearAllPreOrders}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#c53030',
                        fontSize: '12.5px',
                        cursor: 'pointer',
                        textDecoration: 'underline',
                      }}
                    >
                      Bỏ chọn tất cả món
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          <label className="shop-field shop-field-full">
            Ghi chú thêm
            <textarea
              rows="3"
              maxLength={500}
              value={form.note}
              onChange={(e) => set('note', e.target.value)}
              placeholder="Ví dụ: sinh nhật, ghế trẻ em, bàn gần cửa sổ, ăn cay ít..."
            />
          </label>

          {error && (
            <div className="shop-form-error" role="alert">
              {error}
            </div>
          )}

          <button
            className="shop-button shop-submit"
            type="submit"
            disabled={saving || checking || !tableId}
          >
            {saving ? 'Đang gửi yêu cầu...' : 'Xác nhận đặt bàn'} <span>↗</span>
          </button>
          <p className="shop-form-footnote">
            Bằng việc gửi yêu cầu, bạn đồng ý để nhà hàng liên hệ và gửi email xác nhận đặt bàn.
          </p>
        </form>
      </section>
    </ShopLayout>
  );
}
