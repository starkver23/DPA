import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import LandingPage from './pages/LandingPage';
import ModellingEditor from './components/ModellingEditor';
import DesignPatternsPage from './pages/DesignPatternsPage';

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        {/* Landing page is now the root view */}
        <Route path="/" element={<LandingPage />} />
        
        {/* Legacy modelling workflow perfectly preserved */}
        <Route path="/modelling" element={<ModellingEditor />} />
        
        {/* Educational patterns module */}
        <Route path="/design-patterns" element={<DesignPatternsPage />} />
      </Routes>
    </Router>
  );
};

export default App;