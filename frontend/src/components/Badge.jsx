import { STATUS } from '../lib/api';
export default function Badge({ value }) {
  const [label, color] = STATUS[value] || [value ?? '—', 'gray'];
  return <span className={`badge ${color}`}>{label}</span>;
}
