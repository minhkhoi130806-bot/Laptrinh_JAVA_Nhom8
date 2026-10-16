import { redirect } from 'next/navigation';

// Trang gốc "/" chuyển thẳng vào khu quản lý
export default function Home() {
  redirect('/admin');
}
