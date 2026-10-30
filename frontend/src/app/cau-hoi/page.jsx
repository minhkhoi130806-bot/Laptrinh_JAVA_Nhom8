'use client';
import { useState } from 'react';
import Link from 'next/link';
import ShopLayout from '@/components/shop/ShopLayout';
const faqs=[
 ['Làm thế nào để đặt bàn?','Mở trang Đặt bàn, nhập thông tin liên hệ, thời gian và số lượng khách. Chọn bàn phù hợp trong danh sách bàn trống rồi gửi yêu cầu. Nhà hàng sẽ xác nhận theo quy trình vận hành.'],
 ['Tôi có thể thay đổi hoặc hủy đặt bàn không?','Hiện website hỗ trợ gửi yêu cầu đặt bàn mới. Chức năng khách tự đổi hoặc hủy bằng mã xác nhận chưa được kết nối với API công khai; vui lòng gọi hotline để nhân viên hỗ trợ.'],
 ['Website có nhận gọi món và thanh toán trực tuyến không?','Luồng hiện tại tập trung vào đặt bàn và xem thực đơn. Gọi món, quản lý món theo bàn và thanh toán được xử lý trong quy trình phục vụ tại nhà hàng, không phải giỏ hàng mua sắm trực tuyến.'],
 ['Làm sao biết món có cay không?','Một số món hiển thị thông tin cấp độ cay trên thẻ và trang chi tiết. Nếu bạn nhạy cảm với ớt hoặc gia vị, hãy báo nhân viên trước khi gọi món.'],
 ['Nhà hàng có thể hỗ trợ dị ứng thực phẩm không?','Vui lòng thông báo rõ dị ứng với nhân viên trước khi gọi món. Thông tin thành phần chi tiết cần được xác nhận với bếp; không nên chỉ dựa vào mô tả trên website.'],
 ['Danh sách yêu thích được lưu ở đâu?','Món yêu thích được lưu trên trình duyệt của thiết bị hiện tại. Dữ liệu không đồng bộ qua điện thoại hoặc máy tính khác và có thể mất khi xóa dữ liệu trình duyệt.'],
 ['Các mã ưu đãi trên website có tự áp dụng không?','Trang ưu đãi hiện cung cấp gợi ý tham khảo, chưa xác thực mã hoặc tự giảm giá hóa đơn. Hãy hỏi nhân viên về chương trình thực tế trước khi thanh toán.'],
 ['Tôi cần liên hệ nhà hàng bằng cách nào?','Bạn có thể gọi hotline 0900 000 888 hoặc vào trang Liên hệ để chuẩn bị email. Thông tin giờ phục vụ được hiển thị ở chân trang.'],
];
export default function FAQPage(){const [open,setOpen]=useState(0);return <ShopLayout><section className="shop-page-hero"><div className="shop-container"><span className="shop-eyebrow">HELP CENTER</span><h1>Câu hỏi <em>thường gặp</em></h1><p>Thông tin nhanh để bạn chuẩn bị cho bữa ăn tại Han Seoul.</p><div className="shop-breadcrumb"><Link href="/">Trang chủ</Link><span>/</span><span>Hỗ trợ</span></div></div></section><section className="shop-section shop-container shop-faq-layout"><div><span className="shop-eyebrow">NEED A HAND?</span><h2>Chưa tìm thấy câu trả lời?</h2><p>Hãy liên hệ trực tiếp với nhà hàng để được hỗ trợ theo trường hợp cụ thể.</p><Link className="shop-button" href="/lien-he">Liên hệ chúng tôi ↗</Link></div><div className="shop-faq-list">{faqs.map(([q,a],i)=><article className={`shop-faq-item ${open===i?'open':''}`} key={q}><button type="button" onClick={()=>setOpen(open===i?-1:i)} aria-expanded={open===i}><span>{q}</span><b>{open===i?'−':'+'}</b></button>{open===i&&<p>{a}</p>}</article>)}</div></section></ShopLayout>}
