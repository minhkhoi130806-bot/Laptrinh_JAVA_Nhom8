// Ảnh vuông nhỏ (hoặc biểu tượng thay thế khi chưa có ảnh)
export default function Thumb({ url, fallback = '🍽️', size = '', alt = '' }) {
  return (
    <span className={`adm-thumb ${size}`}>
      {url
        // eslint-disable-next-line @next/next/no-img-element
        ? <img src={url} alt={alt} loading="lazy" />
        : fallback}
    </span>
  );
}
