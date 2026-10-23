import ShopHeader from './ShopHeader';
import ShopFooter from './ShopFooter';
export default function ShopLayout({ children }) {
  return <div className="shop-shell"><ShopHeader /><main>{children}</main><ShopFooter /></div>;
}
