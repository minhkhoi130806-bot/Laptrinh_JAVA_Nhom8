// Nhãn + màu của các trạng thái / enum. Dùng chung cho admin và shop.
// [nhãn, màu]  màu: green | amber | red | blue | gray
export const STATUS = {
  // Bàn
  AVAILABLE: ['Trống', 'green'], RESERVED: ['Đã đặt', 'amber'], OCCUPIED: ['Đang dùng', 'red'],
  // Đặt bàn
  PENDING: ['Chờ xác nhận', 'amber'], CONFIRMED: ['Đã xác nhận', 'blue'],
  CANCELLED: ['Đã hủy', 'gray'], COMPLETED: ['Hoàn tất', 'green'],
  // Đơn hàng
  OPEN: ['Mới tạo', 'blue'], SERVING: ['Đang phục vụ', 'amber'], PAID: ['Đã thanh toán', 'green'],
  // Nhân viên
  MANAGER: ['Quản lý', 'blue'], CASHIER: ['Thu ngân', 'green'], WAITER: ['Phục vụ', 'amber'], CHEF: ['Đầu bếp', 'red'],
  // Độ cay
  NONE: ['Không cay', 'gray'], MILD: ['Cay nhẹ', 'amber'], MEDIUM: ['Cay vừa', 'amber'], HOT: ['Cay nhiều', 'red'], EXTRA_HOT: ['Rất cay', 'red'],
};

export const toOptions = (keys) => keys.map((value) => ({ value, label: STATUS[value]?.[0] ?? value }));

export const TABLE_STATUSES = ['AVAILABLE', 'RESERVED', 'OCCUPIED'];
export const RESERVATION_STATUSES = ['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'];
export const ORDER_STATUSES = ['OPEN', 'SERVING', 'PAID', 'CANCELLED'];
export const ROLES = ['MANAGER', 'CASHIER', 'WAITER', 'CHEF'];
export const SPICE_LEVELS = ['NONE', 'MILD', 'MEDIUM', 'HOT', 'EXTRA_HOT'];

export const PAYMENT_METHODS = [
  { value: 'CASH', label: 'Tiền mặt' }, { value: 'CARD', label: 'Thẻ' },
  { value: 'TRANSFER', label: 'Chuyển khoản' }, { value: 'MOMO', label: 'Ví MoMo' },
];
export const paymentLabel = (v) => PAYMENT_METHODS.find((m) => m.value === v)?.label ?? v ?? '—';
