'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import Icon from '@/components/ui/Icon';
import { readCart, updateCartItem, removeCartItem, clearCart, cartTotal, addToCart } from '@/lib/cart';
import { vnd, api } from '@/lib/api';
import './cart.css';

export default function CartPage() {
  const [items, setItems] = useState([]);
  const [user, setUser] = useState(null);
  const [popularDishes, setPopularDishes] = useState([]);
  const [loadingSuggestions, setLoadingSuggestions] = useState(false);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    const refresh = () => setItems(readCart());
    refresh();

    try {
      const stored = localStorage.getItem('currentUser');
      if (stored) {
        setUser(JSON.parse(stored));
      }
    } catch {}

    setMounted(true);

    // Tải các món ăn gợi ý từ thực đơn
    setLoadingSuggestions(true);
    api('/menu-items')
      .then((data) => {
        if (Array.isArray(data)) {
          // Lấy tối đa 4 món có ảnh và còn hàng
          const filtered = data
            .filter((d) => d.isAvailable !== false)
            .slice(0, 4);
          setPopularDishes(filtered);
        }
      })
      .catch(() => {})
      .finally(() => setLoadingSuggestions(false));

    window.addEventListener('cart:changed', refresh);
    window.addEventListener('storage', refresh);
    return () => {
      window.removeEventListener('cart:changed', refresh);
      window.removeEventListener('storage', refresh);
    };
  }, []);

  const totalQuantity = useMemo(() => {
    return items.reduce((sum, item) => sum + Number(item.quantity || 1), 0);
  }, [items]);

  const rawSubtotal = useMemo(() => cartTotal(items), [items]);

  // Ưu đãi hội viên: Giảm 10% (tối đa 50.000 ₫)
  const memberDiscount = useMemo(() => {
    if (!user || rawSubtotal <= 0) return 0;
    return Math.min(Math.round(rawSubtotal * 0.1), 50000);
  }, [user, rawSubtotal]);

  const finalTotal = useMemo(() => {
    return Math.max(0, rawSubtotal - memberDiscount);
  }, [rawSubtotal, memberDiscount]);

  function handleQuantityChange(id, currentQty, delta, note) {
    const newQty = currentQty + delta;
    if (newQty <= 0) {
      removeCartItem(id, note);
    } else {
      updateCartItem(id, Math.min(99, newQty), note);
    }
    setItems(readCart());
  }

  function handleNoteChange(id, currentQty, newNote) {
    updateCartItem(id, currentQty, newNote);
    setItems(readCart());
  }

  function handleRemoveItem(id, note) {
    removeCartItem(id, note);
    setItems(readCart());
  }

  function handleClearAll() {
    if (window.confirm('Bạn có chắc muốn xóa tất cả món ăn trong giỏ hàng?')) {
      clearCart();
      setItems([]);
    }
  }

  function handleAddSuggested(dish) {
    addToCart(
      {
        id: dish.id,
        nameVi: dish.nameVi,
        name: dish.nameVi,
        price: dish.price,
        imageUrl: dish.imageUrl,
      },
      1,
      ''
    );
    setItems(readCart());
  }

  return (
    <ShopLayout>
      {/* HERO BANNER */}
      <section className="shop-page-hero">
        <div className="shop-container">
          <span className="shop-eyebrow">HAN SEOUL · YOUR SELECTION</span>
          <h1>
            Giỏ hàng <em>của bạn</em>
          </h1>
          <p>Kiểm tra danh sách món ăn, số lượng và áp dụng ưu đãi trước khi gửi đơn đến nhà hàng.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <span>Giỏ hàng</span>
          </div>
        </div>
      </section>

      <section className="shop-section shop-container">
        <div className="cart-page-wrap">
          {/* ================= TH1: GIỎ HÀNG TRỐNG ================= */}
          {items.length === 0 ? (
            <div>
              <div className="cart-empty-box">
                <div className="cart-empty-emblem" aria-hidden="true">
                  🍲
                </div>
                <h2 className="cart-empty-title">Giỏ hàng của bạn đang trống</h2>
                <p className="cart-empty-desc">
                  Bữa tiệc ẩm thực Hàn Quốc chuẩn vị Seoul đang chờ bạn khám phá. Hãy thêm các món nướng BBQ, lẩu cay nồng,
                  cơm trộn hay món tráng miệng ngọt ngào vào giỏ ngay nhé!
                </p>
                <div className="cart-empty-actions">
                  <Link href="/thuc-don" className="shop-button" style={{ padding: '12px 28px', fontSize: '15px' }}>
                    Khám phá thực đơn ngay ↗
                  </Link>
                  <Link href="/khuyen-mai" className="shop-outline-link" style={{ padding: '12px 24px', fontSize: '14px' }}>
                    Xem ưu đãi hội viên ✨
                  </Link>
                  <Link href="/dat-ban" className="shop-outline-link" style={{ padding: '12px 24px', fontSize: '14px' }}>
                    Đặt bàn giữ chỗ 📅
                  </Link>
                </div>
              </div>

              {/* GỢI Ý MÓN ĂN THỊNH HÀNH KHI GIỎ HÀNG TRỐNG */}
              {popularDishes.length > 0 && (
                <div className="cart-suggested-section">
                  <div className="cart-suggested-header">
                    <div>
                      <span className="shop-eyebrow">GỢI Ý MÓN NGON HÔM NAY</span>
                      <h3
                        style={{
                          margin: '4px 0 0',
                          fontFamily: "var(--font-display, 'Palatino Linotype', Georgia, serif)",
                          fontSize: '24px',
                          color: '#1a202c',
                        }}
                      >
                        Được yêu thích nhất tại Han Seoul
                      </h3>
                    </div>
                    <Link href="/thuc-don" className="shop-text-link">
                      Xem tất cả món ↗
                    </Link>
                  </div>

                  <div className="cart-suggested-grid">
                    {popularDishes.map((dish) => {
                      const img = dish.imageUrl
                        ? dish.imageUrl.startsWith('http') || dish.imageUrl.startsWith('/')
                          ? dish.imageUrl
                          : `/${dish.imageUrl}`
                        : null;

                      return (
                        <div key={dish.id} className="cart-suggested-card">
                          <div className="cart-suggested-thumb">
                            {img ? <img src={img} alt={dish.nameVi} /> : <div style={{ fontSize: '38px', textAlign: 'center', paddingTop: '20px' }}>🍲</div>}
                          </div>
                          <div className="cart-suggested-body">
                            <h4 className="cart-suggested-name">{dish.nameVi}</h4>
                            <div className="cart-suggested-price">{vnd(dish.price)}</div>
                            <button
                              type="button"
                              className="cart-suggested-btn"
                              onClick={() => handleAddSuggested(dish)}
                            >
                              <Icon name="plus" size={15} /> Thêm vào giỏ
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          ) : (
            /* ================= TH2: GIỎ HÀNG CÓ MÓN ================= */
            <div className="cart-content-layout">
              {/* CỘT TRÁI: DANH SÁCH MÓN ĂN TRONG GIỎ */}
              <div className="cart-items-panel">
                <div className="cart-items-panel-header">
                  <div>
                    <span className="shop-eyebrow">ĐÃ CHỌN TRONG BỮA TIỆC</span>
                    <h2 className="cart-items-count-text">
                      {totalQuantity} món trong giỏ hàng
                    </h2>
                  </div>

                  <div className="cart-header-actions">
                    <Link href="/thuc-don" className="shop-outline-link" style={{ fontSize: '13px', padding: '6px 14px', borderRadius: '8px' }}>
                      ← Chọn thêm món
                    </Link>
                    <button type="button" onClick={handleClearAll} className="cart-clear-all-btn">
                      <Icon name="trash" size={14} /> Xóa tất cả
                    </button>
                  </div>
                </div>

                <div className="cart-dish-list">
                  {items.map((item, idx) => {
                    const itemSubtotal = Number(item.price || 0) * Number(item.quantity || 1);
                    const img = item.imageUrl
                      ? item.imageUrl.startsWith('http') || item.imageUrl.startsWith('/')
                        ? item.imageUrl
                        : `/${item.imageUrl}`
                      : null;

                    return (
                      <div key={`${item.id}-${idx}`} className="cart-dish-row">
                        <div className="cart-dish-img">
                          {img ? <img src={img} alt={item.name} /> : '🍲'}
                        </div>

                        <div className="cart-dish-details">
                          <h3 className="cart-dish-name">{item.name}</h3>
                          <span className="cart-dish-unit-price">{vnd(item.price)} / phần</span>
                          <input
                            type="text"
                            className="cart-dish-note-input"
                            placeholder="Ghi chú món (ví dụ: ít cay, sốt riêng...)"
                            value={item.note || ''}
                            onChange={(e) => handleNoteChange(item.id, item.quantity, e.target.value)}
                            title="Ghi chú khẩu vị cho đầu bếp"
                          />
                        </div>

                        <div className="cart-dish-controls">
                          <strong className="cart-dish-subtotal">{vnd(itemSubtotal)}</strong>

                          <div className="cart-dish-actions-wrap">
                            <div className="cart-stepper">
                              <button
                                type="button"
                                className="cart-stepper-btn"
                                aria-label="Giảm số lượng"
                                onClick={() => handleQuantityChange(item.id, item.quantity, -1, item.note)}
                              >
                                −
                              </button>
                              <span className="cart-stepper-val">{item.quantity}</span>
                              <button
                                type="button"
                                className="cart-stepper-btn"
                                aria-label="Tăng số lượng"
                                onClick={() => handleQuantityChange(item.id, item.quantity, 1, item.note)}
                              >
                                +
                              </button>
                            </div>

                            <button
                              type="button"
                              className="cart-dish-delete-btn"
                              title="Xóa món này"
                              onClick={() => handleRemoveItem(item.id, item.note)}
                            >
                              <Icon name="trash" size={18} />
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* CỘT PHẢI: TÓM TẮT THANH TOÁN */}
              <div className="cart-summary-panel">
                <h3 className="cart-summary-heading">Tóm tắt đơn hàng</h3>

                {/* ƯU ĐÃI THÀNH VIÊN */}
                {mounted && (
                  user ? (
                    <div className="cart-member-banner">
                      <span style={{ fontSize: '20px' }}>🎖️</span>
                      <div>
                        <strong>Ưu đãi hội viên Han Seoul</strong>
                        <div style={{ fontSize: '12px', marginTop: '2px' }}>
                          Giảm ngay 10% (tối đa 50.000 ₫) và tích điểm đổi quà khi thanh toán!
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="cart-member-banner" style={{ background: '#f0fff4', borderColor: '#c6f6d5', color: '#22543d' }}>
                      <span style={{ fontSize: '20px' }}>✨</span>
                      <div>
                        <strong>Tham gia hội viên Han Seoul</strong>
                        <div style={{ fontSize: '12px', marginTop: '2px' }}>
                          <Link href="/dang-nhap?redirect=/gio-hang" style={{ color: '#8B2500', fontWeight: 700, textDecoration: 'underline' }}>
                            Đăng nhập
                          </Link>{' '}
                          hoặc{' '}
                          <Link href="/dang-ky?redirect=/gio-hang" style={{ color: '#8B2500', fontWeight: 700, textDecoration: 'underline' }}>
                            Đăng ký
                          </Link>{' '}
                          để nhận 50 điểm và giảm 10%!
                        </div>
                      </div>
                    </div>
                  )
                )}

                <div className="cart-price-lines">
                  <div className="cart-price-line">
                    <span>Tạm tính ({totalQuantity} món)</span>
                    <b>{vnd(rawSubtotal)}</b>
                  </div>

                  {memberDiscount > 0 && (
                    <div className="cart-price-line discount">
                      <span>Ưu đãi thành viên (10%)</span>
                      <b>- {vnd(memberDiscount)}</b>
                    </div>
                  )}

                  <div className="cart-price-line">
                    <span>Phí dịch vụ & đóng gói</span>
                    <span style={{ color: '#276749', fontWeight: 600 }}>Miễn phí</span>
                  </div>
                </div>

                <div className="cart-summary-divider" />

                <div className="cart-price-total-row">
                  <span>Tổng thanh toán</span>
                  <strong>{vnd(finalTotal)}</strong>
                </div>

                <Link href="/thanh-toan" className="shop-button cart-checkout-btn">
                  Tiến hành đặt món & thanh toán <Icon name="arrowUpRight" size={18} />
                </Link>

                <Link href="/thuc-don" className="cart-continue-link">
                  ← Tiếp tục chọn thêm món
                </Link>

                <div className="cart-perks-list">
                  <span>
                    <Icon name="check" size={15} style={{ color: '#276749' }} />
                    Món ăn chuẩn bị tươi nóng ngay sau khi đặt
                  </span>
                  <span>
                    <Icon name="check" size={15} style={{ color: '#276749' }} />
                    Hỗ trợ thanh toán Tiền mặt, VNPay, ZaloPay
                  </span>
                  <span>
                    <Icon name="check" size={15} style={{ color: '#276749' }} />
                    Tự động nhận hóa đơn điện tử qua Gmail
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>
      </section>
    </ShopLayout>
  );
}
