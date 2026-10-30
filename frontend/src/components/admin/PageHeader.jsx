import Link from 'next/link';
import Icon from '@/components/ui/Icon';

// Tiêu đề trang: [← quay lại] + tiêu đề + mô tả + vùng nút thao tác (children)
export default function PageHeader({ title, icon, subtitle, back, children }) {
  return (
    <header className="adm-page-h">
      <div>
        {back && <Link href={back.href} className="adm-crumb"><Icon name="arrowLeft" size={14} />{back.label}</Link>}
        <h1>{icon && <span className="adm-page-icon"><Icon name={icon} size={21} /></span>}{title}</h1>
        {subtitle && <p className="adm-muted">{subtitle}</p>}
      </div>
      {children && <div className="adm-tools">{children}</div>}
    </header>
  );
}
