'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
import MenuCard from '@/components/shop/MenuCard';
import { api } from '@/lib/api';

export default function MenuPage() {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [category, setCategory] = useState('all');
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState('featured');
  const [maxPrice, setMaxPrice] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    Promise.all([api('/menu-items'), api('/categories')])
      .then(([menu, cats]) => {
        if (alive) {
          setItems((menu || []).filter((item) => item.available && !item.deleted));
          setCategories((cats || []).filter((item) => !item.deleted));
        }
      })
      .catch((e) => alive && setError(e.message || 'Không thể tải thực đơn.'))
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
  }, []);

  const hasFilter = query.trim() !== '' || category !== 'all' || maxPrice !== '' || sort !== 'featured';

  const resetFilter = () => {
    setQuery('');
    setCategory('all');
    setMaxPrice('');
    setSort('featured');
  };

  const filtered = useMemo(() => {
    const rows = items.filter((item) => {
      const matchesCategory = category === 'all' || String(item.categoryId) === String(category);
      const term = query.trim().toLocaleLowerCase('vi');
      const matchesQuery =
        !term ||
        `${item.nameVi || ''} ${item.nameKo || ''} ${item.description || ''}`
          .toLocaleLowerCase('vi')
          .includes(term);
      const matchesPrice = maxPrice === '' || Number(item.price || 0) <= Number(maxPrice);
      return matchesCategory && matchesQuery && matchesPrice;
    });
    return rows.sort((a, b) => {
      if (sort === 'price-asc') return Number(a.price || 0) - Number(b.price || 0);
      if (sort === 'price-desc') return Number(b.price || 0) - Number(a.price || 0);
      if (sort === 'name') return String(a.nameVi || '').localeCompare(String(b.nameVi || ''), 'vi');
      return 0;
    });
  }, [items, category, query, maxPrice, sort]);

  return (
    <ShopLayout>
      <section className="shop-page-hero shop-menu-hero">
        <div className="shop-container">
          <span className="shop-eyebrow">TỪ BẾP HAN SEOUL</span>
          <h1>
            Khám phá <em>thực đơn</em>
          </h1>
          <p>Từ món ăn đường phố Seoul đến những hương vị truyền thống được yêu thích.</p>
          <div className="shop-breadcrumb">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <span>Thực đơn</span>
          </div>
        </div>
      </section>

      <section className="shop-section shop-container">
        {/* TIÊU ĐỀ KHU VỰC THỰC ĐƠN */}
        <div style={{ marginBottom: '20px' }}>
          <span className="shop-eyebrow">MÓN NGON MỖI NGÀY</span>
          <h2
            style={{
              margin: '8px 0 0',
              fontFamily: "var(--font-display, 'Palatino Linotype', Georgia, serif)",
              fontSize: '36px',
              fontWeight: 400,
              color: '#1a202c',
            }}
          >
            Chọn món bạn yêu thích
          </h2>
        </div>

        {/* DANH SÁCH DANH MỤC (PILLS) */}
        <div className="shop-category-tabs" style={{ margin: '0 0 24px' }}>
          <button
            className={category === 'all' ? 'selected' : ''}
            onClick={() => setCategory('all')}
          >
            Tất cả món
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              className={String(category) === String(c.id) ? 'selected' : ''}
              onClick={() => setCategory(String(c.id))}
            >
              {c.name}
            </button>
          ))}
        </div>

        {/* KHUNG TÌM KIẾM VÀ LỌC ĐƯỢC THIẾT KẾ ĐỒNG BỘ, ĐỀU VÀ ĐẸP */}
        <div className="shop-menu-controls">
          <div className="shop-menu-filter-inputs">
            {/* 1. Ô TÌM KIẾM MÓN ĂN */}
            <div className="shop-filter-input-wrap shop-filter-search-box">
              <span className="shop-filter-icon">🔍</span>
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Tìm tên món ăn, nguyên liệu..."
                aria-label="Tìm món ăn"
              />
              {query && (
                <button
                  type="button"
                  className="shop-filter-clear-btn"
                  onClick={() => setQuery('')}
                  title="Xóa tìm kiếm"
                >
                  ✕
                </button>
              )}
            </div>

            {/* 2. Ô LỌC THEO GIÁ TỐI ĐA */}
            <div className="shop-filter-input-wrap shop-filter-price-box">
              <span className="shop-filter-icon">₫</span>
              <input
                type="number"
                min="0"
                step="10000"
                value={maxPrice}
                onChange={(e) => setMaxPrice(e.target.value)}
                placeholder="Giá tối đa"
                aria-label="Giá tối đa"
              />
              {maxPrice && (
                <button
                  type="button"
                  className="shop-filter-clear-btn"
                  onClick={() => setMaxPrice('')}
                  title="Bỏ lọc giá"
                >
                  ✕
                </button>
              )}
            </div>

            {/* 3. DROPDOWN SẮP XẾP */}
            <div className="shop-filter-input-wrap shop-filter-sort-box">
              <span className="shop-filter-icon">↕</span>
              <select
                value={sort}
                onChange={(e) => setSort(e.target.value)}
                aria-label="Sắp xếp thực đơn"
              >
                <option value="featured">Thứ tự mặc định</option>
                <option value="price-asc">Giá: Thấp đến cao</option>
                <option value="price-desc">Giá: Cao đến thấp</option>
                <option value="name">Tên món: A – Z</option>
              </select>
            </div>

            {/* 4. NÚT XÓA BỘ LỌC (KHI CÓ BỘ LỌC ĐANG ÁP DỤNG) */}
            {hasFilter && (
              <button
                type="button"
                className="shop-filter-reset-all"
                onClick={resetFilter}
              >
                ✕ Xóa bộ lọc
              </button>
            )}
          </div>

          {/* SỐ LƯỢNG MÓN PHÙ HỢP */}
          <div className="shop-menu-results-count">
            Hiển thị <strong>{filtered.length}</strong> món phù hợp
          </div>
        </div>

        {/* TRẠNG THÁI HIỂN THỊ */}
        {loading && (
          <div className="shop-state">
            <span className="shop-spinner" /> Đang tải thực đơn...
          </div>
        )}
        {error && (
          <div className="shop-state shop-state-error">
            <b>Chưa thể tải thực đơn.</b>
            <p>{error}</p>
            <button onClick={() => location.reload()}>Thử lại</button>
            <small>Vui lòng kiểm tra kết nối hoặc thử lại sau ít phút.</small>
          </div>
        )}
        {!loading && !error && filtered.length === 0 && (
          <div className="shop-empty">
            <span>🍽️</span>
            <h3>Chưa tìm thấy món phù hợp</h3>
            <p>Thử từ khóa khác hoặc bấm nút bên dưới để xem lại tất cả món ăn nhé.</p>
            <button onClick={resetFilter}>Xóa bộ lọc</button>
          </div>
        )}
        {!loading && !error && filtered.length > 0 && (
          <div className="shop-menu-grid">
            {filtered.map((item) => (
              <MenuCard key={item.id} item={item} />
            ))}
          </div>
        )}
      </section>

      <section className="shop-reservation-strip">
        <div>
          <span className="shop-eyebrow">MỘT CHỖ NGỒI ĐANG CHỜ BẠN</span>
          <h2>Bữa ăn ngon hơn khi có kế hoạch.</h2>
          <p>Đặt bàn trước để chúng tôi chuẩn bị trải nghiệm tốt nhất cho bạn.</p>
        </div>
        <Link href="/dat-ban" className="shop-button shop-button-light">
          Đặt bàn ngay <span>↗</span>
        </Link>
      </section>
    </ShopLayout>
  );
}
