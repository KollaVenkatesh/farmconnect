import { useState } from "react";
import FarmConnectIcon from "./FarmConnectIcon";

const howItWorksSteps = [
  {
    title: "Discover local farms",
    summary: "Find seasonal produce from growers in your community.",
    detail: "Explore nearby farms, learn about the people who grow your food, and see what each farm is harvesting.",
  },
  {
    title: "Choose what’s in season",
    summary: "Explore fresh harvests with clear origins and fair prices.",
    detail: "Browse produce at its freshest, check where it came from, and compare clear prices before you choose.",
  },
  {
    title: "Make a closer connection",
    summary: "Support local farmers with every thoughtful purchase.",
    detail: "Every purchase helps local farmers earn a fair return and keeps more of your food spending in the community.",
  },
];

function BrandMark() {
  return <span className="brand-mark"><FarmConnectIcon /></span>;
}

function Home({ onSignIn, onSignUp }) {
  const [activeStep, setActiveStep] = useState(null);

  return (
    <div className="landing-page" id="home">
      <header className="site-header">
        <div className="header-inner">
          <a className="brand" href="#home" aria-label="FarmConnect home"><BrandMark /><span className="brand-name">Farm<span className="brand-accent">Connect</span></span></a>
          <nav className="landing-nav" aria-label="Main navigation">
            <a className="nav-current" href="#home">Home</a><a href="#how-it-works">How it works</a>
          </nav>
        </div>
      </header>

      <main>
        <section className="hero-section">
          <div className="hero-copy">
            <span className="eyebrow"><span className="eyebrow-dot" /> CONNECTING LOCAL FARMS WITH FRESH COMMUNITIES</span>
            <h1>Welcome to<br /><em>FarmConnect.</em></h1>
            <p className="hero-subtitle">Good food starts with a closer connection.</p>
            <p className="hero-lede">Meet the people who grow your food. Discover seasonal produce, fair prices, and a better way to shop local.</p>
            <div className="hero-actions"><button className="button button-dark button-large" onClick={onSignIn}>Sign In</button><button className="button button-signup button-large" onClick={onSignUp}>Sign Up</button></div>
            <div className="hero-proof"><div className="avatar-stack"><span>RK</span><span>AS</span><span>MV</span><span>+</span></div><p><strong>Growing together</strong><br />for a more connected food system</p></div>
          </div>
          <div className="hero-art" aria-label="Farmers growing and harvesting fresh produce">
            <img className="hero-photo" src="https://images.squarespace-cdn.com/content/v1/6257536342b010638376c856/3adaa712-2b65-4565-a3f7-387f24346531/In%2BGround%2BGarden%2BWatsonville.png" alt="Tomatoes, peppers, and melons growing on plants in a farm garden" />
            <div className="art-label label-top"><span className="label-icon">&#10033;</span><span><strong>Picked with care</strong><small>Fresh from the farm</small></span></div>
            <div className="art-label label-bottom"><span className="label-icon label-leaf">&#8599;</span><span><strong>Fair by nature</strong><small>Direct, transparent pricing</small></span></div>
          </div>
        </section>

        <section className="trust-strip"><span>GOOD FOOD, GROWN CLOSER</span><p>Small farms. Seasonal harvests. Stronger connections with local growers.</p><span className="trust-arrow">&#8595;</span></section>

        <section className="how-section" id="how-it-works"><div className="how-intro"><span className="eyebrow">A BETTER WAY TO BUY</span><h2>Good things happen<br />when we grow <em>together.</em></h2><p>FarmConnect makes it easier to know where your food comes from and who&#8217;s behind every harvest.</p><button className="button button-dark" onClick={onSignUp}>Join the community <span aria-hidden="true">&#8594;</span></button></div><div className="steps-list">{howItWorksSteps.map((step, index) => <article key={step.title}><button className="step-trigger" aria-expanded={activeStep === index} aria-controls={`step-detail-${index}`} onClick={() => setActiveStep(activeStep === index ? null : index)}><span>{String(index + 1).padStart(2, "0")}</span><div><h3>{step.title}</h3><p>{step.summary}</p></div><b aria-hidden="true">{activeStep === index ? "−" : "+"}</b></button>{activeStep === index && <p className="step-detail" id={`step-detail-${index}`}>{step.detail}</p>}</article>)}</div></section>

        <section className="join-banner"><div><span className="eyebrow">BETTER TOGETHER</span><h2>Rooted in community.<br /><em>Ready to grow.</em></h2></div><div><p>Whether you&#8217;re growing or gathering, there&#8217;s a place for you here.</p><button className="button button-cream" onClick={onSignUp}>Find your place <span aria-hidden="true">&#8594;</span></button></div><span className="banner-flower" aria-hidden="true">&#10033;</span></section>
      </main>
      <footer className="site-footer"><a className="brand footer-brand" href="#home"><BrandMark/><span className="brand-name">Farm<span className="brand-accent">Connect</span></span></a><p>Growing better connections between farms and families.</p><span>&#169; {new Date().getFullYear()} FarmConnect</span></footer>
    </div>
  );
}

export default Home;
