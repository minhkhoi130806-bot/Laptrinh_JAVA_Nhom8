// Proxy sang Spring Boot (localhost:8080) để tránh lỗi CORS khi dev
//   /api/*      -> API JSON
//   /uploads/*  -> ảnh đã upload (backend lưu trong thư mục ./uploads)
const BACKEND = process.env.BACKEND_URL || 'http://localhost:8080';

export default {
  async rewrites() {
    return [
      { source: '/api/:path*', destination: `${BACKEND}/api/:path*` },
      { source: '/uploads/:path*', destination: `${BACKEND}/uploads/:path*` },
    ];
  },
};
