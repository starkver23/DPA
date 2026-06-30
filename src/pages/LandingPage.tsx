import React from 'react';
import { Link } from 'react-router-dom';
import { Workflow, BookOpen, FileCode, GraduationCap, ArrowRight, Binary } from 'lucide-react';

const LandingPage: React.FC = () => {
  return (
    <div className="landing-container">
      <main className="landing-hero">
        
        {/* <div className="academic-badge">
          <Binary size={14} />
          <span>MSc Computer Science Dissertation Project</span>
        </div> */}

        <div className="academic-logo">
          <GraduationCap size={56} style={{ position: 'absolute', top: '-20px', right: '-16px', color: '#fbbf24', transform: 'rotate(12deg)' }} />
          <span className="logo-brackets">&lt;/&gt;</span>
        </div>

        <h1 className="main-title">CodeClassroom</h1>

        <p className="main-description">
          An intuitive web-based environment designed to bridge the gap between architectural layout and functional code generation. Learn software design visually using 
          <strong> JHipster Domain Language (JDL)</strong> modeling frameworks alongside interactive design pattern modules.
        </p>

        <div className="button-group">
          <Link to="/modelling" className="btn-primary">
            <Workflow size={20} />
            <span>Launch Modelling Editor</span>
            <ArrowRight size={16} />
          </Link>

          <Link to="/design-patterns" className="btn-secondary">
            <BookOpen size={20} />
            <span>Study Design Patterns</span>
          </Link>
        </div>

        <div className="features-grid">
          <div className="feature-card">
            <div className="icon-wrapper icon-blue">
              <Workflow size={24} />
            </div>
            <h3 className="card-title">Visual Modelling</h3>
            <p className="card-text">
              Create database tables and domain models within an interactive canvas workspace using clean drag-and-drop actions.
            </p>
          </div>

          <div className="feature-card">
            <div className="icon-wrapper icon-green">
              <FileCode size={24} />
            </div>
            <h3 className="card-title">JDL Code Generation</h3>
            <p className="card-text">
              Compile your architecture canvas models to standard JHipster Domain Language scripts instantly.
            </p>
          </div>

          <div className="feature-card">
            <div className="icon-wrapper icon-amber">
              <BookOpen size={24} />
            </div>
            <h3 className="card-title">Design Patterns</h3>
            <p className="card-text">
              Learn Gang of Four structural design configurations using interactive visualizations.
            </p>
          </div>
        </div>
      </main>

      <footer className="academic-footer">
        <div className="footer-content">
          <p className="footer-logo">CodeClassroom</p>
          <p className="footer-desc">An Educational Framework for Software Architecture and Pattern Recognition</p>
          <div className="footer-divider" />
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;