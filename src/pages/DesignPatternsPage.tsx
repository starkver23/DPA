import React from 'react';
import { Link } from 'react-router-dom';
import { BookOpen, ArrowLeft, Lightbulb } from 'lucide-react';

const DesignPatternsPage: React.FC = () => {
  return (
    <div className="patterns-layout">
      <header className="patterns-header">
        <Link to="/" className="btn-back">
          <ArrowLeft size={16} />
          Return to Hub
        </Link>
      </header>

      <main className="patterns-center-panel">
        <div className="patterns-card">
          <div className="pattern-icon-container">
            <BookOpen size={32} />
          </div>
          <h1 className="card-title" style={{ fontSize: '1.875rem', marginBottom: '0.75rem' }}>Design Patterns Lab</h1>
          <p className="card-text" style={{ marginBottom: '2rem' }}>
            Interactive Gang of Four (GoF) behavioral, creational, and structural lessons are currently being staged.
          </p>
          
          <div className="pattern-alert-box">
            <Lightbulb size={20} style={{ color: '#fbbf24', flexShrink: 0 }} />
            <div>
              <span className="alert-title">Upcoming Modules:</span>
              <p className="alert-body">
                Singleton, Factory Method, Strategy, and Observer pattern structures mapped directly to auto-generated boilerplate modules.
              </p>
            </div>
          </div>

          <Link to="/" className="btn-pattern-home">
            Back to Dashboard
          </Link>
        </div>
      </main>

      <footer className="academic-footer" style={{ borderTop: 'none' }}>
        <p className="footer-logo" style={{ fontSize: '0.75rem' }}>CodeClassroom</p>
        <p className="footer-uni" style={{ fontSize: '11px' }}>University of Birmingham MSc Dissertation</p>
      </footer>
    </div>
  );
};

export default DesignPatternsPage;