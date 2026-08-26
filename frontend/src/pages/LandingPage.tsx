import { Link } from 'react-router-dom';
import { Workflow, BookOpen, FileCode, GraduationCap, ArrowRight, ExternalLink } from 'lucide-react';
import ThemeToggle from '../components/ThemeToggle';

export default function LandingPage() {
  return (
    <div className="landing-container">
      {/* Theme Toggle Button */}
      <div style={{ position: 'absolute', top: '1.5rem', right: '1.5rem', zIndex: 10 }}>
        <ThemeToggle />
      </div>

      <nav className="landing-navbar" aria-label="Landing page navigation">
        <a
          className="navbar-about-button"
          href="https://www.vermacodes.me"
          target="_blank"
          rel="noreferrer"
        >
          <span>About</span>
          <ExternalLink size={14} />
        </a>
      </nav>

      {/* Soft Blue Ambient Pulse Orb Overlay */}
      <div className="ambient-glow" />

      <main className="landing-hero">

        {/* Step 1: Minimalist Logo Box */}
        <div className="academic-logo animate-step-2">
          <GraduationCap size={52} style={{ position: 'absolute', top: '-18px', right: '-14px', color: '#fbbf24', transform: 'rotate(12deg)' }} />
          <span className="logo-brackets">&lt;/&gt;</span>
        </div>

        <h1 className="main-title animate-step-2">CodeClassroom</h1>

        <p className="main-description animate-step-2">
          An intuitive web-based environment designed to bridge the gap between architectural layout and functional code generation. Learn software design visually using 
          <strong> JHipster Domain Language (JDL)</strong> modeling frameworks alongside interactive design pattern modules.
        </p>

        {/* Step 2: Minimal Blue Actions Bar */}
        <div className="button-group animate-step-3">
          <Link to="/modelling" className="btn-primary">
            <Workflow size={18} />
            <span>Launch Modelling Editor</span>
            <ArrowRight size={14} />
          </Link>

          <Link to="/design-patterns" className="btn-secondary">
            <BookOpen size={18} />
            <span>Study Design Patterns</span>
          </Link>
        </div>

        {/* Step 3: Frosty Uniform Cards Matrix */}
        <div className="features-grid animate-step-4">
          <div className="feature-card">
            <div className="icon-wrapper icon-blue">
              <Workflow size={22} />
            </div>
            <h3 className="card-title">Visual Modelling</h3>
            <p className="card-text">
              Create database tables and domain models within an interactive canvas workspace using clean drag-and-drop actions.
            </p>
          </div>

          <div className="feature-card">
            <div className="icon-wrapper icon-green">
              <FileCode size={22} />
            </div>
            <h3 className="card-title">JDL Code Generation</h3>
            <p className="card-text">
              Compile your architecture canvas models to standard JHipster Domain Language scripts instantly.
            </p>
          </div>

          <div className="feature-card">
            <div className="icon-wrapper icon-amber">
              <BookOpen size={22} />
            </div>
            <h3 className="card-title">Design Patterns</h3>
            <p className="card-text">
              Learn Gang of Four structural design configurations using interactive visualizations.
            </p>
          </div>
        </div>
      </main>

      {/* Viewport-locked Bottom Navbar bar */}
      <footer className="academic-footer">
        <div className="footer-content">
          <div className="footer-left">
            <p className="footer-logo">CodeClassroom</p>
            <p className="footer-desc">Teaching and Learning Software Design Architecture</p>
          </div>
          <div className="footer-right">
            <a
              className="footer-link"
              href="https://www.vermacodes.me"
              target="_blank"
              rel="noreferrer"
            >
              About
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
