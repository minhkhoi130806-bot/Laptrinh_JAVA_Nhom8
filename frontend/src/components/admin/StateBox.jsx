import Icon from '@/components/ui/Icon';

// Hiển thị trạng thái đang tải / lỗi kết nối. Trả về null khi dữ liệu đã sẵn sàng.
export default function StateBox({ loading, error, onRetry }) {
  if (error) {
    return (
      <div className="adm-alert">
        <div className="adm-alert-copy"><Icon name="globe" size={20} /><span><b>Chưa thể tải dữ liệu</b><small>{error}. Vui lòng kiểm tra kết nối và thử lại.</small></span></div>
        {onRetry && <button className="adm-btn ghost sm" onClick={onRetry}>Thử lại</button>}
      </div>
    );
  }
  if (loading) return <div className="adm-loading" role="status"><span className="adm-loading-ring" aria-hidden="true" />Đang chuẩn bị dữ liệu…</div>;
  return null;
}
