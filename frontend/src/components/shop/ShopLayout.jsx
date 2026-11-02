import ShopHeader from './ShopHeader';
import ShopFooter from './ShopFooter';
import AiCustomerChatbot from './AiCustomerChatbot';

export default function ShopLayout({ children }) {
  return (
    <div className="shop-shell">
      <ShopHeader />
      <main>{children}</main>
      <ShopFooter />
      <AiCustomerChatbot />
    </div>
  );
}
