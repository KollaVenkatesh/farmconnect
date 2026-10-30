function ProductCard({ product, onAddToCart }) {
  return (
    <article className="market-product-card">
      <div className="market-product-emoji" aria-hidden="true">{product.emoji}</div>
      <div className="market-product-content">
        <div className="market-product-heading"><span className="product-category">{product.category}</span><h3>{product.name}</h3></div>
        <p className="farmer-details">👨‍🌾 {product.farmer} <span>·</span> {product.location}</p>
        <div className="market-product-footer"><strong>₹{product.price} <small>/ {product.unit}</small></strong><button type="button" onClick={() => onAddToCart(product)}>Add to cart</button></div>
      </div>
    </article>
  )
}
export default ProductCard
