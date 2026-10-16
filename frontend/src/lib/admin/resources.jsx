// Cấu hình các trang quản lý admin. Mỗi tài nguyên dùng cho:
//   admin/<tên>/page.jsx        -> <ResourceList config>
//   admin/<tên>/create/page.jsx -> <ResourceForm config>
//   admin/<tên>/[id]/page.jsx   -> <ResourceForm config id>
import Link from 'next/link';
import Badge from '@/components/admin/Badge';
import Thumb from '@/components/admin/Thumb';
import { AvailableSwitch, ReservationActions, TableCard, isOverdue } from '@/components/admin/RowWidgets';
import { dt, num, vnd } from '@/lib/api';
import { ORDER_STATUSES, RESERVATION_STATUSES, ROLES, SPICE_LEVELS, TABLE_STATUSES, paymentLabel, toOptions } from '@/lib/constants';

// ---------- Danh sách tra cứu (đổ vào <select> và đổi id -> tên) ----------
// scope=ALL: lấy cả bản ghi đã vào thùng rác để vẫn hiện được tên trong lịch sử
export const LK = {
  customers: { endpoint: '/customers?scope=ALL', label: (c) => (c.phone ? `${c.fullName} – ${c.phone}` : c.fullName) },
  tables: { endpoint: '/tables?scope=ALL', label: (t) => `${t.tableCode} (${t.capacity} chỗ)` },
  employees: { endpoint: '/employees?scope=ALL', label: (e) => e.fullName },
  categories: { endpoint: '/categories', label: (c) => c.name },
  menu: { endpoint: '/menu-items?scope=ALL', label: (m) => m.nameVi },
  ingredients: { endpoint: '/ingredients', label: (i) => `${i.name} (${i.unit})` },
  suppliers: { endpoint: '/suppliers', label: (s) => s.name },
  reservations: { endpoint: '/reservations', label: (r) => `#${r.id}` },
};
const L = (key) => ({ [key]: LK[key] });

export const nameOf = (lk, key, id, fallback = '—') => (id == null ? fallback : lk[key]?.map[id] ?? `#${id}`);
const bySortKey = (k) => (a, b) => String(a[k]).localeCompare(String(b[k]), 'vi', { numeric: true });

// ---------- Món ăn ----------
export const menu = {
  key: 'menu', base: '/admin/menu', endpoint: '/menu-items', title: 'Món ăn', singular: 'món ăn', icon: '🍜',
  trash: true, softDelete: true,
  titleOf: (r) => r.nameVi,
  afterCreate: (saved) => `/admin/menu/${saved.id}`, // tạo xong chuyển sang trang món để nhập công thức
  lookups: L('categories'),
  searchText: (r) => `${r.nameKo ?? ''} ${r.categoryName}`,
  filters: [
    { name: 'categoryId', label: 'Mọi danh mục', lookup: 'categories' },
    { name: 'available', label: 'Mọi trạng thái bán', options: [{ value: 'true', label: 'Đang bán' }, { value: 'false', label: 'Tạm ngưng' }] },
  ],
  fields: [
    { name: 'imageUrl', label: 'Ảnh món ăn', type: 'image', folder: 'menu' },
    { name: 'nameVi', label: 'Tên món (Tiếng Việt)', required: true },
    { name: 'nameKo', label: 'Tên món (Tiếng Hàn)' },
    { name: 'categoryId', label: 'Danh mục', lookup: 'categories', required: true },
    { name: 'price', label: 'Giá bán (₫)', type: 'number', min: 0, step: 1000, required: true },
    { name: 'spiceLevel', label: 'Độ cay', options: toOptions(SPICE_LEVELS), default: 'NONE' },
    { name: 'available', label: 'Trạng thái bán', type: 'checkbox', checkLabel: 'Đang bán', default: true },
    { name: 'description', label: 'Mô tả', type: 'textarea' },
  ],
  columns: [
    {
      key: 'name', label: 'Món ăn', render: (r) => (
        <div className="adm-cell-flex">
          <Thumb url={r.imageUrl} alt={r.nameVi} />
          <div><div className="adm-cell-main">{r.nameVi}</div>{r.nameKo && <div className="adm-cell-sub">{r.nameKo}</div>}</div>
        </div>
      ),
    },
    { key: 'categoryName', label: 'Danh mục' },
    { key: 'price', label: 'Giá', align: 'right', render: (r) => vnd(r.price) },
    { key: 'spiceLevel', label: 'Độ cay', render: (r) => <Badge value={r.spiceLevel} /> },
    { key: 'available', label: 'Đang bán', render: (r, ctx) => <AvailableSwitch row={r} reload={ctx.reload} /> },
  ],
};

// ---------- Danh mục ----------
export const categories = {
  key: 'categories', base: '/admin/categories', endpoint: '/categories', title: 'Danh mục', singular: 'danh mục', icon: '🗂️',
  titleOf: (r) => r.name,
  fields: [
    { name: 'imageUrl', label: 'Ảnh danh mục', type: 'image', folder: 'categories' },
    { name: 'name', label: 'Tên danh mục', required: true },
    { name: 'description', label: 'Mô tả', type: 'textarea' },
  ],
  columns: [
    {
      key: 'name', label: 'Danh mục', render: (r) => (
        <div className="adm-cell-flex"><Thumb url={r.imageUrl} fallback="🗂️" />
          <div><div className="adm-cell-main">{r.name}</div><div className="adm-cell-sub">{r.description}</div></div></div>
      ),
    },
    { key: 'itemCount', label: 'Số món đang bán', align: 'right' },
  ],
};

// ---------- Nguyên liệu ----------
export const ingredients = {
  key: 'ingredients', base: '/admin/ingredients', endpoint: '/ingredients', title: 'Nguyên liệu', singular: 'nguyên liệu', icon: '🥬',
  titleOf: (r) => r.name,
  filters: [{ name: 'lowStock', label: 'Mọi mức tồn', options: [{ value: 'true', label: 'Sắp hết hàng' }, { value: 'false', label: 'Còn đủ' }] }],
  fields: [
    { name: 'name', label: 'Tên nguyên liệu', required: true },
    { name: 'unit', label: 'Đơn vị tính', required: true, placeholder: 'kg, lít, gói…' },
    { name: 'stockQty', label: 'Tồn kho hiện tại', type: 'number', min: 0, step: 0.001, default: 0, hint: 'Thường được cộng tự động qua Phiếu nhập; chỉ sửa tay khi kiểm kê.' },
    { name: 'minStock', label: 'Mức tồn tối thiểu', type: 'number', min: 0, step: 0.001, default: 0, hint: 'Tồn kho ≤ mức này sẽ báo "Sắp hết".' },
  ],
  columns: [
    { key: 'id', label: 'Mã' },
    { key: 'name', label: 'Nguyên liệu', render: (r) => <span className="adm-cell-main">{r.name}</span> },
    { key: 'unit', label: 'Đơn vị' },
    { key: 'stockQty', label: 'Tồn kho', align: 'right', render: (r) => num(r.stockQty) },
    { key: 'minStock', label: 'Tối thiểu', align: 'right', render: (r) => num(r.minStock) },
    { key: 'lowStock', label: 'Tình trạng', render: (r) => (r.lowStock ? <Badge label="Sắp hết" color="red" /> : <Badge label="Còn đủ" color="green" />) },
  ],
};

// ---------- Nhà cung cấp ----------
export const suppliers = {
  key: 'suppliers', base: '/admin/suppliers', endpoint: '/suppliers', title: 'Nhà cung cấp', singular: 'nhà cung cấp', icon: '🚚',
  titleOf: (r) => r.name,
  fields: [
    { name: 'name', label: 'Tên nhà cung cấp', required: true },
    { name: 'phone', label: 'Điện thoại', type: 'tel', required: true },
    { name: 'email', label: 'Email', type: 'email' },
    { name: 'address', label: 'Địa chỉ', type: 'textarea' },
  ],
  columns: [
    { key: 'id', label: 'Mã' },
    { key: 'name', label: 'Tên', render: (r) => <span className="adm-cell-main">{r.name}</span> },
    { key: 'phone', label: 'Điện thoại' }, { key: 'email', label: 'Email' }, { key: 'address', label: 'Địa chỉ' },
  ],
};

// ---------- Phiếu nhập (chỉ tạo + xem) ----------
export const receipts = {
  key: 'receipts', base: '/admin/receipts', endpoint: '/receipts', title: 'Phiếu nhập kho', singular: 'phiếu nhập', icon: '📥',
  canDelete: false, openLabel: 'Xem', createLabel: 'Lập phiếu nhập',
  titleOf: (r) => `Phiếu nhập #${r.id}`,
  sort: (a, b) => new Date(b.receiptDate) - new Date(a.receiptDate) || b.id - a.id,
  searchText: (r) => r.details.map((d) => d.ingredientName).join(' '),
  columns: [
    { key: 'id', label: 'Mã phiếu', render: (r) => `#${r.id}` },
    { key: 'receiptDate', label: 'Ngày nhập', render: (r) => dt(r.receiptDate) },
    { key: 'supplierName', label: 'Nhà cung cấp' },
    { key: 'employeeName', label: 'Người lập', render: (r) => r.employeeName ?? '—' },
    { key: 'lines', label: 'Số dòng', align: 'right', render: (r) => r.details.length },
    { key: 'totalAmount', label: 'Tổng tiền', align: 'right', render: (r) => <b>{vnd(r.totalAmount)}</b> },
  ],
};

// ---------- Bàn ----------
export const tables = {
  key: 'tables', base: '/admin/tables', endpoint: '/tables', title: 'Sơ đồ bàn', singular: 'bàn', icon: '🪑',
  trash: true, softDelete: true,
  titleOf: (r) => `Bàn ${r.tableCode}`,
  lookups: { ...L('reservations'), ...L('customers') },
  sort: bySortKey('tableCode'),
  filters: [{ name: 'status', label: 'Mọi trạng thái', options: toOptions(TABLE_STATUSES) }],
  fields: [
    { name: 'tableCode', label: 'Mã bàn', required: true, placeholder: 'T01, VIP1…' },
    { name: 'capacity', label: 'Sức chứa (người)', type: 'number', min: 1, required: true },
    { name: 'zone', label: 'Khu vực', required: true, placeholder: 'Tầng 1, Sân vườn, VIP…' },
    { name: 'status', label: 'Trạng thái', options: toOptions(TABLE_STATUSES), default: 'AVAILABLE' },
  ],
  columns: [],
  cards: (t, ctx) => <TableCard t={t} reload={ctx.reload} lk={ctx.lk} />,
};

// ---------- Nhân viên ----------
export const employees = {
  key: 'employees', base: '/admin/employees', endpoint: '/employees', title: 'Nhân viên', singular: 'nhân viên', icon: '🧑‍🍳',
  trash: true, softDelete: true,
  titleOf: (r) => r.fullName,
  sort: (a, b) => a.id - b.id,
  filters: [{ name: 'role', label: 'Mọi chức vụ', options: toOptions(ROLES) }],
  fields: [
    { name: 'fullName', label: 'Họ tên', required: true },
    { name: 'username', label: 'Tên đăng nhập', required: true },
    { name: 'password', label: 'Mật khẩu', type: 'password', requiredOnCreate: true, hint: 'Khi sửa: để trống nếu không đổi mật khẩu.' },
    { name: 'role', label: 'Chức vụ', options: toOptions(ROLES), required: true },
    { name: 'phone', label: 'Điện thoại', type: 'tel', required: true },
    { name: 'salary', label: 'Lương (₫)', type: 'number', min: 0, step: 100000, required: true },
  ],
  columns: [
    { key: 'id', label: 'Mã' },
    { key: 'fullName', label: 'Họ tên', render: (r) => <span className="adm-cell-main">{r.fullName}</span> },
    { key: 'username', label: 'Tài khoản' },
    { key: 'role', label: 'Chức vụ', render: (r) => <Badge value={r.role} /> },
    { key: 'phone', label: 'Điện thoại' },
    { key: 'salary', label: 'Lương', align: 'right', render: (r) => vnd(r.salary) },
  ],
};

// ---------- Khách hàng (trang chi tiết có thêm danh sách đơn đã mua) ----------
export const customers = {
  key: 'customers', base: '/admin/customers', endpoint: '/customers', title: 'Khách hàng', singular: 'khách hàng', icon: '👥',
  trash: true, softDelete: true,
  titleOf: (r) => r.fullName,
  sort: (a, b) => a.id - b.id,
  fields: [
    { name: 'fullName', label: 'Họ tên', required: true },
    { name: 'phone', label: 'Điện thoại', type: 'tel', required: true },
    { name: 'email', label: 'Email', type: 'email' },
    { name: 'loyaltyPoints', label: 'Điểm tích lũy', type: 'number', min: 0, hideOnCreate: true },
  ],
  columns: [
    { key: 'id', label: 'Mã' },
    { key: 'fullName', label: 'Họ tên', render: (r) => <span className="adm-cell-main">{r.fullName}</span> },
    { key: 'phone', label: 'Điện thoại' }, { key: 'email', label: 'Email' },
    { key: 'loyaltyPoints', label: 'Điểm tích lũy', align: 'right', render: (r) => <b>{r.loyaltyPoints ?? 0}</b> },
  ],
};

// ---------- Đặt bàn (không xóa; kết thúc bằng Nhận bàn / Hoàn tất / Hủy) ----------
// Trang tạo / sửa dùng components/admin/ReservationForm.jsx (có gợi ý bàn trống theo giờ)
const sameDay = (iso, d = new Date()) => new Date(iso).toDateString() === d.toDateString();
export const reservations = {
  key: 'reservations', base: '/admin/reservations', endpoint: '/reservations', title: 'Đặt bàn', singular: 'lượt đặt bàn', icon: '📅',
  canDelete: false, openLabel: 'Chi tiết', createLabel: 'Đặt bàn mới',
  titleOf: (r) => `Lượt đặt #${r.id}`,
  lookups: { ...L('customers'), ...L('tables') },
  sort: (a, b) => new Date(a.reservedAt) - new Date(b.reservedAt),
  searchText: (r, lk) => `${nameOf(lk, 'customers', r.customerId)} ${nameOf(lk, 'tables', r.tableId)} ${r.note ?? ''}`,
  filters: [
    { name: 'status', label: 'Mọi trạng thái', options: toOptions(RESERVATION_STATUSES) },
    {
      name: 'when', label: 'Mọi thời gian',
      options: [{ value: 'today', label: 'Hôm nay' }, { value: 'upcoming', label: 'Sắp tới' }, { value: 'overdue', label: 'Quá giờ chưa đến' }],
      test: (r, v) => (v === 'today' ? sameDay(r.reservedAt)
        : v === 'upcoming' ? ['PENDING', 'CONFIRMED'].includes(r.status) && new Date(r.reservedAt) >= new Date()
          : isOverdue(r)),
    },
  ],
  summary: (data) => {
    const today = data.filter((r) => sameDay(r.reservedAt) && r.status !== 'CANCELLED');
    const chips = [
      ['Hôm nay', today.length, 'blue'],
      ['Chờ xác nhận', data.filter((r) => r.status === 'PENDING').length, 'amber'],
      ['Đã xác nhận', data.filter((r) => r.status === 'CONFIRMED').length, 'green'],
      ['Quá giờ chưa đến', data.filter(isOverdue).length, 'red'],
    ];
    return <div className="adm-chips">{chips.map(([l, n, c]) => <span key={l} className={`adm-chip ${c}`}>{l}: <b>{n}</b></span>)}</div>;
  },
  columns: [
    { key: 'id', label: 'Mã', render: (r) => `#${r.id}` },
    { key: 'customerId', label: 'Khách hàng', render: (r, { lk }) => <Link href={`/admin/customers/${r.customerId}`} className="adm-link">{nameOf(lk, 'customers', r.customerId)}</Link> },
    { key: 'tableId', label: 'Bàn', render: (r, { lk }) => <Link href={`/admin/tables/${r.tableId}`} className="adm-link">{nameOf(lk, 'tables', r.tableId)}</Link> },
    { key: 'reservedAt', label: 'Thời gian', render: (r) => <span className="adm-nowrap">{dt(r.reservedAt)}</span> },
    { key: 'guestCount', label: 'Số khách', align: 'right' },
    { key: 'note', label: 'Ghi chú', render: (r) => <span className="adm-cell-sub">{r.note ?? ''}</span> },
    { key: 'status', label: 'Trạng thái', render: (r) => <><Badge value={r.status} />{isOverdue(r) && <> <Badge label="Quá giờ" color="red" /></>}</> },
  ],
  rowActions: (r, ctx) => <ReservationActions r={r} reload={ctx.reload} />,
};

// ---------- Đơn hàng (tạo ở trang create riêng; xem chi tiết ở [id]) ----------
export const orders = {
  key: 'orders', base: '/admin/orders', endpoint: '/orders', title: 'Đơn hàng', singular: 'đơn hàng', icon: '🧾',
  canDelete: false, openLabel: 'Chi tiết', createLabel: 'Tạo đơn mới',
  titleOf: (r) => `Đơn #${r.id}`,
  lookups: { ...L('customers'), ...L('tables'), ...L('employees') },
  searchText: (r, lk) => `${nameOf(lk, 'customers', r.customerId, '')} ${nameOf(lk, 'tables', r.tableId, '')} ${nameOf(lk, 'employees', r.employeeId, '')}`,
  filters: [{ name: 'status', label: 'Mọi trạng thái', options: toOptions(ORDER_STATUSES) }],
  columns: [
    { key: 'id', label: 'Mã đơn', render: (r) => <b>#{r.id}</b> },
    {
      key: 'customerId', label: 'Khách hàng', render: (r, { lk }) => (r.customerId
        ? <Link href={`/admin/customers/${r.customerId}`} className="adm-link">{nameOf(lk, 'customers', r.customerId)}</Link>
        : <span className="adm-muted">Khách vãng lai</span>),
    },
    { key: 'tableId', label: 'Bàn', render: (r, { lk }) => nameOf(lk, 'tables', r.tableId) },
    { key: 'employeeId', label: 'Nhân viên', render: (r, { lk }) => nameOf(lk, 'employees', r.employeeId) },
    { key: 'createdAt', label: 'Tạo lúc', render: (r) => dt(r.createdAt) },
    { key: 'itemCount', label: 'Số món', align: 'right', render: (r) => r.itemCount ?? 0 },
    { key: 'total', label: 'Tổng tiền', align: 'right', render: (r) => <b>{vnd(r.total)}</b> },
    { key: 'status', label: 'Trạng thái', render: (r) => <Badge value={r.status} /> },
  ],
};

// ---------- Hóa đơn (chỉ xem + in; được tạo khi thanh toán đơn hàng) ----------
export const invoices = {
  key: 'invoices', base: '/admin/invoices', endpoint: '/invoices', title: 'Hóa đơn', singular: 'hóa đơn', icon: '💳',
  canCreate: false, canDelete: false, openLabel: 'Xem',
  titleOf: (r) => `Hóa đơn #${r.id}`,
  searchText: (r) => paymentLabel(r.paymentMethod),
  columns: [
    { key: 'id', label: 'Mã HĐ', render: (r) => <b>#{r.id}</b> },
    { key: 'orderId', label: 'Đơn hàng', render: (r) => <Link href={`/admin/orders/${r.orderId}`} className="adm-link">Đơn #{r.orderId}</Link> },
    { key: 'subtotal', label: 'Tạm tính', align: 'right', render: (r) => vnd(r.subtotal) },
    { key: 'discount', label: 'Giảm giá', align: 'right', render: (r) => vnd(r.discount) },
    { key: 'total', label: 'Tổng cộng', align: 'right', render: (r) => <b>{vnd(r.total)}</b> },
    { key: 'paymentMethod', label: 'Thanh toán', render: (r) => paymentLabel(r.paymentMethod) },
    { key: 'paidAt', label: 'Thời gian', render: (r) => dt(r.paidAt) },
  ],
};

// Các loại có thùng rác (dùng cho trang /admin/trash)
export const TRASH_TYPES = [menu, customers, employees, tables];
