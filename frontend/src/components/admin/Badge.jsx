import { STATUS } from '@/lib/constants';

// <Badge value="PENDING" />  hoặc  <Badge label="Sắp hết" color="red" />
export default function Badge({ value, label, color }) {
  const [text, col] = label ? [label, color || 'gray'] : STATUS[value] || [value ?? '—', 'gray'];
  return <span className={`adm-badge ${col}`}>{text}</span>;
}
