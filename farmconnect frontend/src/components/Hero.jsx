
function Hero({ onExplore }) {
  return (
    <section className="hero">
      <div className="hero-content">
        <p className="hero-label">
          🌱 WELCOME TO FARMCONNECT
        </p>

        <h1>
          Fresh Produce.
          <br />
          Direct From Farmers.
        </h1>

        <p className="hero-description">
          Connect with farmers, discover fresh agricultural
          products, and explore a simple way to support
          direct farm-to-buyer sales.
        </p>

        <div className="hero-buttons">
          <button
            className="primary-btn"
            onClick={onExplore}
          >
            Explore Products →
          </button>

          <button
            className="secondary-btn"
            onClick={() => alert("Farmer registration coming soon!")}
          >
            Join as Farmer
          </button>
        </div>
      </div>

      <div className="hero-visual">
        <div className="hero-circle">
          <span>👨‍🌾</span>
          <span>🌾</span>
          <span>🥬</span>
          <span>🍅</span>
        </div>
      </div>
    </section>
  );
}

export default Hero;