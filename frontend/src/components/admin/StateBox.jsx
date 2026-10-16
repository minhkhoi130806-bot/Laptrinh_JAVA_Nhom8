// Hiển thị trạng thái đang tải / lỗi kết nối. Trả về null khi dữ liệu đã sẵn sàng.
export default function StateBox({ loading, error, onRetry }) {
  if (error) {
    return (
      <div className="adm-alert">
        <span>Không tải được dữ liệu: {error}. Hãy kiểm tra Spring Boot (cổng 8080) và MySQL.</span>
        {onRetry && <button className="adm-btn ghost sm" onClick={onRetry}>Thử lại</button>}
      </div>
    );
  }
  if (loading) return <div className="adm-muted adm-pad">Đang tải…</div>;
  return null;
}
