interface SidebarControlBeltProps {
  loadComplexJDLScriptSample: () => void;
  parseJDLToCanvas: () => void;
  generateJDLFromCanvas: () => void;
  downloadJDLHandler: () => void;
  isGenerating: boolean;
}

export default function SidebarControlBelt({
  loadComplexJDLScriptSample,
  parseJDLToCanvas,
  generateJDLFromCanvas,
  downloadJDLHandler,
  isGenerating,
}: SidebarControlBeltProps) {
  return (
    <div className="sidebar-control-belt">
      <button onClick={loadComplexJDLScriptSample} className="btn-sidebar-action btn-slate">
        Load Sample
      </button>
      <button onClick={parseJDLToCanvas} className="btn-sidebar-action btn-amber">
        Parse CDL
      </button>
      <button 
        onClick={generateJDLFromCanvas} 
        className="btn-sidebar-action btn-green"
        disabled={isGenerating}
        style={{ display: 'flex', gap: '0.25rem', justifyContent: 'center', alignItems: 'center' }}
      >
        {isGenerating ? (
          <>
            <svg className="animate-spin" style={{ width: '12px', height: '12px' }} viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Generating...
          </>
        ) : (
          'Generate'
        )}
      </button>
      <button onClick={downloadJDLHandler} className="btn-sidebar-action btn-blue">
        Export
      </button>
    </div>
  );
}
