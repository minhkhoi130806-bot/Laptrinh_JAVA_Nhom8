'use client';

import { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import Icon from '@/components/ui/Icon';

const INITIAL_SUGGESTIONS = [
  { label: '🍲 Món ăn nổi tiếng nhất', text: 'Gợi ý cho tôi các món ăn đặc sắc nhất của nhà hàng' },
  { label: '📅 Hướng dẫn đặt bàn', text: 'Tôi muốn đặt bàn thì làm thế nào?' },
  { label: '⭐ Quyền lợi hội viên & Gmail', text: 'Đăng ký thành viên có ưu đãi gì và nhận hóa đơn qua Gmail ra sao?' },
  { label: '💳 Cách thanh toán online', text: 'Nhà hàng hỗ trợ những phương thức thanh toán nào?' },
  { label: '📍 Giờ mở cửa & Địa chỉ', text: 'Nhà hàng mở cửa lúc mấy giờ và hotline là gì?' },
];

function generateAiReply(query, userName) {
  const q = query.toLowerCase().trim();
  const greeting = userName ? `Chào bạn ${userName}! ` : 'Dạ chào quý khách! ';

  // 1. Món ăn, thực đơn, khuyến nghị
  if (
    q.includes('món') ||
    q.includes('thực đơn') ||
    q.includes('ăn gì') ||
    q.includes('ngon') ||
    q.includes('thịt nướng') ||
    q.includes('bò') ||
    q.includes('heo') ||
    q.includes('samgyeopsal') ||
    q.includes('kim chi') ||
    q.includes('cơm trộn') ||
    q.includes('bibimbap') ||
    q.includes('mì lạnh') ||
    q.includes('tokbokki') ||
    q.includes('soju')
  ) {
    return {
      text: `${greeting}Tại Han Seoul, các món ăn được yêu thích nhất gồm có:
• 🥩 **Thịt nướng Samgyeopsal:** Thịt ba chỉ heo tươi cuộn lá mè, sốt ssamjang chuẩn vị Hàn.
• 🥩 **Bò Wagyu sốt tương nướng than:** Thơm mềm, ngọt thịt đậm đà.
• 🍲 **Canh kim chi Jjigae hải sản:** Cay nồng ấm cúng, đậm đà vị truyền thống.
• 🍚 **Cơm trộn Thố đá Bibimbap:** Giòn rụm đáy thố, dồi dào rau nấm và tương ớt Gochujang.
• 🥘 **Bánh gạo cay Tokbokki phô mai & Rượu Soju / Makgeolli**.`,
      action: { href: '/thuc-don', label: 'Xem Thực đơn đầy đủ ↗' },
    };
  }

  // 2. Đặt bàn, giữ chỗ
  if (
    q.includes('đặt bàn') ||
    q.includes('giữ chỗ') ||
    q.includes('bàn') ||
    q.includes('tiệc') ||
    q.includes('sinh nhật') ||
    q.includes('hẹn')
  ) {
    return {
      text: `${greeting}Để đặt bàn nhanh chóng, quý khách chỉ cần vào mục Đặt bàn trực tuyến:
1. Chọn ngày, giờ và số lượng khách (gia đình, bạn bè hoặc tiệc nhóm).
2. Điền số điện thoại và ghi chú (ghế trẻ em, phòng riêng, trang trí sinh nhật...).
3. Nhà hàng sẽ giữ bàn chu đáo và xác nhận tức thì qua hệ thống!`,
      action: { href: '/dat-ban', label: 'Đặt bàn ngay bây giờ ↗' },
    };
  }

  // 3. Hội viên, Gmail, Tích điểm
  if (
    q.includes('hội viên') ||
    q.includes('thành viên') ||
    q.includes('đăng ký') ||
    q.includes('tài khoản') ||
    q.includes('gmail') ||
    q.includes('email') ||
    q.includes('tích điểm') ||
    q.includes('điểm') ||
    q.includes('hóa đơn')
  ) {
    return {
      text: `${greeting}Khi đăng ký thành viên tại Han Seoul, bạn sẽ nhận ngay 4 đặc quyền:
• 🎁 **Tặng ngay 50 điểm thưởng chào mừng** được cộng thẳng vào tài khoản.
• 🏷️ **Giảm giá 10%** cho đơn hàng đặt món trực tuyến đầu tiên.
• 📧 **Hóa đơn điện tử qua Gmail:** Mỗi lần thanh toán đơn trực tuyến (ZaloPay, VNPay), hóa đơn chi tiết tự động gửi về Gmail của bạn.
• ⭐ **Tích lũy điểm đổi quà:** Cứ mỗi 10.000 ₫ thanh toán được tích lũy 1 điểm thưởng.`,
      action: { href: '/dang-ky', label: 'Đăng ký thành viên (30 giây) ↗' },
    };
  }

  // 4. Thanh toán online: ZaloPay, VNPay
  if (
    q.includes('thanh toán') ||
    q.includes('zalopay') ||
    q.includes('vnpay') ||
    q.includes('tiền mặt') ||
    q.includes('chuyển khoản') ||
    q.includes('ngân hàng') ||
    q.includes('qr')
  ) {
    return {
      text: `${greeting}Han Seoul hỗ trợ các phương thức thanh toán an toàn, bảo mật cao:
• 📱 **Ví ZaloPay:** Quét mã QR tiện lợi, liên kết ví ZaloPay và thẻ ngân hàng.
• 💳 **Cổng VNPay:** Hỗ trợ quét mã VNPAY-QR từ hơn 40 ứng dụng ngân hàng, thẻ ATM, Visa, MasterCard.
• 💵 **Tiền mặt / Chuyển khoản:** Thanh toán linh hoạt tại quầy hoặc khi nhận món tận nơi (cho phép khách vãng lai bình thường, không gửi mail làm phiền).
Sau khi thanh toán online thành công, hóa đơn chi tiết sẽ được tự động gửi về Gmail của bạn!`,
      action: { href: '/thuc-don', label: 'Đặt món & Trải nghiệm thanh toán ↗' },
    };
  }

  // 5. Giờ mở cửa, hotline, địa chỉ
  if (
    q.includes('giờ') ||
    q.includes('mở cửa') ||
    q.includes('đóng cửa') ||
    q.includes('địa chỉ') ||
    q.includes('ở đâu') ||
    q.includes('hotline') ||
    q.includes('liên hệ') ||
    q.includes('sđt')
  ) {
    return {
      text: `${greeting}Thông tin hoạt động của Nhà hàng Han Seoul:
• ⏰ **Giờ phục vụ:** 10:00 – 22:00 (Tất cả các ngày trong tuần, kể cả Lễ, Tết).
• 📞 **Hotline hỗ trợ:** 0900 000 888.
• ✉️ **Email tiếp nhận:** hello@hanseoul.vn.
• 📍 **Địa chỉ:** Tổ hợp Ẩm thực Hàn Quốc Han Seoul Korean Dining.`,
      action: { href: '/lien-he', label: 'Xem trang Liên hệ ↗' },
    };
  }

  // 6. Tra cứu đơn hàng
  if (q.includes('đơn hàng') || q.includes('tra cứu') || q.includes('giao hàng') || q.includes('ship')) {
    return {
      text: `${greeting}Quý khách có thể tra cứu đơn hàng trực tiếp bằng cách nhập Mã đơn hàng và Số điện thoại đã đặt để kiểm tra tình trạng chế biến và giao món.`,
      action: { href: '/don-hang', label: 'Tra cứu đơn hàng ↗' },
    };
  }

  // Mặc định
  return {
    text: `${greeting}Em có thể giải đáp chi tiết về thực đơn món Hàn, đặt bàn trước, đăng ký hội viên nhận hóa đơn qua Gmail và hỗ trợ thanh toán online. Quý khách vui lòng chọn câu hỏi gợi ý bên dưới hoặc gõ trực tiếp câu hỏi nhé!`,
    action: { href: '/thuc-don', label: 'Khám phá Thực đơn ↗' },
  };
}

export default function AiCustomerChatbot() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [typing, setTyping] = useState(false);
  const [userName, setUserName] = useState('');
  const messagesEndRef = useRef(null);

  useEffect(() => {
    try {
      const user = JSON.parse(localStorage.getItem('currentUser') || 'null');
      if (user) {
        setUserName(user.fullName || user.name || '');
      }
    } catch {}
  }, []);

  useEffect(() => {
    if (messages.length === 0) {
      const greetingName = userName ? ` ${userName}` : '';
      setMessages([
        {
          sender: 'ai',
          time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
          text: `Xin chào quý khách${greetingName}! 👋\nEm là **Trợ lý AI Han Seoul 🤖**.\nEm có thể giúp bạn tìm món ngon, hướng dẫn đặt bàn, đăng ký thành viên nhận hóa đơn Gmail và thanh toán online. Bạn cần em hỗ trợ gì ạ?`,
        },
      ]);
    }
  }, [userName, messages.length]);

  useEffect(() => {
    if (open) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, typing, open]);

  function handleSend(textToSend) {
    const query = (textToSend || input).trim();
    if (!query) return;

    const time = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    const userMsg = { sender: 'user', time, text: query };

    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setTyping(true);

    setTimeout(() => {
      const reply = generateAiReply(query, userName);
      const aiMsg = {
        sender: 'ai',
        time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        text: reply.text,
        action: reply.action,
      };
      setMessages((prev) => [...prev, aiMsg]);
      setTyping(false);
    }, 600);
  }

  return (
    <>
      {/* NÚT BẤM NỔI DƯỚI GÓC PHẢI */}
      <div className="ai-chatbot-launcher-wrap" style={{ position: 'fixed', bottom: '24px', right: '24px', zIndex: 9999 }}>
        {!open ? (
          <button
            type="button"
            onClick={() => setOpen(true)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              background: 'linear-gradient(135deg, #8B2500 0%, #b83a1b 100%)',
              color: '#ffffff',
              border: 'none',
              borderRadius: '32px',
              padding: '10px 18px',
              cursor: 'pointer',
              boxShadow: '0 8px 25px rgba(139, 37, 0, 0.35)',
              transition: 'transform 0.2s, box-shadow 0.2s',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.transform = 'translateY(-2px)')}
            onMouseLeave={(e) => (e.currentTarget.style.transform = 'translateY(0)')}
            aria-label="Mở Trợ lý AI Han Seoul"
          >
            <span style={{ fontSize: '22px', display: 'grid', placeItems: 'center' }}>🤖</span>
            <div style={{ textAlign: 'left', lineHeight: '1.2' }}>
              <div style={{ fontSize: '13px', fontWeight: 700, letterSpacing: '0.2px' }}>Trợ lý AI Han Seoul</div>
              <div style={{ fontSize: '10.5px', opacity: 0.88 }}>● Trực tuyến 24/7 hỗ trợ bạn</div>
            </div>
          </button>
        ) : null}
      </div>

      {/* CỬA SỔ CHAT TRỢ LÝ AI */}
      {open && (
        <div
          style={{
            position: 'fixed',
            bottom: '24px',
            right: '24px',
            width: '380px',
            maxWidth: 'calc(100vw - 32px)',
            height: '560px',
            maxHeight: 'calc(100vh - 48px)',
            background: '#ffffff',
            borderRadius: '20px',
            boxShadow: '0 16px 48px rgba(0, 0, 0, 0.2)',
            border: '1px solid #e2e8f0',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            zIndex: 10000,
            animation: 'fadeInUp 0.25s ease',
          }}
        >
          {/* HEADER */}
          <div
            style={{
              background: 'linear-gradient(135deg, #173c32 0%, #264e43 100%)',
              color: '#ffffff',
              padding: '14px 18px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div
                style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '50%',
                  background: '#e2c89b',
                  color: '#173c32',
                  display: 'grid',
                  placeItems: 'center',
                  fontSize: '20px',
                  boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
                }}
              >
                🤖
              </div>
              <div>
                <div style={{ fontSize: '14px', fontWeight: 700, color: '#e2c89b' }}>Trợ lý AI Han Seoul</div>
                <div style={{ fontSize: '11px', color: '#c6f6d5', display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: '#48bb78', display: 'inline-block' }}></span>
                  Trực tuyến · AI tư vấn ẩm thực
                </div>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setOpen(false)}
              style={{
                background: 'rgba(255,255,255,0.12)',
                border: 'none',
                color: '#ffffff',
                borderRadius: '50%',
                width: '30px',
                height: '30px',
                cursor: 'pointer',
                fontSize: '18px',
                display: 'grid',
                placeItems: 'center',
              }}
              title="Đóng cửa sổ chat"
            >
              ✕
            </button>
          </div>

          {/* NỘI DUNG TIN NHẮN */}
          <div
            style={{
              flex: 1,
              overflowY: 'auto',
              padding: '16px',
              background: '#f8fafc',
              display: 'flex',
              flexDirection: 'column',
              gap: '12px',
            }}
          >
            {messages.map((m, idx) => (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: m.sender === 'user' ? 'flex-end' : 'flex-start',
                }}
              >
                <div
                  style={{
                    maxWidth: '85%',
                    padding: '10px 14px',
                    borderRadius: m.sender === 'user' ? '14px 14px 2px 14px' : '14px 14px 14px 2px',
                    background: m.sender === 'user' ? '#8B2500' : '#ffffff',
                    color: m.sender === 'user' ? '#ffffff' : '#2d3748',
                    fontSize: '13px',
                    lineHeight: '1.55',
                    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
                    border: m.sender === 'user' ? 'none' : '1px solid #edf2f7',
                    whiteSpace: 'pre-line',
                  }}
                >
                  {m.text}

                  {m.action && (
                    <div style={{ marginTop: '8px', paddingTop: '8px', borderTop: '1px dashed #e2e8f0' }}>
                      <Link
                        href={m.action.href}
                        onClick={() => setOpen(false)}
                        style={{
                          display: 'inline-block',
                          color: '#8B2500',
                          fontWeight: 700,
                          fontSize: '12px',
                          textDecoration: 'none',
                          background: '#fff5f5',
                          padding: '4px 10px',
                          borderRadius: '6px',
                          border: '1px solid #fed7d7',
                        }}
                      >
                        {m.action.label}
                      </Link>
                    </div>
                  )}
                </div>
                <span style={{ fontSize: '10px', color: '#a0aec0', marginTop: '3px', paddingInline: '4px' }}>
                  {m.time}
                </span>
              </div>
            ))}

            {typing && (
              <div style={{ alignSelf: 'flex-start', background: '#ffffff', padding: '8px 14px', borderRadius: '12px', fontSize: '12px', color: '#718096', border: '1px solid #e2e8f0' }}>
                AI đang suy nghĩ câu trả lời... ⏳
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* GỢI Ý NHANH (CHIPS) */}
          <div
            style={{
              padding: '8px 12px',
              background: '#ffffff',
              borderTop: '1px solid #edf2f7',
              display: 'flex',
              gap: '6px',
              overflowX: 'auto',
              whiteSpace: 'nowrap',
            }}
          >
            {INITIAL_SUGGESTIONS.map((s, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => handleSend(s.text)}
                style={{
                  background: '#f1f5f9',
                  border: '1px solid #e2e8f0',
                  borderRadius: '16px',
                  padding: '4px 10px',
                  fontSize: '11px',
                  color: '#475569',
                  cursor: 'pointer',
                  flexShrink: 0,
                  transition: 'background 0.15s',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.background = '#e2e8f0')}
                onMouseLeave={(e) => (e.currentTarget.style.background = '#f1f5f9')}
              >
                {s.label}
              </button>
            ))}
          </div>

          {/* Ô NHẬP TIN NHẮN */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSend();
            }}
            style={{
              padding: '10px 12px',
              background: '#ffffff',
              borderTop: '1px solid #e2e8f0',
              display: 'flex',
              gap: '8px',
              alignItems: 'center',
            }}
          >
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Nhập câu hỏi cho AI Han Seoul..."
              style={{
                flex: 1,
                padding: '9px 14px',
                border: '1px solid #cbd5e0',
                borderRadius: '20px',
                fontSize: '13px',
                outline: 'none',
              }}
              onFocus={(e) => (e.currentTarget.style.borderColor = '#8B2500')}
              onBlur={(e) => (e.currentTarget.style.borderColor = '#cbd5e0')}
            />
            <button
              type="submit"
              disabled={!input.trim()}
              style={{
                background: input.trim() ? '#8B2500' : '#cbd5e0',
                color: '#ffffff',
                border: 'none',
                borderRadius: '50%',
                width: '36px',
                height: '36px',
                cursor: input.trim() ? 'pointer' : 'not-allowed',
                display: 'grid',
                placeItems: 'center',
                transition: 'background 0.2s',
              }}
              title="Gửi câu hỏi"
            >
              <Icon name="arrowUpRight" size={16} />
            </button>
          </form>
        </div>
      )}
    </>
  );
}
