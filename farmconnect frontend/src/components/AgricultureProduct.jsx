function AgricultureProduct({ product, onAddToCart }) {
  const { name, category, price, unit, farmer, location, emoji } = product;

  return (
    <article className="market-product-card">
      <div className="market-product-emoji" aria-hidden="true">
        {emoji}
      </div>
      <div className="market-product-content">
        <div className="market-product-heading">
          <span className="product-category">{category}</span>
          <h3>{name}</h3>
        </div>
        <p className="farmer-details">
          <span aria-hidden="true">👨‍🌾</span> {farmer} <span>·</span> {location}
        </p>
        <div className="market-product-footer">
          <strong>
            ₹{price} <small>/ {unit}</small>
          </strong>
          <button type="button" onClick={() => onAddToCart(product)}>
            Add to cart
          </button>
        </div>
      </div>
    </article>
  );
}

export default AgricultureProduct;